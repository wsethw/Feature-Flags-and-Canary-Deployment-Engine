function createAdminGuard(adminApiToken) {
  return function adminGuard(request, response, next) {
    if (!adminApiToken) {
      return next();
    }

    if (request.header("X-Admin-Token") === adminApiToken) {
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
  createAdminGuard
};

