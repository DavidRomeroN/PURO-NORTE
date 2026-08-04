import { NavLink } from 'react-router-dom'
import { Flame, LogOut } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { seccionesVisibles } from './navegacion'
import { INFO_ROLES } from '@/utils/constantes'
import { cn } from '@/utils/cn'

/**
 * En tablet apaisada y escritorio sobra ancho: la navegación se va al costado y
 * deja toda la altura para el contenido, que es donde está el trabajo.
 */
export function SidebarNav() {
  const { puedeCobrar, esAdmin, usuario, logout } = useAuth()
  const secciones = seccionesVisibles({ puedeCobrar, esAdmin })

  return (
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-borde bg-superficie lg:flex">
      <div className="flex items-center gap-3 px-5 py-6">
        <span className="flex size-11 items-center justify-center rounded-app bg-carbon">
          <Flame size={24} className="text-brasa-500" fill="currentColor" />
        </span>
        <span>
          <span className="block text-lg font-extrabold leading-tight text-carbon">Puro Norte</span>
          <span className="block text-sm text-tinta">Anticuchería</span>
        </span>
      </div>

      <nav className="grow px-3">
        <ul className="flex flex-col gap-1">
          {secciones.map(({ a, etiqueta, Icono }) => (
            <li key={a}>
              <NavLink
                to={a}
                className={({ isActive }) =>
                  cn(
                    'flex min-h-14 items-center gap-3 rounded-app px-3 text-lg font-semibold transition-colors',
                    isActive
                      ? 'bg-brasa-50 text-brasa-700'
                      : 'text-tinta hover:bg-hundido hover:text-carbon',
                  )
                }
              >
                {({ isActive }) => (
                  <>
                    <Icono size={24} strokeWidth={isActive ? 2.6 : 2} />
                    {etiqueta}
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="border-t border-borde p-3">
        <div className="px-2 pb-2">
          <p className="truncate text-base font-bold text-carbon">{usuario?.nombre}</p>
          <p className="truncate text-sm text-tinta">
            {usuario?.rol ? INFO_ROLES[usuario.rol]?.etiqueta : ''}
          </p>
        </div>
        <button
          type="button"
          onClick={logout}
          className="flex min-h-12 w-full items-center gap-3 rounded-app px-3 text-base font-semibold text-tinta transition-colors hover:bg-alerta-suave hover:text-alerta"
        >
          <LogOut size={22} />
          Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
