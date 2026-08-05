/**
 * Base del API (…/api) → origen del WebSocket (…/ws).
 * En proxy local (VITE_API_URL=/api) usa el mismo host de la página.
 */
export function urlWebSocket(token) {
  const api = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
  let origen
  if (api.startsWith('http')) {
    origen = api.replace(/\/api\/?$/, '')
  } else {
    origen = window.location.origin
  }
  const qs = token ? `?token=${encodeURIComponent(token)}` : ''
  return `${origen}/ws${qs}`
}
