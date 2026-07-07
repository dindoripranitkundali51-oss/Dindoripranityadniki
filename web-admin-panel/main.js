const { app, BrowserWindow, shell, Menu, dialog } = require("electron");
const http = require("http");
const fs = require("fs");
const path = require("path");

let mainWindow = null;
let server = null;

const MIME_TYPES = {
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".ico": "image/x-icon",
  ".woff": "font/woff",
  ".woff2": "font/woff2"
};

function resolveOutDir() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, "web-admin-out");
  }
  return path.resolve(__dirname, "out");
}

function createStaticServer(outDir) {
  const resolvedOutDir = path.resolve(outDir);
  const outDirPrefix = `${resolvedOutDir}${path.sep}`;
  return new Promise((resolve, reject) => {
    const localServer = http.createServer((req, res) => {
      let urlPath;
      try {
        urlPath = decodeURIComponent((req.url || "/").split("?")[0]);
      } catch {
        res.writeHead(400, { "Content-Type": "text/plain; charset=utf-8" });
        res.end("Invalid request path.");
        return;
      }
      let filePath = path.resolve(resolvedOutDir, urlPath === "/" ? "index.html" : urlPath.replace(/^\/+/, ""));
      if (filePath !== resolvedOutDir && !filePath.startsWith(outDirPrefix)) {
        res.writeHead(403, { "Content-Type": "text/plain; charset=utf-8" });
        res.end("Forbidden.");
        return;
      }

      if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
        filePath = path.join(filePath, "index.html");
      } else if (!fs.existsSync(filePath)) {
        const maybeHtml = `${filePath}.html`;
        if (fs.existsSync(maybeHtml)) {
          filePath = maybeHtml;
        } else {
          filePath = path.join(resolvedOutDir, "index.html");
        }
      }

      fs.readFile(filePath, (error, data) => {
        if (error) {
          res.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
          res.end("Unable to load admin application.");
          return;
        }
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, {
          "Content-Type": MIME_TYPES[ext] || "application/octet-stream",
          "Cache-Control": "no-cache"
        });
        res.end(data);
      });
    });

    localServer.on("error", reject);
    localServer.listen(0, "127.0.0.1", () => resolve(localServer));
  });
}

async function bootstrapUrl() {
  if (process.env.ADMIN_REMOTE_URL) return process.env.ADMIN_REMOTE_URL;

  const outDir = resolveOutDir();
  if (!fs.existsSync(outDir)) {
    throw new Error(`Admin export not found at ${outDir}. Run the web admin build first.`);
  }

  server = await createStaticServer(outDir);
  const address = server.address();
  return `http://127.0.0.1:${address.port}/login`;
}

function createMenu() {
  const template = [
    {
      label: "App",
      submenu: [
        { role: "reload" },
        { role: "toggleDevTools" },
        { type: "separator" },
        { role: "quit" }
      ]
    },
    {
      label: "Window",
      submenu: [{ role: "minimize" }, { role: "close" }]
    }
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

async function createMainWindow() {
  const startUrl = await bootstrapUrl();

  mainWindow = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1180,
    minHeight: 760,
    icon: path.join(__dirname, "admin-icon.png"),
    autoHideMenuBar: false,
    show: false,
    backgroundColor: "#f8fafc",
    title: "Dindori Pranit Admin",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.once("ready-to-show", () => mainWindow.show());
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (typeof url === "string" && (url.startsWith("http://") || url.startsWith("https://"))) {
      shell.openExternal(url);
    }
    return { action: "deny" };
  });

  mainWindow.webContents.on("will-navigate", (event, url) => {
    if (!url.startsWith("http://127.0.0.1:") && url.startsWith("https://")) {
      event.preventDefault();
      shell.openExternal(url);
    } else if (!url.startsWith("http://127.0.0.1:")) {
      event.preventDefault();
    }
  });

  await mainWindow.loadURL(startUrl);
}

const gotSingleInstanceLock = app.requestSingleInstanceLock();
if (!gotSingleInstanceLock) {
  app.quit();
} else {
  app.on("second-instance", () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });
}

app.whenReady().then(async () => {
  try {
    createMenu();
    await createMainWindow();
  } catch (error) {
    dialog.showErrorBox("Admin app failed to start", error.message || String(error));
    app.quit();
  }
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

app.on("before-quit", () => {
  if (server) {
    try {
      server.close();
    } catch (_) {}
  }
});
