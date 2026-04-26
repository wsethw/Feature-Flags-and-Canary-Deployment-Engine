module.exports = {
  serviceName: "node-data-plane",
  port: Number(process.env.PORT || 8080),
  redisUrl: process.env.REDIS_URL || "redis://redis:6379",
  updateChannel: process.env.UPDATE_CHANNEL || "feature-flags:updates",
  rollbackChannel: process.env.ROLLBACK_CHANNEL || "feature-flags:rollback",
  redisStateIndexKey: process.env.REDIS_STATE_INDEX_KEY || "feature-flags:index",
  stableTarget: process.env.STABLE_TARGET || "http://app-stable:8080",
  canaryTarget: process.env.CANARY_TARGET || "http://app-canary:8080",
  flagKey: process.env.FLAG_KEY || "new-checkout",
  adminApiToken: process.env.ADMIN_API_TOKEN || ""
};

