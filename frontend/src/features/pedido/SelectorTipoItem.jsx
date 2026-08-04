import { Beef, CupSoda, Package, UtensilsCrossed } from 'lucide-react'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { BotonGrande } from '@/components/common/BotonGrande'
import { TIPO_ITEM } from '@/utils/constantes'

// El ejemplo de abajo evita que haya que abrir cada categoría para saber qué hay.
const OPCIONES = [
  { tipo: TIPO_ITEM.ANTICUCHO, etiqueta: 'Anticuchos', detalle: 'Carne, pollo, corazón...', Icono: Beef },
  { tipo: TIPO_ITEM.COMBO, etiqueta: 'Combos', detalle: 'Mixto simple y especial', Icono: UtensilsCrossed },
  { tipo: TIPO_ITEM.BEBIDA, etiqueta: 'Bebidas', detalle: 'Gaseosa, mate...', Icono: CupSoda },
  { tipo: TIPO_ITEM.EXTRA, etiqueta: 'Extras', detalle: 'Taper, papa...', Icono: Package },
]

export function SelectorTipoItem({ abierto, onOpenChange, onElegir }) {
  return (
    <Sheet open={abierto} onOpenChange={onOpenChange}>
      <SheetContent titulo="¿Qué agregamos?">
        <div className="grid gap-3 pb-2 sm:grid-cols-2">
          {OPCIONES.map(({ tipo, etiqueta, detalle, Icono }) => (
            <BotonGrande
              key={tipo}
              etiqueta={etiqueta}
              detalle={detalle}
              icono={Icono}
              className="min-h-20"
              onClick={() => onElegir(tipo)}
            />
          ))}
        </div>
      </SheetContent>
    </Sheet>
  )
}
