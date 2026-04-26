function stableBucket(candidate) {
  const normalized = String(candidate === undefined || candidate === null ? "" : candidate).trim();
  if (/^[+-]?\d+$/.test(normalized)) {
    const numeric = BigInt(normalized);
    const longMin = -9223372036854775808n;
    const longMax = 9223372036854775807n;
    if (numeric >= longMin && numeric <= longMax) {
      const remainder = numeric % 100n;
      return Number((remainder + 100n) % 100n);
    }
  }

  let hash = 0;
  for (let index = 0; index < normalized.length; index += 1) {
    hash = (hash << 5) - hash + normalized.charCodeAt(index);
    hash |= 0;
  }
  return ((hash % 100) + 100) % 100;
}

function parsePercentage(value) {
  const normalized = String(value === undefined || value === null ? "" : value).trim();
  if (!/^\d+$/.test(normalized)) {
    return null;
  }

  const percentage = Number.parseInt(normalized, 10);
  return percentage >= 0 && percentage <= 100 ? percentage : null;
}

function describeRule(rule) {
  return `${rule.attribute}:${rule.operator}:${rule.value}->${rule.targetVersion}`;
}

function matchesRule(rule, context) {
  const currentValue = context[rule.attribute];
  if (currentValue === undefined || currentValue === null || String(currentValue).trim() === "") {
    return false;
  }

  const normalizedValue = String(currentValue).trim();

  switch (rule.operator) {
    case "EQUALS":
      return normalizedValue.toLowerCase() === String(rule.value).trim().toLowerCase();
    case "IN":
      return String(rule.value)
        .split(",")
        .map((item) => item.trim().toLowerCase())
        .includes(normalizedValue.toLowerCase());
    case "PERCENTAGE": {
      const percentage = parsePercentage(rule.value);
      return percentage !== null && stableBucket(normalizedValue) < percentage;
    }
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
