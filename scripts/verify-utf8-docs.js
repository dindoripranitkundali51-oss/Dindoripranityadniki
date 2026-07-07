const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const targets = [
  path.join(rootDir, "README.md"),
  path.join(rootDir, "PRODUCTION_CHECKLIST.md"),
  path.join(rootDir, "docs"),
];

const suspiciousPatterns = [
  /ðŸ/u,
  /â€”/u,
  /â€“/u,
  /â€"/u,
  /â€˜/u,
  /â€™/u,
  /â€œ/u,
  /â€/u,
  /â€¦/u,
  /â†’/u,
  /â”/u,
  /ï¸/u,
  /Ã[\u0080-\u00ff]/u,
  /Â[\u0080-\u00ff]/u,
];

function collectMarkdownFiles(entryPath, results) {
  const stat = fs.statSync(entryPath);
  if (stat.isDirectory()) {
    for (const child of fs.readdirSync(entryPath)) {
      collectMarkdownFiles(path.join(entryPath, child), results);
    }
    return;
  }

  if (entryPath.toLowerCase().endsWith(".md")) {
    results.push(entryPath);
  }
}

function assertUtf8WithoutReplacement(filePath) {
  const raw = fs.readFileSync(filePath);
  const text = raw.toString("utf8");
  if (text.includes("\uFFFD")) {
    throw new Error(`${filePath}: contains replacement characters (U+FFFD), likely broken UTF-8 decoding`);
  }
  for (const pattern of suspiciousPatterns) {
    if (pattern.test(text)) {
      throw new Error(`${filePath}: contains suspicious mojibake pattern ${pattern}`);
    }
  }
}

const markdownFiles = [];
for (const target of targets) {
  if (fs.existsSync(target)) {
    collectMarkdownFiles(target, markdownFiles);
  }
}

for (const filePath of markdownFiles) {
  assertUtf8WithoutReplacement(filePath);
}

console.log(`UTF-8 docs verification passed for ${markdownFiles.length} Markdown files.`);
