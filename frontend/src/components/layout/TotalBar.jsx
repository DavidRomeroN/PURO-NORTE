import { Button } from '@/components/ui/button'
import { formatoMoneda } from '@/utils/formatoMoneda'

/** Barra fija con el total siempre visible y la única acción principal de la pantalla. */
export function TotalBar({ total, etiqueta = 'Total', accion, onAccion, accionDeshabilitada }) {
  return (
    <div className="fixed inset-x-0 bottom-0 z-30 border-t border-borde bg-superficie/95 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3 shadow-[0_-4px_20px_rgb(28_22_19/0.08)] backdrop-blur lg:left-64">
      <div className="mx-auto flex max-w-3xl items-center gap-4 px-4 sm:px-6">
        <div className="min-w-0 grow">
          <p className="text-sm font-semibold uppercase tracking-wide text-tinta">{etiqueta}</p>
          <p className="monto text-3xl font-extrabold leading-tight text-carbon sm:text-4xl">
            {formatoMoneda(total)}
          </p>
        </div>
        {accion ? (
          <Button
            tamano="grande"
            disabled={accionDeshabilitada}
            onClick={onAccion}
            className="shrink-0 px-8"
          >
            {accion}
          </Button>
        ) : null}
      </div>
    </div>
  )
}
