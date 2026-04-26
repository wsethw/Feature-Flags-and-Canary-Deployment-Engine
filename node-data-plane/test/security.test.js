const test = require("node:test");
const assert = require("node:assert/strict");

const { tokensMatch } = require("../lib/security");

test("tokensMatch accepts exact admin token matches only", () => {
  assert.equal(tokensMatch("demo-secret", "demo-secret"), true);
  assert.equal(tokensMatch("demo-secret", "wrong-secret"), false);
  assert.equal(tokensMatch("demo-secret", ""), false);
});
