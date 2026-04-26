const crypto = require("node:crypto");
const express = require("express");
const { createProxyMiddleware } = require("http-proxy-middleware");
const Redis = require("ioredis");

const config = require("./lib/config");
const { createLogger } = require("./lib/logger");
const { parseUserContext, normalizeContext, maskContextForLogs } = require("./lib/context");
const { evaluateFlag } = require("./lib/feature-flag-engine");
const {
  buildHealthPayload: buildRuntimeHealthPayload,
  isReady
} = require("./lib/runtime-readiness");
const { createAdminGuard } = require("./lib/security");

const logger = createLogger(config.serviceName);
const app = express();
const adminGuard = createAdminGuard(config.adminApiToken);

app.disable("x-powered-by");
app.use(createRequestContext);
app.use(express.json({ limit: "32kb" }));

const flagCache = new Map();
const runtime = {
  startedAt: new Date().toISOString(),
  redisCommandReady: false,
  redisSubscriberReady: false,
  cacheHydrated: false,
  lastCacheHydratedAt: null,
  lastCacheHydrationError: null,
  lastEventAt: null,
  subscriptionsRegistered: false,
  startupComplete: false
};

const redisOptions = {
  lazyConnect: true,
  maxRetriesPerRequest: null,
  enableReadyCheck: true
};

const redis = new Redis(config.redisUrl, redisOptions);
const subscriber = new Redis(config.redisUrl, redisOptions);

function log(message, details = {}) {
  logger.info(message, details);
}

function upsertFlag(snapshot, source) {
  if (!snapshot || !snapshot.key) {
    return;
  }

  flagCache.set(snapshot.key, snapshot);
  runtime.lastEventAt = new Date().toISOString();
  log("flag-cache-updated", {
    source,
    key: snapshot.key,
    enabled: snapshot.enabled,
    rules: Array.isArray(snapshot.rules) ? snapshot.rules.length : 0
  });
}

async function hydrateCacheFromRedis() {
  runtime.cacheHydrated = false;
  runtime.lastCacheHydrationError = null;

  try {
    const stateKeys = await redis.smembers(config.redisStateIndexKey);
    if (!stateKeys.length) {
      runtime.cacheHydrated = true;
      runtime.lastCacheHydratedAt = new Date().toISOString();
      log("cache-hydrated", { cachedFlags: [] });
      return;
    }

    const pipeline = redis.pipeline();
    stateKeys.forEach((stateKey) => pipeline.get(stateKey));
    const results = await pipeline.exec();

    results.forEach(([error, payload], index) => {
      if (error || !payload) {
        if (error) {
          logger.warn("cache-entry-read-failed", {
            stateKey: stateKeys[index],
            error: error.message
          });
        }
        return;
      }

      try {
        const snapshot = JSON.parse(payload);
        if (snapshot?.key) {
          flagCache.set(snapshot.key, snapshot);
        }
      } catch (error) {
        logger.warn("cache-entry-parse-failed", {
          stateKey: stateKeys[index],
          error: error.message
        });
      }
    });

    runtime.cacheHydrated = true;
    runtime.lastCacheHydratedAt = new Date().toISOString();
    log("cache-hydrated", {
      cachedFlags: Array.from(flagCache.keys())
    });
  } catch (error) {
    runtime.lastCacheHydrationError = error.message;
    throw error;
  }
}

async function ensureSubscriptionsRegistered() {
  if (runtime.subscriptionsRegistered) {
    return;
  }

  await subscriber.subscribe(config.updateChannel, config.rollbackChannel);
  runtime.subscriptionsRegistered = true;
  log("redis-subscriptions-registered", {
    channels: [config.updateChannel, config.rollbackChannel]
  });
}

function handleRedisMessage(channel, message) {
  try {
    const payload = JSON.parse(message);

    if (channel === config.updateChannel) {
      upsertFlag(payload, "redis-pubsub-update");
      return;
    }

    if (channel === config.rollbackChannel) {
      const rollbackKey = payload.flagKey || payload.key || config.flagKey;
      const currentSnapshot = flagCache.get(rollbackKey);
      if (currentSnapshot) {
        flagCache.set(rollbackKey, {
          ...currentSnapshot,
          enabled: false,
          reason: payload.reason || "rollback-received"
        });
      }

      runtime.lastEventAt = new Date().toISOString();
      log("rollback-event-received", {
        channel,
        flagKey: rollbackKey,
        reason: payload.reason,
        triggeredAt: payload.triggeredAt
      });
    }
  } catch (error) {
    logger.error("redis-message-processing-failed", {
      channel,
      error: error.message
    });
  }
}

function createRequestContext(request, response, next) {
  request.requestId = request.header("X-Request-Id") || crypto.randomUUID();
  request.startedAt = Date.now();
  response.setHeader("X-Request-Id", request.requestId);

  response.on("finish", () => {
    logger.info("request-completed", {
      requestId: request.requestId,
      method: request.method,
      path: request.originalUrl,
      statusCode: response.statusCode,
      durationMs: Date.now() - request.startedAt
    });
  });

  next();
}

function buildHealthPayload() {
  return buildRuntimeHealthPayload(runtime, config, flagCache);
}

const proxy = createProxyMiddleware({
  target: config.stableTarget,
  changeOrigin: true,
  xfwd: true,
  router: (request) => request.routingTarget || config.stableTarget,
  on: {
    proxyReq(proxyRequest, request) {
      proxyRequest.setHeader("X-Routed-By", config.serviceName);
      proxyRequest.setHeader("X-Target-Version", request.targetVersion || "STABLE");
      proxyRequest.setHeader("X-Routing-Reason", request.routingReason || "unknown");
      proxyRequest.setHeader("X-Request-Id", request.requestId);
    },
    error(error, request, response) {
      logger.error("proxy-error", {
        requestId: request.requestId,
        path: request.originalUrl,
        target: request.routingTarget,
        error: error.message
      });

      if (!response.headersSent) {
        response.status(502).json({
          error: "proxy_failure",
          message: "The data plane could not reach the selected upstream target.",
          requestId: request.requestId
        });
      }
    }
  }
});

