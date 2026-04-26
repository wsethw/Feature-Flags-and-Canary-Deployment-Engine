const crypto = require("node:crypto");

function tokensMatch(configuredToken, presentedToken) {
  if (!presentedToken) {
    return false;
  }

  const configured = Buffer.from(configuredToken);
  const presented = Buffer.from(presentedToken);
  return configured.length === presented.length && crypto.timingSafeEqual(configured, presented);
}

function createAdminGuard(adminApiToken) {
  return function adminGuard(request, response, next) {
    if (!adminApiToken) {
      return next();
    }

    if (tokensMatch(adminApiToken, request.header("X-Admin-Token"))) {
      return next();
    }

    return response.status(401).json({
      error: "unauthorized",
      message: "A valid admin token is required to access this internal endpoint.",
      requestId: request.requestId
    });
  };
}

module.exports = {
  createAdminGuard,
  tokensMatch
};
