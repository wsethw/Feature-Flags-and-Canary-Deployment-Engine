function createLogger(service) {
  function write(level, message, details = {}) {
    const payload = {
      level,
      service,
      timestamp: new Date().toISOString(),
      message,
      ...details
    };

    const writer = level === "error" ? console.error : console.log;
    writer(JSON.stringify(payload));
  }

  return {
    info: (message, details) => write("info", message, details),
    warn: (message, details) => write("warn", message, details),
    error: (message, details) => write("error", message, details)
  };
}

module.exports = {
  createLogger
};

