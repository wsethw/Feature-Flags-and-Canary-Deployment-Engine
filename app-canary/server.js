const crypto = require("node:crypto");
const express = require("express");

const PORT = Number(process.env.PORT || 8080);
const VERSION = process.env.VERSION_LABEL || "canary";
const CHECKOUT_FLOW = process.env.CHECKOUT_FLOW || "new amazing flow";
const ADMIN_TOKEN = process.env.SIMULATION_ADMIN_TOKEN || "";

const app = express();
app.disable("x-powered-by");
app.use(express.json());

let healthFailure = false;

function log(message, details = {}) {
  console.log(
    JSON.stringify({
      service: "app-canary",
      timestamp: new Date().toISOString(),
      message,
      ...details
    })
  );
}

function requireAdmin(request, response, next) {
  if (!ADMIN_TOKEN || request.header("X-Admin-Token") === ADMIN_TOKEN) {
    return next();
  }

  return response.status(401).json({
    error: "unauthorized",
    message: "A valid admin token is required to change backend simulation mode.",
    requestId: request.requestId
  });
}

app.use((request, response, next) => {
  request.requestId = request.header("X-Request-Id") || crypto.randomUUID();
  response.setHeader("X-Request-Id", request.requestId);
  next();
});

app.get("/health", (request, response) => {
  if (healthFailure) {
    return response.status(500).json({
      status: "DOWN",
      version: VERSION
    });
  }

  return response.json({
    status: "UP",
    version: VERSION
  });
});

app.get("/ready", (request, response) => {
  response.json({
    status: healthFailure ? "DEGRADED" : "READY",
    version: VERSION
  });
});

app.post("/admin/fail-health", requireAdmin, (request, response) => {
  healthFailure = true;
  log("health-mode-changed", { requestId: request.requestId, healthFailure });
  response.json({ version: VERSION, healthFailure });
});

app.post("/admin/recover-health", requireAdmin, (request, response) => {
  healthFailure = false;
  log("health-mode-changed", { requestId: request.requestId, healthFailure });
  response.json({ version: VERSION, healthFailure });
});

app.all("*", (request, response) => {
  log("request-served", {
    requestId: request.requestId,
    method: request.method,
    path: request.originalUrl,
    routedBy: request.header("X-Routed-By"),
    targetVersion: request.header("X-Target-Version")
  });

  response.json({
    version: VERSION,
    checkout: CHECKOUT_FLOW,
    path: request.originalUrl,
    routedBy: request.header("X-Routed-By") || "direct",
    targetVersion: request.header("X-Target-Version") || VERSION,
    requestId: request.requestId,
    userContext: request.header("X-User-Context") || null,
    servedAt: new Date().toISOString()
  });
});

app.listen(PORT, () => {
  log("backend-started", { port: PORT, version: VERSION });
});
