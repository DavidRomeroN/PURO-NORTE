import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import { authApi } from '@/api/authApi'
import { registrarManejadorDeSesion } from '@/api/axiosClient'
import { CLAVE_TOKEN, CLAVE_USUARIO, ROLES } from '@/utils/constantes'

export const AuthContext = createContext(null)

function leerUsuarioGuardado() {
  try {
    const crudo = localStorage.getItem(CLAVE_USUARIO)
    return crudo ? JSON.parse(crudo) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(CLAVE_TOKEN))
  const [usuario, setUsuario] = useState(leerUsuarioGuardado)
  const [cargando, setCargando] = useState(Boolean(localStorage.getItem(CLAVE_TOKEN)))

  const logout = useCallback(() => {
    localStorage.removeItem(CLAVE_TOKEN)
    localStorage.removeItem(CLAVE_USUARIO)
    setToken(null)
    setUsuario(null)
  }, [])

  useEffect(() => {
    registrarManejadorDeSesion(logout)
  }, [logout])

  // La jornada dura 8 horas y el token también: al abrir la app solo confirmamos
  // que el token sigue siendo válido, sin cerrar sesión por inactividad.
  useEffect(() => {
    if (!token) {
      setCargando(false)
      return
    }
    let vigente = true
    authApi
      .yo()
      .then((datos) => {
        if (!vigente) return
        setUsuario(datos)
        localStorage.setItem(CLAVE_USUARIO, JSON.stringify(datos))
      })
      .catch(() => {
        if (vigente) logout()
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })
    return () => {
      vigente = false
    }
    // Solo al montar: si el token cambia es porque se acaba de hacer login.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = useCallback(async (nombreUsuario, password) => {
    const respuesta = await authApi.login(nombreUsuario, password)
    localStorage.setItem(CLAVE_TOKEN, respuesta.token)
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify(respuesta.usuario))
    setToken(respuesta.token)
    setUsuario(respuesta.usuario)
    return respuesta.usuario
  }, [])

  const valor = useMemo(() => {
    const rol = usuario?.rol ?? null
    return {
      token,
      usuario,
      rol,
      cargando,
      estaAutenticado: Boolean(token && usuario),
      // La dueña usa un solo usuario ADMIN: donde puede entrar CAJA, entra ADMIN.
      puedeCobrar: rol === ROLES.CAJA || rol === ROLES.ADMIN,
      esAdmin: rol === ROLES.ADMIN,
      login,
      logout,
    }
  }, [token, usuario, cargando, login, logout])

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
