function parseUserContext(rawHeader) {
  if (!rawHeader) {
    return {};
  }

  const candidate = Array.isArray(rawHeader) ? rawHeader.join(",") : String(rawHeader).trim();
  if (!candidate) {
    return {};
  }

  if (candidate.startsWith("{")) {
    return JSON.parse(candidate);
  }

  return candidate
    .split(/[;,]/)
    .map((entry) => entry.trim())
    .filter(Boolean)
    .reduce((accumulator, entry) => {
      const [key, value] = entry.split("=");
      if (key && value) {
        accumulator[key.trim()] = value.trim();
      }
      return accumulator;
    }, {});
}

function normalizeContext(context) {
  return Object.entries(context || {}).reduce((accumulator, [key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      accumulator[key] = String(value).trim();
    }
    return accumulator;
  }, {});
}

function maskContextForLogs(context) {
  const normalized = normalizeContext(context);
  if (!normalized.userId) {
    return normalized;
  }

  return {
    ...normalized,
    userId: `***${String(normalized.userId).slice(-2)}`
  };
}

module.exports = {
  parseUserContext,
  normalizeContext,
  maskContextForLogs
};

