import { Link2 } from 'lucide-react'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { horaCorta } from '@/utils/fechas'
import { ESTADO_PEDIDO } from '@/utils/constantes'
import { cn } from '@/utils/cn'

/** Estado por color Y por texto: con luz cálida tenue el color solo no alcanza. */
export function MesaCard({ mesa, onAbrir }) {
  // La cuenta viva es la fuente de verdad: el flag `estado` de la mesa a veces
  // queda desfasado tras cobrar y mostraba "Ocupada" sin pedido ni total.
  const ocupada = Boolean(mesa.pedido)
  const porCobrar = mesa.pedido?.estado === ESTADO_PEDIDO.CERRADO
  // Cuando la mesa se juntó con otra, la cuenta se lleva en la principal. Mostrar el
  // total en las dos haría pensar que se cobra doble.
  const esUnida = mesa.unidaA != null
  const total = esUnida ? null : mesa.pedido?.total
  const juntadas = (mesa.pedido?.mesasUnidas ?? []).map((otra) => otra.numero)

  const etiquetaEstado = esUnida
    ? `Unida a la ${mesa.unidaA}`
    : porCobrar
      ? 'Por cobrar'
      : ocupada
        ? 'Ocupada'
        : 'Libre'

  return (
    <button
      type="button"
      onClick={() => onAbrir(mesa)}
      aria-label={
        esUnida
          ? `Mesa ${mesa.numero}, unida a la mesa ${mesa.unidaA}`
          : `Mesa ${mesa.numero}, ${etiquetaEstado.toLowerCase()}`
      }
      className={cn(
        'flex min-h-32 w-full flex-col justify-between rounded-app border p-4 text-left',
        'transition-all duration-150 active:scale-[0.97]',
        ocupada
          ? porCobrar
            ? 'border-hoja-300 bg-hoja-50 shadow-suave hover:border-hoja-500 hover:shadow-media'
            : 'border-brasa-200 bg-brasa-50 shadow-suave hover:border-brasa-400 hover:shadow-media'
          : 'border-borde bg-superficie shadow-suave hover:border-borde-fuerte hover:shadow-media',
      )}
    >
      <span className="flex w-full items-start justify-between gap-2">
        <span
          className={cn(
            'text-4xl font-extrabold leading-none',
            ocupada ? 'text-carbon' : 'text-borde-fuerte',
          )}
        >
          {mesa.numero}
        </span>
        {ocupada && mesa.pedido ? (
          <span className="shrink-0 text-right text-sm font-semibold text-brasa-700">
            <span className="block">Entró {horaCorta(mesa.pedido.creadoEn)}</span>
            {mesa.pedido.cerradoEn ? (
              <span className="block text-hoja-700">Cerró {horaCorta(mesa.pedido.cerradoEn)}</span>
            ) : null}
          </span>
        ) : null}
      </span>

      <span className="w-full">
        <span
          className={cn(
            'flex items-center gap-1.5 text-base font-bold',
            porCobrar ? 'text-hoja-700' : ocupada ? 'text-brasa-700' : 'text-hoja-700',
          )}
        >
          {esUnida ? (
            <>
              <Link2 size={18} className="shrink-0" aria-hidden="true" />
              Unida a la {mesa.unidaA}
            </>
          ) : (
            <>
              <span
                aria-hidden="true"
                className={cn(
                  'size-2 rounded-full',
                  porCobrar ? 'bg-hoja-600' : ocupada ? 'bg-brasa-500' : 'bg-hoja-600',
                )}
              />
              {etiquetaEstado}
            </>
          )}
        </span>

        {total != null ? (
          <span className="monto mt-0.5 block text-2xl font-extrabold leading-tight text-carbon">
            {formatoMoneda(total)}
          </span>
        ) : null}

        {juntadas.length > 0 ? (
          <span className="mt-0.5 block text-sm font-semibold text-brasa-700">
            con la {juntadas.join(' y la ')}
          </span>
        ) : null}
      </span>
    </button>
  )
}
