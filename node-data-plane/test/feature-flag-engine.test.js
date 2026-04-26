const test = require("node:test");
const assert = require("node:assert/strict");

const { evaluateFlag, matchesRule, stableBucket } = require("../lib/feature-flag-engine");

test("stableBucket is deterministic for numeric identifiers", () => {
  assert.equal(stableBucket("107"), 7);
});

test("matchesRule evaluates percentage buckets", () => {
  assert.equal(matchesRule({
    attribute: "userId",
    operator: "PERCENTAGE",
    value: "10",
    targetVersion: "CANARY"
  }, {
    userId: "7"
  }), true);
});

test("evaluateFlag routes matching context to canary", () => {
  const decision = evaluateFlag({
    key: "new-checkout",
    enabled: true,
    rules: [
      { attribute: "country", operator: "EQUALS", value: "BR", targetVersion: "CANARY" },
      { attribute: "platform", operator: "EQUALS", value: "iOS", targetVersion: "CANARY" },
      { attribute: "userId", operator: "PERCENTAGE", value: "10", targetVersion: "CANARY" }
    ]
  }, {
    userId: "7",
    country: "BR",
    platform: "iOS"
  });

  assert.equal(decision.targetVersion, "CANARY");
});

test("evaluateFlag falls back to stable when the context misses a rule", () => {
  const decision = evaluateFlag({
    key: "new-checkout",
    enabled: true,
    rules: [
      { attribute: "country", operator: "EQUALS", value: "BR", targetVersion: "CANARY" },
      { attribute: "platform", operator: "EQUALS", value: "iOS", targetVersion: "CANARY" },
      { attribute: "userId", operator: "PERCENTAGE", value: "10", targetVersion: "CANARY" }
    ]
  }, {
    userId: "15",
    country: "BR",
    platform: "iOS"
  });

  assert.equal(decision.targetVersion, "STABLE");
});
