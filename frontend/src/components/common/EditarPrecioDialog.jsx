import { useEffect, useState } from 'react'
import { Dialog, DialogContent } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { formatoNumero } from '@/utils/formatoMoneda'
import { etiquetaItem } from '@/utils/etiquetas'

export function EditarPrecioDialog({ item, onOpenChange, onGuardar, guardando }) {
  const [precio, setPrecio] = useState('')
  const [motivo, setMotivo] = useState('')

  useEffect(() => {
    if (item) {
      setPrecio(formatoNumero(item.precioFinal))
      setMotivo(item.motivoEdicion ?? '')
    }
  }, [item])

  const valor = Number(precio.replace(',', '.'))
  const valido = precio !== '' && Number.isFinite(valor) && valor >= 0

  return (
    <Dialog open={Boolean(item)} onOpenChange={onOpenChange}>
      <DialogContent titulo="Cambiar el precio" descripcion={item ? etiquetaItem(item) : ''}>
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="precio-final">Precio</Label>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-bold text-tinta">S/</span>
              <Input
                id="precio-final"
                inputMode="decimal"
                value={precio}
                onChange={(e) => setPrecio(e.target.value.replace(/[^\d.,]/g, ''))}
                className="monto text-2xl font-bold"
                autoFocus
              />
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="motivo">Motivo (opcional)</Label>
            <Input
              id="motivo"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="acuerdo con el cliente"
              maxLength={255}
            />
          </div>

          <Button
            tamano="grande"
            className="w-full"
            disabled={!valido || guardando}
            onClick={() => onGuardar(valor, motivo)}
          >
            {guardando ? 'Guardando...' : 'Guardar precio'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
