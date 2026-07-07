const { contextBridge, shell } = require("electron");

contextBridge.exposeInMainWorld("desktopAdmin", {
  isDesktop: true,
  platform: process.platform,
  openExternal: (url) => {
    if (typeof url === "string" && (url.startsWith("http://") || url.startsWith("https://"))) {
      shell.openExternal(url);
    }
  }
});
