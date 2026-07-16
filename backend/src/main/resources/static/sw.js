// GhanaRide Service Worker - PWA Support
// Version: 5.2.2

const CACHE_NAME = 'ghanaride-v5.2.2';
const STATIC_CACHE = 'ghanaride-static-v5.2.2';
const DYNAMIC_CACHE = 'ghanaride-dynamic-v5.2.2';
const OFFLINE_URL = '/offline.html';

// Files to cache immediately on install
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/rides',
  '/login',
  '/register',
  '/dashboard',
  '/wallet',
  '/my-bookings',
  '/offline.html',
  '/manifest.json',
  '/css/ghanaride.css',
  '/images/icon-192.png',
  '/images/icon-512.png',
  '/images/icon-192.svg'
];

// API endpoints to cache with network-first strategy
const API_CACHE_PATTERNS = [
  '/api/trips/',
  '/api/v1/public/'
];

// Maximum items in dynamic cache
const MAX_DYNAMIC_CACHE = 50;

// Install event - cache static assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then((cache) => {
        console.log('[SW] Caching static assets');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => self.skipWaiting())
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames
            .filter((name) => name !== STATIC_CACHE && name !== DYNAMIC_CACHE)
            .map((name) => caches.delete(name))
        );
      })
      .then(() => self.clients.claim())
  );
});

// Fetch event - implement caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests
  if (request.method !== 'GET') {
    return;
  }

  // Skip chrome-extension and other non-http(s) schemes
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // Strategy 1: Network first for API calls
  if (API_CACHE_PATTERNS.some(pattern => url.pathname.startsWith(pattern))) {
    event.respondWith(networkFirstStrategy(request));
    return;
  }

  // Strategy 2: Cache first for static assets
  if (isStaticAsset(url.pathname)) {
    event.respondWith(cacheFirstStrategy(request));
    return;
  }

  // Strategy 3: Stale while revalidate for HTML pages
  if (request.headers.get('accept').includes('text/html')) {
    event.respondWith(staleWhileRevalidate(request));
    return;
  }

  // Default: network first
  event.respondWith(networkFirstStrategy(request));
});

// Network-first strategy
async function networkFirstStrategy(request) {
  try {
    const response = await fetch(request);
    
    // Cache successful responses
    if (response.ok) {
      const cache = await caches.open(DYNAMIC_CACHE);
      cache.put(request, response.clone());
      await limitCacheSize(DYNAMIC_CACHE, MAX_DYNAMIC_CACHE);
    }
    
    return response;
  } catch (error) {
    // Fallback to cache
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    // Offline fallback for navigation requests
    if (request.mode === 'navigate') {
      return caches.match(OFFLINE_URL);
    }
    
    throw error;
  }
}

// Cache-first strategy
async function cacheFirstStrategy(request) {
  const cachedResponse = await caches.match(request);
  
  if (cachedResponse) {
    // Update cache in background
    fetch(request).then(response => {
      if (response.ok) {
        caches.open(STATIC_CACHE).then(cache => cache.put(request, response));
      }
    }).catch(() => {});
    
    return cachedResponse;
  }
  
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(STATIC_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    throw error;
  }
}

// Stale-while-revalidate strategy
async function staleWhileRevalidate(request) {
  const cachedResponse = await caches.match(request);
  
  // Fetch fresh version in background
  const networkResponsePromise = fetch(request).then(response => {
    if (response.ok) {
      caches.open(DYNAMIC_CACHE).then(cache => {
        cache.put(request, response.clone());
        limitCacheSize(DYNAMIC_CACHE, MAX_DYNAMIC_CACHE);
      });
    }
    return response;
  }).catch(() => cachedResponse);
  
  // Return cached version immediately if available
  if (cachedResponse) {
    return cachedResponse;
  }
  
  // Otherwise wait for network
  try {
    return await networkResponsePromise;
  } catch (error) {
    if (request.mode === 'navigate') {
      return caches.match(OFFLINE_URL);
    }
    throw error;
  }
}

// Check if URL is a static asset
function isStaticAsset(pathname) {
  return pathname.startsWith('/css/') ||
         pathname.startsWith('/js/') ||
         pathname.startsWith('/images/') ||
         pathname.startsWith('/webjars/') ||
         pathname.endsWith('.css') ||
         pathname.endsWith('.js') ||
         pathname.endsWith('.png') ||
         pathname.endsWith('.jpg') ||
         pathname.endsWith('.svg') ||
         pathname.endsWith('.woff2');
}

// Limit cache size
async function limitCacheSize(cacheName, maxItems) {
  const cache = await caches.open(cacheName);
  const keys = await cache.keys();
  
  if (keys.length > maxItems) {
    await cache.delete(keys[0]);
    await limitCacheSize(cacheName, maxItems);
  }
}

// Background sync for offline bookings
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-bookings') {
    event.waitUntil(syncBookings());
  }
});

async function syncBookings() {
  // Implementation for syncing offline bookings
  console.log('[SW] Syncing offline bookings');
}

// Push notifications
self.addEventListener('push', (event) => {
  if (!event.data) return;
  
  const data = event.data.json();
  const options = {
    body: data.message,
    icon: '/images/icon-192.png',
    badge: '/images/icon-72.png',
    vibrate: [100, 50, 100],
    data: {
      url: data.actionUrl || '/'
    },
    actions: [
      { action: 'open', title: 'Open' },
      { action: 'close', title: 'Dismiss' }
    ]
  };
  
  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

// Notification click handler
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  if (event.action === 'open') {
    event.waitUntil(
      clients.matchAll({ type: 'window', includeUncontrolled: true })
        .then((clientList) => {
          const url = event.notification.data.url;
          
          // Check if already open
          for (const client of clientList) {
            if (client.url.includes(url) && 'focus' in client) {
              return client.focus();
            }
          }
          
          // Open new window
          return clients.openWindow(url);
        })
    );
  }
});

// Periodic background sync (if supported)
self.addEventListener('periodicsync', (event) => {
  if (event.tag === 'update-trips') {
    event.waitUntil(updateTripsCache());
  }
});

async function updateTripsCache() {
  try {
    const response = await fetch('/api/trips/upcoming');
    if (response.ok) {
      const cache = await caches.open(DYNAMIC_CACHE);
      const requests = await cache.keys();
      // Update trip data in cache
      console.log('[SW] Updated trips cache');
    }
  } catch (error) {
    console.log('[SW] Failed to update trips cache:', error);
  }
}

console.log('[SW] Service Worker loaded v5.2.2');