const test = require("node:test");
const assert = require("node:assert/strict");

const {
  parseUserContext,
  normalizeContext,
  maskContextForLogs
} = require("../lib/context");

test("parseUserContext supports key value headers", () => {
  assert.deepEqual(parseUserContext("userId=7,country=BR,platform=iOS"), {
    userId: "7",
    country: "BR",
    platform: "iOS"
  });
});

test("parseUserContext supports JSON payloads", () => {
  assert.deepEqual(parseUserContext('{"userId":"8","country":"US"}'), {
    userId: "8",
    country: "US"
  });
});

test("parseUserContext rejects oversized headers", () => {
  assert.throws(() => parseUserContext(`userId=${"x".repeat(2049)}`), /too large/);
});

test("normalizeContext trims values and removes empties", () => {
  assert.deepEqual(normalizeContext({
    userId: " 13 ",
    platform: " ",
    country: "BR"
  }), {
    userId: "13",
    country: "BR"
  });
});

test("maskContextForLogs redacts user id", () => {
  assert.deepEqual(maskContextForLogs({
    userId: "12345",
    country: "BR"
  }), {
    userId: "***45",
    country: "BR"
  });
});
