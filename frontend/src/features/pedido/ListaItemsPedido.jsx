import { useState } from 'react'
import { Pencil, Trash2 } from 'lucide-react'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { detalleItem, etiquetaItem } from '@/utils/etiquetas'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'

export function ListaItemsPedido({ items, onQuitar, onEditarPrecio, quitando = false }) {
  const [porQuitar, setPorQuitar] = useState(null)

  return (
    <>
      <ul className="flex flex-col gap-2">
        {items.map((item) => {
          const detalle = detalleItem(item)
          return (
            <li key={item.id}>
              <Card className="flex items-center gap-3 p-3 sm:p-4">
                <div className="min-w-0 grow">
                  <p className="text-lg font-bold leading-tight text-carbon">
                    {item.cantidad > 1 ? (
                      <span className="monto mr-1.5 rounded-md bg-hundido px-1.5 py-0.5 text-base">
                        {item.cantidad}×
                      </span>
                    ) : null}
                    {etiquetaItem(item)}
                  </p>
                  {detalle ? <p className="mt-0.5 text-base text-tinta">{detalle}</p> : null}
                  {item.editadoManualmente ? (
                    <Badge tono="brasa" className="mt-1.5">
                      Precio cambiado
                    </Badge>
                  ) : null}
                </div>

                <span className="monto shrink-0 text-xl font-extrabold text-carbon sm:text-2xl">
                  {formatoMoneda(Number(item.precioFinal) * item.cantidad)}
                </span>

                <div className="flex shrink-0 items-center">
                  {onEditarPrecio ? (
                    <button
                      type="button"
                      onClick={() => onEditarPrecio(item)}
                      aria-label={`Cambiar el precio de ${etiquetaItem(item)}`}
                      className="flex min-h-12 min-w-12 items-center justify-center rounded-app text-tinta transition-colors hover:bg-brasa-50 hover:text-brasa-700"
                    >
                      <Pencil size={20} />
                    </button>
                  ) : null}

                  {onQuitar ? (
                    <button
                      type="button"
                      onClick={() => setPorQuitar(item)}
                      aria-label={`Quitar ${etiquetaItem(item)}`}
                      className="flex min-h-12 min-w-12 items-center justify-center rounded-app text-tinta transition-colors hover:bg-alerta-suave hover:text-alerta"
                    >
                      <Trash2 size={20} />
                    </button>
                  ) : null}
                </div>
              </Card>
            </li>
          )
        })}
      </ul>

      <ConfirmDialog
        abierto={Boolean(porQuitar)}
        onOpenChange={(abierto) => !abierto && setPorQuitar(null)}
        titulo="¿Quitar del pedido?"
        descripcion={porQuitar ? etiquetaItem(porQuitar) : ''}
        textoConfirmar="Sí, quitar"
        destructivo
        enProceso={quitando}
        onConfirmar={async () => {
          await onQuitar(porQuitar)
          setPorQuitar(null)
        }}
      />
    </>
  )
}
