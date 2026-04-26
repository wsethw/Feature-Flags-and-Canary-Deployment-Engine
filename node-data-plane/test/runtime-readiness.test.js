const test = require("node:test");
const assert = require("node:assert/strict");

const { buildHealthPayload, isReady } = require("../lib/runtime-readiness");

function baseRuntime(overrides = {}) {
  return {
    startedAt: "2026-04-26T00:00:00.000Z",
    redisCommandReady: true,
    redisSubscriberReady: true,
    cacheHydrated: true,
    lastCacheHydratedAt: "2026-04-26T00:00:01.000Z",
    lastCacheHydrationError: null,
    lastEventAt: null,
    subscriptionsRegistered: true,
    ...overrides
  };
}

test("isReady requires cache hydration and both Redis connections", () => {
  assert.equal(isReady(baseRuntime()), true);
  assert.equal(isReady(baseRuntime({ cacheHydrated: false })), false);
  assert.equal(isReady(baseRuntime({ redisCommandReady: false })), false);
  assert.equal(isReady(baseRuntime({ redisSubscriberReady: false })), false);
  assert.equal(isReady(baseRuntime({ subscriptionsRegistered: false })), false);
});

test("buildHealthPayload exposes degraded readiness details", () => {
  const flags = new Map([["new-checkout", { key: "new-checkout" }]]);
  const payload = buildHealthPayload(
    baseRuntime({ redisSubscriberReady: false }),
    { flagKey: "new-checkout" },
    flags
  );

  assert.equal(payload.ready, false);
  assert.equal(payload.status, "DEGRADED");
  assert.equal(payload.redisHealthy, false);
  assert.deepEqual(payload.cachedFlags, ["new-checkout"]);
});
