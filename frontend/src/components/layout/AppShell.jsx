import { ChevronLeft, LogOut } from 'lucide-react'
import { BottomNav } from './BottomNav'
import { SidebarNav } from './SidebarNav'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/utils/cn'

const ANCHOS = {
  normal: 'max-w-3xl',
  ancho: 'max-w-6xl',
}

/**
 * Cabecera + contenido + (nav inferior o barra de total).
 *
 * En celular las pantallas de tarea ocultan el nav y muestran su barra de total,
 * para dejar una sola acción principal a la vista. En escritorio la navegación
 * vive siempre en la barra lateral, donde no compite con nada.
 */
export function AppShell({
  titulo,
  subtitulo,
  onVolver,
  conNav = true,
  conSalir = false,
  ancho = 'normal',
  barraInferior = null,
  children,
}) {
  const { logout, usuario } = useAuth()
  const contenedor = cn('mx-auto w-full px-4 sm:px-6', ANCHOS[ancho])

  // El nav inferior desaparece en escritorio; la barra de total no, y sigue tapando.
  const espacioAbajo = barraInferior ? 'pb-40' : conNav ? 'pb-36 lg:pb-12' : 'pb-10'

  return (
    <div className="min-h-dvh bg-fondo">
      <SidebarNav />

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-borde bg-superficie/90 pt-[env(safe-area-inset-top)] backdrop-blur">
          <div className={cn(contenedor, 'flex items-center gap-2 py-3')}>
            {onVolver ? (
              <button
                type="button"
                onClick={onVolver}
                aria-label="Volver"
                className="-ml-2 flex min-h-12 min-w-12 shrink-0 items-center justify-center rounded-app text-carbon transition-colors hover:bg-hundido"
              >
                <ChevronLeft size={28} />
              </button>
            ) : null}

            <div className="min-w-0 grow">
              <h1 className="truncate text-2xl font-extrabold leading-tight text-carbon sm:text-3xl">
                {titulo}
              </h1>
              {subtitulo ? (
                <p className="truncate text-sm font-medium text-tinta sm:text-base">{subtitulo}</p>
              ) : null}
            </div>

            {/* En escritorio salir vive en la barra lateral: acá sería un duplicado. */}
            {conSalir ? (
              <button
                type="button"
                onClick={logout}
                aria-label={`Cerrar sesión de ${usuario?.nombre ?? ''}`}
                className="-mr-2 flex min-h-12 min-w-12 shrink-0 items-center justify-center rounded-app text-tinta transition-colors hover:bg-alerta-suave hover:text-alerta lg:hidden"
              >
                <LogOut size={24} />
              </button>
            ) : null}
          </div>
        </header>

        <main className={cn(contenedor, 'pt-5', espacioAbajo)}>{children}</main>
      </div>

      {barraInferior}
      {conNav && !barraInferior ? <BottomNav /> : null}
    </div>
  )
}
