function parseUserContext(rawHeader) {
  if (!rawHeader) {
    return {};
  }

  const candidate = Array.isArray(rawHeader) ? rawHeader.join(",") : String(rawHeader).trim();
  if (!candidate) {
    return {};
  }
  if (candidate.length > 2048) {
    throw new Error("User context header is too large.");
  }

  if (candidate.startsWith("{")) {
    return JSON.parse(candidate);
  }

  return candidate
    .split(/[;,]/)
    .map((entry) => entry.trim())
    .filter(Boolean)
    .reduce((accumulator, entry) => {
      const separatorIndex = entry.indexOf("=");
      if (separatorIndex > 0) {
        const key = entry.slice(0, separatorIndex).trim();
        const value = entry.slice(separatorIndex + 1).trim();
        if (key && value) {
          accumulator[key] = value;
        }
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
