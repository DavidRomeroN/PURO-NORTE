import { useEffect, useState } from 'react'
import { Minus, Plus } from 'lucide-react'
import { Sheet, SheetPantalla } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { BotonGrande } from '@/components/common/BotonGrande'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { EstadoVacio } from '@/components/common/EstadoVacio'
import { useCatalogo } from '@/hooks/useCatalogo'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { TIPO_ITEM } from '@/utils/constantes'

/** Bebidas y extras: elegir uno y decir cuántos. */
export function SelectorSimple({ abierto, tipo, onOpenChange, onAgregar, guardando }) {
  const { bebidas, extras, cargando } = useCatalogo()
  const [producto, setProducto] = useState(null)
  const [cantidad, setCantidad] = useState(1)

  useEffect(() => {
    if (!abierto) {
      setProducto(null)
      setCantidad(1)
    }
  }, [abierto])

  const esBebida = tipo === TIPO_ITEM.BEBIDA
  const productos = esBebida ? bebidas : extras
  const titulo = esBebida ? 'Bebidas' : 'Extras'
  const total = producto ? Number(producto.precioUnitario) * cantidad : 0

  function agregar() {
    onAgregar([{ tipoItem: tipo, componentes: [producto.id], cantidad }])
  }

  return (
    <Sheet open={abierto} onOpenChange={onOpenChange}>
      <SheetPantalla
        titulo={titulo}
        subtitulo={producto ? producto.nombre : 'Elige uno'}
        pie={
          <Button
            tamano="grande"
            className="w-full"
            disabled={!producto || guardando}
            onClick={agregar}
          >
            {guardando ? 'Agregando...' : `Agregar — ${formatoMoneda(total)}`}
          </Button>
        }
      >
        {cargando ? (
          <EstadoCarga filas={4} alto="h-16" />
        ) : productos.length === 0 ? (
          <EstadoVacio
            titulo={`No hay ${titulo.toLowerCase()} cargadas`}
            descripcion="El administrador puede agregarlas desde Admin › Productos."
          />
        ) : (
          <div className="flex flex-col gap-4">
            <div className="grid gap-3 sm:grid-cols-2">
              {productos.map((opcion) => (
                <BotonGrande
                  key={opcion.id}
                  etiqueta={opcion.nombre}
                  monto={formatoMoneda(opcion.precioUnitario)}
                  seleccionado={producto?.id === opcion.id}
                  onClick={() => {
                    setProducto(opcion)
                    setCantidad(1)
                  }}
                />
              ))}
            </div>

            {producto ? (
              <Card className="flex items-center justify-between gap-3 p-3">
                <span className="text-lg font-bold text-carbon">¿Cuántos?</span>
                <div className="flex items-center gap-3">
                  <Button
                    variante="secundaria"
                    tamano="icono"
                    aria-label="Quitar uno"
                    disabled={cantidad <= 1}
                    onClick={() => setCantidad((valor) => Math.max(1, valor - 1))}
                  >
                    <Minus size={24} />
                  </Button>
                  <span className="monto w-10 text-center text-2xl font-bold text-carbon">
                    {cantidad}
                  </span>
                  <Button
                    variante="secundaria"
                    tamano="icono"
                    aria-label="Agregar uno"
                    onClick={() => setCantidad((valor) => valor + 1)}
                  >
                    <Plus size={24} />
                  </Button>
                </div>
              </Card>
            ) : null}
          </div>
        )}
      </SheetPantalla>
    </Sheet>
  )
}
