import { NavLink } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { seccionesVisibles } from './navegacion'
import { cn } from '@/utils/cn'

/** Iconos siempre con etiqueta: la usuaria principal no interpreta iconografía sola. */
export function BottomNav() {
  const { puedeCobrar, esAdmin } = useAuth()
  const secciones = seccionesVisibles({ puedeCobrar, esAdmin })

  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-borde bg-superficie/95 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden">
      <ul className="mx-auto flex max-w-3xl">
        {secciones.map(({ a, etiqueta, Icono }) => (
          <li key={a} className="flex-1">
            <NavLink
              to={a}
              className={({ isActive }) =>
                cn(
                  'relative flex min-h-16 flex-col items-center justify-center gap-1 px-2 py-2 transition-colors',
                  isActive ? 'text-brasa-600' : 'text-tinta',
                )
              }
            >
              {({ isActive }) => (
                <>
                  {/* La barrita superior marca la sección aunque no se distinga el color. */}
                  <span
                    className={cn(
                      'absolute inset-x-6 top-0 h-1 rounded-b-full transition-opacity',
                      isActive ? 'bg-brasa-600 opacity-100' : 'opacity-0',
                    )}
                  />
                  <Icono size={26} strokeWidth={isActive ? 2.6 : 2} />
                  <span className={cn('text-sm', isActive ? 'font-bold' : 'font-medium')}>
                    {etiqueta}
                  </span>
                </>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
