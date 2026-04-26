function isReady(runtime) {
  return Boolean(
    runtime.cacheHydrated &&
    runtime.redisCommandReady &&
    runtime.redisSubscriberReady &&
    runtime.subscriptionsRegistered
  );
}

function buildHealthPayload(runtime, config, flagCache) {
  const ready = isReady(runtime);
  return {
    status: ready ? "UP" : runtime.cacheHydrated ? "DEGRADED" : "STARTING",
    ready,
    trackedFlag: config.flagKey,
    redisHealthy: runtime.redisCommandReady && runtime.redisSubscriberReady,
    redisCommandReady: runtime.redisCommandReady,
    redisSubscriberReady: runtime.redisSubscriberReady,
    subscriptionsRegistered: runtime.subscriptionsRegistered,
    cacheHydrated: runtime.cacheHydrated,
    cachedFlags: Array.from(flagCache.keys()),
    lastCacheHydratedAt: runtime.lastCacheHydratedAt,
    lastCacheHydrationError: runtime.lastCacheHydrationError,
    lastEventAt: runtime.lastEventAt,
    startedAt: runtime.startedAt
  };
}

module.exports = {
  buildHealthPayload,
  isReady
};
