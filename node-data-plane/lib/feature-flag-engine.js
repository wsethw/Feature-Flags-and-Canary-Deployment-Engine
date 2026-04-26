function stableBucket(candidate) {
  const numeric = Number.parseInt(candidate, 10);
  if (!Number.isNaN(numeric)) {
    return Math.abs(numeric % 100);
  }

  let hash = 0;
  for (let index = 0; index < candidate.length; index += 1) {
    hash = (hash << 5) - hash + candidate.charCodeAt(index);
    hash |= 0;
  }
  return Math.abs(hash % 100);
}

function describeRule(rule) {
  return `${rule.attribute}:${rule.operator}:${rule.value}->${rule.targetVersion}`;
}

function matchesRule(rule, context) {
  const currentValue = context[rule.attribute];
  if (!currentValue) {
    return false;
  }

  switch (rule.operator) {
    case "EQUALS":
      return currentValue.toLowerCase() === String(rule.value).trim().toLowerCase();
    case "IN":
      return String(rule.value)
        .split(",")
        .map((item) => item.trim().toLowerCase())
        .includes(currentValue.toLowerCase());
    case "PERCENTAGE":
      return stableBucket(currentValue) < Number.parseInt(rule.value, 10);
    default:
      return false;
  }
}

function evaluateFlag(flagSnapshot, context) {
  if (!flagSnapshot) {
    return {
      targetVersion: "STABLE",
      reason: "flag-not-present-in-cache",
      cacheHit: false
    };
  }

  if (!flagSnapshot.enabled) {
    return {
      targetVersion: "STABLE",
      reason: "flag-globally-disabled",
      cacheHit: true
    };
  }

  const rules = Array.isArray(flagSnapshot.rules) ? flagSnapshot.rules : [];
  if (rules.length === 0) {
    return {
      targetVersion: "CANARY",
      reason: "flag-enabled-without-targeting-rules",
      cacheHit: true
    };
  }

  const canaryRules = rules.filter((rule) => rule.targetVersion === "CANARY");
  if (canaryRules.length > 0 && canaryRules.every((rule) => matchesRule(rule, context))) {
    return {
      targetVersion: "CANARY",
      reason: `matched-canary-rules:${canaryRules.map(describeRule).join("|")}`,
      cacheHit: true
    };
  }

  const stableRules = rules.filter((rule) => rule.targetVersion === "STABLE");
  if (stableRules.length > 0 && stableRules.every((rule) => matchesRule(rule, context))) {
    return {
      targetVersion: "STABLE",
      reason: `matched-stable-rules:${stableRules.map(describeRule).join("|")}`,
      cacheHit: true
    };
  }

  return {
    targetVersion: "STABLE",
    reason: `fallback-stable-after-evaluation:${canaryRules.map(describeRule).join("|")}`,
    cacheHit: true
  };
}

module.exports = {
  evaluateFlag,
  matchesRule,
  stableBucket
};

