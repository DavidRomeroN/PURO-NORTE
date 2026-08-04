import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { EstadoCarga } from './EstadoCarga'

export function RutaProtegida({ roles, children }) {
  const { estaAutenticado, rol, cargando } = useAuth()
  const ubicacion = useLocation()

  if (cargando) {
    return (
      <div className="mx-auto max-w-3xl p-4 pt-8">
        <EstadoCarga filas={3} />
      </div>
    )
  }

  if (!estaAutenticado) {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />
  }

  if (roles && !roles.includes(rol)) {
    return <Navigate to="/mesas" replace />
  }

  return children
}