app.use((error, request, response, next) => {
  if (error instanceof SyntaxError && "body" in error) {
    return response.status(400).json({
      error: "invalid_json",
      message: "Request body must be valid JSON.",
      requestId: request.requestId
    });
  }

  return next(error);
});

app.get("/health", (request, response) => {
  response.json(buildHealthPayload());
});

app.get("/ready", (request, response) => {
  const ready = isReady(runtime);
  response.status(ready ? 200 : 503).json({
    status: ready ? "READY" : "WAITING_FOR_REDIS_OR_SNAPSHOT",
    ...buildHealthPayload()
  });
});

app.get("/internal/flags", adminGuard, (request, response) => {
  response.json({
    flags: Array.from(flagCache.values())
  });
});

app.post("/internal/decision-preview", adminGuard, (request, response) => {
  const flagKey = request.body?.flagKey || config.flagKey;
  const context = normalizeContext(request.body?.context || {});
  const flagSnapshot = flagCache.get(flagKey);
  const decision = evaluateFlag(flagSnapshot, context);

  response.json({
    flagKey,
    targetVersion: decision.targetVersion,
    reason: decision.reason,
    context,
    cacheHit: decision.cacheHit
  });
});

app.use((request, response, next) => {
  if (
    request.path === "/health" ||
    request.path === "/ready" ||
    request.path.startsWith("/internal/")
  ) {
    return next();
  }

  let parsedContext = {};
  try {
    parsedContext = normalizeContext(parseUserContext(request.header("X-User-Context")));
  } catch (error) {
    logger.warn("invalid-user-context", {
      requestId: request.requestId,
      rawHeader: request.header("X-User-Context"),
      error: error.message
    });
  }

  const flagSnapshot = flagCache.get(config.flagKey);
  const decision = evaluateFlag(flagSnapshot, parsedContext);

  request.routingTarget =
    decision.targetVersion === "CANARY" ? config.canaryTarget : config.stableTarget;
  request.targetVersion = decision.targetVersion;
  request.routingReason = decision.reason;

  log("request-routed", {
    requestId: request.requestId,
    method: request.method,
    path: request.originalUrl,
    targetVersion: request.targetVersion,
    target: request.routingTarget,
    userContext: maskContextForLogs(parsedContext),
    flagEnabled: flagSnapshot ? flagSnapshot.enabled : false,
    cacheHit: decision.cacheHit
  });

  return proxy(request, response, next);
});

app.use((error, request, response, next) => {
  logger.error("unhandled-request-error", {
    requestId: request.requestId,
    path: request.originalUrl,
    error: error.message
  });

  if (response.headersSent) {
    return next(error);
  }

  return response.status(500).json({
    error: "internal_error",
    message: "The data plane could not complete the request.",
    requestId: request.requestId
  });
});

async function start() {
  redis.on("ready", () => {
    runtime.redisCommandReady = true;
    log("redis-command-connection-ready", { redisUrl: config.redisUrl });
    if (runtime.startupComplete) {
      hydrateCacheFromRedis().catch((error) => {
        logger.warn("background-cache-hydration-failed", { error: error.message });
      });
    }
  });

  redis.on("error", (error) => {
    runtime.redisCommandReady = false;
    logger.error("redis-command-connection-error", { error: error.message });
  });
  redis.on("close", () => {
    runtime.redisCommandReady = false;
    logger.warn("redis-command-connection-closed");
  });
  redis.on("reconnecting", () => {
    runtime.redisCommandReady = false;
  });
  redis.on("end", () => {
    runtime.redisCommandReady = false;
  });

  subscriber.on("ready", () => {
    runtime.redisSubscriberReady = true;
    log("redis-subscriber-ready", { redisUrl: config.redisUrl });
    if (runtime.startupComplete && !runtime.subscriptionsRegistered) {
      ensureSubscriptionsRegistered()
        .catch((error) => {
          logger.error("redis-subscribe-failed", { error: error.message });
        });
    }
  });

  subscriber.on("error", (error) => {
    runtime.redisSubscriberReady = false;
    logger.error("redis-subscriber-error", { error: error.message });
  });
  subscriber.on("close", () => {
    runtime.redisSubscriberReady = false;
    logger.warn("redis-subscriber-connection-closed");
  });
  subscriber.on("reconnecting", () => {
    runtime.redisSubscriberReady = false;
  });
  subscriber.on("end", () => {
    runtime.redisSubscriberReady = false;
  });

  subscriber.on("message", handleRedisMessage);

  await Promise.all([redis.connect(), subscriber.connect()]);
  runtime.redisCommandReady = redis.status === "ready";
  runtime.redisSubscriberReady = subscriber.status === "ready";
  await ensureSubscriptionsRegistered();
  await hydrateCacheFromRedis();
  runtime.startupComplete = true;

  const server = app.listen(config.port, () => {
    log("data-plane-started", {
      port: config.port,
      stableTarget: config.stableTarget,
      canaryTarget: config.canaryTarget,
      trackedFlag: config.flagKey
    });
  });

  process.on("SIGTERM", async () => {
    server.close(() => {
      log("http-server-closed");
    });
    await Promise.allSettled([redis.quit(), subscriber.quit()]);
    process.exit(0);
  });
}

start().catch((error) => {
  logger.error("startup-failed", { error: error.message, stack: error.stack });
  process.exit(1);
});
