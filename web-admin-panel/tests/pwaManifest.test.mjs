import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const publicDir = path.resolve(process.cwd(), "public");
const manifestPath = path.join(publicDir, "manifest.webmanifest");
const swPath = path.join(publicDir, "sw.js");

test("PWA manifest exposes standalone install metadata", () => {
  const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
  assert.equal(manifest.name, "Dindori Pranit Admin");
  assert.equal(manifest.display, "standalone");
  assert.ok(Array.isArray(manifest.icons) && manifest.icons.length > 0);
});

test("service worker file exists", () => {
  assert.equal(fs.existsSync(swPath), true);
});
