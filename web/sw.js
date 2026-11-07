/* Service worker: the shell is cached, the prices are not.
 *
 * Policy, in one line each:
 *   - App shell (HTML/CSS/JS/icons) → cache-first, refreshed in the background. Opening the app
 *     offline must never wait on a network timeout.
 *   - Rate APIs → never touched here. A cached HTTP response looks identical to a live one, so a
 *     cached price would be drawn as fresh; the app keeps its own last-known rates in localStorage
 *     and labels them with their age ("Cached 20:41"). Honest beats clever.
 *   - Anything else → straight to the network.
 *
 * Bump CACHE whenever the shell changes — the HTML and JS are served from this cache, so a stale
 * version string means users keep the old build until they clear site data.
 */
const CACHE = "currency-shell-v7";
const SHELL = [
  "./",
  "./index.html",
  "./app.css",
  "./app.js",
  "./manifest.webmanifest",
  "./icon-192.png",
  "./icon-512.png",
  "./icon-maskable-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;
  const url = new URL(request.url);

  if (url.origin === location.origin) {
    event.respondWith(
      caches.match(request).then((hit) => {
        const network = fetch(request).then((res) => {
          if (res && res.ok) caches.open(CACHE).then((cache) => cache.put(request, res.clone()));
          return res;
        }).catch(() => hit);
        return hit || network;
      })
    );
    return;
  }

  // Cross-origin requests (the rate feeds) go straight to the network. See the note at the top.
});
