// BoilerReport: funciona sin señal (guarda la app en el teléfono)
const V = 'br-v17';
const FILES = ['./', 'index.html', 'jszip.min.js', 'pdfjs/pdf.min.js', 'pdfjs/pdf.worker.min.js', 'manifest.json', 'icon-192.png', 'icon-512.png', 'spell/worker.js', 'spell/nspell.js', 'spell/es.aff', 'spell/es.dic'];
self.addEventListener('install', e => { e.waitUntil(caches.open(V).then(c => c.addAll(FILES)).then(() => self.skipWaiting())); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== V).map(k => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;
  // red primero (para recibir actualizaciones); si no hay señal, la copia guardada
  e.respondWith(fetch(e.request, { cache: 'no-cache' }).then(r => { const cp = r.clone(); caches.open(V).then(c => c.put(e.request, cp)); return r; })
    .catch(() => caches.match(e.request, { ignoreSearch: true }).then(r => r || caches.match('index.html'))));
});
