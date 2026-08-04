import { useEffect, useState } from 'react'
import { Dialog, DialogContent } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { CardBoton } from '@/components/ui/card'
import { formatoMoneda } from '@/utils/formatoMoneda'

/**
 * Los motivos de siempre, a un toque. Escribir en una tablet con las manos ocupadas es
 * lento, y si cuesta poner el motivo la gente termina inventando cualquiera.
 */
const MOTIVOS_FRECUENTES = [
  'Se fueron sin consumir',
  'Mesa abierta por error',
  'Cuenta duplicada',
]

export function AnularCuentaDialog({ abierto, onOpenChange, pedido, onAnular, anulando }) {
  const [motivo, setMotivo] = useState('')

  useEffect(() => {
    if (abierto) setMotivo('')
  }, [abierto])

  const total = Number(pedido?.total ?? 0)
  const conConsumo = total > 0
  const valido = motivo.trim().length > 0

  return (
    <Dialog open={abierto} onOpenChange={onOpenChange}>
      <DialogContent
        titulo="¿Anular esta cuenta?"
        descripcion="La mesa queda libre y la cuenta no se cobra."
      >
        <div className="flex flex-col gap-4">
          {/* Anular con consumo es cómo se hace desaparecer una venta. Que se vea. */}
          {conConsumo ? (
            <div className="rounded-app border-2 border-alerta/30 bg-alerta-suave p-4">
              <p className="text-base font-semibold text-alerta">
                Esta cuenta tiene {formatoMoneda(total)} consumidos.
              </p>
              <p className="mt-1 text-base text-tinta">
                Si la comida ya salió, la anulación queda registrada a tu nombre.
              </p>
            </div>
          ) : null}

          <div className="flex flex-col gap-2">
            <Label htmlFor="motivo-anulacion">¿Por qué se anula?</Label>
            <div className="grid gap-2 sm:grid-cols-3">
              {MOTIVOS_FRECUENTES.map((texto) => (
                <CardBoton
                  key={texto}
                  seleccionada={motivo === texto}
                  aria-pressed={motivo === texto}
                  onClick={() => setMotivo(texto)}
                  className="min-h-14 px-3 py-2 text-center text-base font-semibold text-carbon"
                >
                  {texto}
                </CardBoton>
              ))}
            </div>
            <Input
              id="motivo-anulacion"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="o escribe el motivo"
              maxLength={300}
            />
          </div>

          <Button
            variante="destructiva"
            tamano="grande"
            className="w-full"
            disabled={!valido || anulando}
            onClick={() => onAnular(motivo.trim())}
          >
            {anulando ? 'Anulando...' : 'Sí, anular la cuenta'}
          </Button>
          <Button
            variante="secundaria"
            tamano="grande"
            className="w-full"
            disabled={anulando}
            onClick={() => onOpenChange(false)}
          >
            No, volver
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
