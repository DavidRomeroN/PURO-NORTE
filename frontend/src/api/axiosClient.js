import axios from 'axios'
import { CLAVE_TOKEN } from '@/utils/constantes'

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

let alPerderSesion = null

/** AuthContext registra aquí qué hacer cuando el backend responde 401. */
export function registrarManejadorDeSesion(handler) {
  alPerderSesion = handler
}

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(CLAVE_TOKEN)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const esLogin = error.config?.url?.includes('/auth/login')
    if (error.response?.status === 401 && !esLogin) {
      alPerderSesion?.()
    }
    return Promise.reject(error)
  },
)

/**
 * Traduce cualquier error de red o del backend a una frase que la usuaria entienda.
 * Nunca devuelve códigos HTTP ni texto técnico.
 */
export function mensajeDeError(error) {
  if (!error?.response) {
    return 'Sin conexión. Revisa el wifi e intenta de nuevo.'
  }

  const { status, data } = error.response
  const mensaje = typeof data?.mensaje === 'string' ? data.mensaje : ''

  if (status === 401) return 'Usuario o contraseña incorrectos'
  if (status === 403) return 'No tienes permiso para hacer eso.'
  if (status === 404) return 'Eso ya no existe. Actualiza la pantalla.'
  if (status >= 500) return 'Algo falló. Intenta de nuevo.'

  if (status === 409) {
    if (mensaje.includes('pedido abierto')) return 'Esa mesa ya tiene un pedido abierto.'
    if (mensaje.toLowerCase().includes('cortesía')) {
      return 'La salchicha viene de cortesía y no se puede cambiar.'
    }
    return mensaje || 'No se puede hacer eso ahora.'
  }

  if (status === 400) {
    const primerCampo = Array.isArray(data?.errores) ? data.errores[0] : null
    return primerCampo ? 'Revisa los datos: ' + primerCampo : 'Revisa los datos e intenta de nuevo.'
  }

  return mensaje || 'Algo falló. Intenta de nuevo.'
}

export default axiosClient
