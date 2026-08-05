import { useEffect, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { Sheet, SheetContent, SheetPantalla } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { CardBoton } from '@/components/ui/card'
import { BotonGrande } from '@/components/common/BotonGrande'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { useCatalogo } from '@/hooks/useCatalogo'
import { calcularPrecioCombo, diferenciaDeSustitucion } from '@/hooks/useCalculoPrecio'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { TIPO_ITEM } from '@/utils/constantes'

export function ArmadorCombo({ abierto, onOpenChange, onAgregar, guardando, comboInicial = null }) {
  const { combos, anticuchos, cargando } = useCatalogo()
  const [combo, setCombo] = useState(null)
  const [sustituciones, setSustituciones] = useState({})
  const [slotEnEdicion, setSlotEnEdicion] = useState(null)

  useEffect(() => {
    if (!abierto) {
      setCombo(null)
      setSustituciones({})
      setSlotEnEdicion(null)
      return
    }
    if (comboInicial) {
      setCombo(comboInicial)
      setSustituciones({})
      setSlotEnEdicion(null)
    }
  }, [abierto, comboInicial])

  const total = calcularPrecioCombo(combo, sustituciones)

  function elegirReemplazo(slot, producto) {
    setSustituciones((actuales) => ({ ...actuales, [slot.id]: producto }))
    setSlotEnEdicion(null)
  }

  function agregar() {
    onAgregar([
      {
        tipoItem: TIPO_ITEM.COMBO,
        comboId: combo.id,
        sustituciones: Object.entries(sustituciones).map(([comboSlotId, producto]) => ({
          comboSlotId: Number(comboSlotId),
          productoBaseNuevoId: producto.id,
        })),
        cantidad: 1,
        paraLlevar: false,
      },
    ])
  }

  return (
    <Sheet open={abierto} onOpenChange={onOpenChange}>
      <SheetPantalla
        titulo={combo ? combo.nombre : 'Combos'}
        subtitulo={combo ? 'Toca un palito para cambiarlo' : 'Elige el combo'}
        onVolver={combo ? () => setCombo(null) : undefined}
        pie={
          combo ? (
            <Button tamano="grande" className="w-full" disabled={guardando} onClick={agregar}>
              {guardando ? 'Agregando...' : `Agregar — ${formatoMoneda(total)}`}
            </Button>
          ) : null
        }
      >
        {cargando ? (
          <EstadoCarga filas={3} />
        ) : !combo ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {combos.map((opcion) => (
              <BotonGrande
                key={opcion.id}
                etiqueta={opcion.nombre}
                detalle={`${opcion.slots?.length ?? 0} palitos`}
                monto={formatoMoneda(opcion.precioBase)}
                className="min-h-20"
                onClick={() => setCombo(opcion)}
              />
            ))}
          </div>
        ) : (
          <ul className="flex flex-col gap-3">
            {combo.slots.map((slot) => {
              const reemplazo = sustituciones[slot.id]
              const producto = reemplazo ?? slot.productoBaseDefault
              const bloqueado = slot.esCortesia || !slot.esSustituible
              const diferencia = reemplazo ? diferenciaDeSustitucion(slot, reemplazo) : 0

              if (bloqueado) {
                return (
                  <li
                    key={slot.id}
                    aria-disabled="true"
                    className="flex min-h-16 items-center gap-3 rounded-app border border-dashed border-borde-fuerte bg-hundido/60 px-4 py-3"
                  >
                    <span className="grow">
                      <span className="block text-lg font-semibold text-tinta">
                        {producto.nombre}
                      </span>
                      <span className="block text-sm text-tinta">No se puede cambiar</span>
                    </span>
                    <Badge tono="hoja">Cortesía</Badge>
                  </li>
                )
              }

              return (
                <li key={slot.id}>
                  <CardBoton
                    seleccionada={Boolean(reemplazo)}
                    onClick={() => setSlotEnEdicion(slot)}
                    className="flex min-h-16 items-center gap-3 px-4 py-3"
                  >
                    <span className="min-w-0 grow">
                      <span className="block truncate text-lg font-semibold text-carbon">
                        {producto.nombre}
                      </span>
                      {reemplazo ? (
                        <span className="monto block text-sm font-bold text-brasa-700">
                          {diferencia > 0
                            ? `+${formatoMoneda(diferencia)}`
                            : 'Sin costo adicional'}
                        </span>
                      ) : (
                        <span className="block text-sm text-tinta">Toca para cambiar</span>
                      )}
                    </span>
                    <ChevronRight size={24} className="shrink-0 text-tinta" />
                  </CardBoton>
                </li>
              )
            })}
          </ul>
        )}

        <Sheet
          open={Boolean(slotEnEdicion)}
          onOpenChange={(valor) => !valor && setSlotEnEdicion(null)}
        >
          <SheetContent titulo="Cambiar por">
            <ul className="flex flex-col gap-3 pb-2">
              {slotEnEdicion
                ? anticuchos.map((producto) => {
                    const diferencia = diferenciaDeSustitucion(slotEnEdicion, producto)
                    return (
                      <li key={producto.id}>
                        <BotonGrande
                          etiqueta={producto.nombre}
                          detalle={
                            diferencia > 0
                              ? `${formatoMoneda(producto.precioUnitario)} · cuesta más`
                              : 'Sin costo adicional'
                          }
                          monto={diferencia > 0 ? `+${formatoMoneda(diferencia)}` : 'S/ 0.00'}
                          seleccionado={
                            sustituciones[slotEnEdicion.id]?.id === producto.id ||
                            slotEnEdicion.productoBaseDefault.id === producto.id
                          }
                          onClick={() => elegirReemplazo(slotEnEdicion, producto)}
                        />
                      </li>
                    )
                  })
                : null}
            </ul>
          </SheetContent>
        </Sheet>
      </SheetPantalla>
    </Sheet>
  )
}
