import { Button } from '@/components/ui/button'

/** Barra fija con la única acción principal de una pantalla de administración. */
export function BarraAccion({ etiqueta, icono: Icono, onAccion, deshabilitada }) {
  return (
    <div className="fixed inset-x-0 bottom-0 z-30 border-t border-borde bg-superficie/95 px-4 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3 shadow-[0_-4px_20px_rgb(28_22_19/0.08)] backdrop-blur sm:px-6 lg:left-64">
      <div className="mx-auto max-w-3xl">
        <Button tamano="grande" className="w-full" disabled={deshabilitada} onClick={onAccion}>
          {Icono ? <Icono size={24} /> : null}
          {etiqueta}
        </Button>
      </div>
    </div>
  )
}
