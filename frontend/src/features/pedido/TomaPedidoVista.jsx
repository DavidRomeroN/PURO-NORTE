import { useMemo, useState } from 'react'
import { MessageCircle, Minus, Plus, RefreshCw, X } from 'lucide-react'
import { Sheet, SheetPantalla } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { ArmadorCombo } from './ArmadorCombo'
import { useCatalogo } from '@/hooks/useCatalogo'
import { calcularPrecioAnticucho, calcularPrecioCombo } from '@/hooks/useCalculoPrecio'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { claveAgrupacion } from '@/utils/agruparItems'
import { descripcionCocina } from '@/utils/descripcionCocina'
import { TIPO_ITEM } from '@/utils/constantes'
import { cn } from '@/utils/cn'

const MODOS = [
  { id: 'SIMPLE', etiqueta: 'SIMPLE', sticks: 1 },
  { id: 'DOBLE', etiqueta: 'DOBLE', sticks: 2 },
  { id: 'TRIPLE', etiqueta: 'TRIPLE', sticks: 3 },
  { id: 'COMBO', etiqueta: 'COMBO', sticks: 0 },
  { id: 'BEBIDAS', etiqueta: 'BEBIDAS', sticks: 0 },
  { id: 'EXTRAS', etiqueta: 'EXTRAS', sticks: 0 },
]

const CHIPS_NOTA = ['sin ají', 'bien cocido', 'poco cocido', 'sin papa', 'para llevar']

function claveLinea(linea) {
  return claveAgrupacion({
    tipoItem: linea.tipoItem,
    comboId: linea.comboId,
    observaciones: linea.observaciones,
    paraLlevar: linea.paraLlevar,
    componentes: (linea.componentesDetalle ?? []).map((c) => ({
      productoBaseId: c.id,
      productoNombre: c.nombre,
      comboSlotId: c.comboSlotId,
      esSustitucion: c.esSustitucion,
      productoOriginalNombre: c.productoOriginalNombre,
    })),
  })
}

function precioPreview(linea) {
  if (linea.tipoItem === TIPO_ITEM.ANTICUCHO) {
    return calcularPrecioAnticucho(linea.componentesDetalle) * linea.cantidad
  }
  if (linea.tipoItem === TIPO_ITEM.COMBO && linea.combo) {
    return calcularPrecioCombo(linea.combo, linea.sustitucionesPorSlot ?? {}) * linea.cantidad
  }
  const unit = Number(linea.componentesDetalle?.[0]?.precioUnitario ?? 0)
  return unit * linea.cantidad
}

/**
 * Toma de pedidos en una sola vista: modo + botones grandes + canasta fija.
 */
export function TomaPedidoVista({ abierto, onOpenChange, onEnviar, enviando }) {
  const { anticuchos, bebidas, extras, combos, cargando } = useCatalogo()
  const [modo, setModo] = useState('SIMPLE')
  const [parcial, setParcial] = useState([])
  const [canasta, setCanasta] = useState([])
  const [notaLineaId, setNotaLineaId] = useState(null)
  const [textoNota, setTextoNota] = useState('')
  /** Línea de canasta cuyo mixto se está editando (Cambiar). */
  const [editandoComboId, setEditandoComboId] = useState(null)

  const sticksNecesarios = MODOS.find((m) => m.id === modo)?.sticks ?? 0

  const total = useMemo(
    () => canasta.reduce((s, l) => s + precioPreview(l), 0),
    [canasta],
  )

  function resetParcial() {
    setParcial([])
  }

  function agregarACanasta(lineaNueva) {
    setCanasta((prev) => {
      // Los mixtos van en filas aparte: cada uno se puede "Cambiar" por su cuenta.
      if (lineaNueva.tipoItem === TIPO_ITEM.COMBO) {
        return [...prev, { ...lineaNueva, idLocal: `${Date.now()}-${Math.random()}` }]
      }
      const clave = claveLinea(lineaNueva)
      const idx = prev.findIndex((l) => claveLinea(l) === clave && !l.observaciones)
      if (idx >= 0 && !lineaNueva.observaciones) {
        const copia = [...prev]
        copia[idx] = { ...copia[idx], cantidad: copia[idx].cantidad + lineaNueva.cantidad }
        return copia
      }
      return [...prev, { ...lineaNueva, idLocal: `${Date.now()}-${Math.random()}` }]
    })
  }

  function tocarAnticucho(producto) {
    if (modo === 'SIMPLE') {
      agregarACanasta({
        tipoItem: TIPO_ITEM.ANTICUCHO,
        cantidad: 1,
        componentes: [producto.id],
        componentesDetalle: [producto],
        paraLlevar: false,
        observaciones: null,
      })
      return
    }
    const siguiente = [...parcial, producto]
    setParcial(siguiente)
    if (siguiente.length >= sticksNecesarios) {
      agregarACanasta({
        tipoItem: TIPO_ITEM.ANTICUCHO,
        cantidad: 1,
        componentes: siguiente.map((p) => p.id),
        componentesDetalle: siguiente,
        paraLlevar: false,
        observaciones: null,
      })
      setParcial([])
    }
  }

  function tocarSimple(producto, tipoItem) {
    agregarACanasta({
      tipoItem,
      cantidad: 1,
      componentes: [producto.id],
      componentesDetalle: [producto],
      paraLlevar: false,
      observaciones: null,
    })
  }

  /** Un toque en el mixto: va tal cual a la canasta. Los cambios se hacen después. */
  function tocarCombo(combo) {
    agregarACanasta({
      tipoItem: TIPO_ITEM.COMBO,
      comboId: combo.id,
      comboNombre: combo.nombre,
      combo,
      cantidad: 1,
      componentes: [],
      componentesDetalle: (combo.slots ?? []).map((slot) => ({
        id: slot.productoBaseDefault.id,
        nombre: slot.productoBaseDefault.nombre,
        precioUnitario: slot.productoBaseDefault.precioUnitario,
        comboSlotId: slot.id,
        esSustitucion: false,
      })),
      sustituciones: [],
      sustitucionesPorSlot: {},
      paraLlevar: false,
      observaciones: null,
    })
  }

  const lineaEditandoCombo = canasta.find((l) => l.idLocal === editandoComboId) ?? null

  function cambiarCantidad(idLocal, delta) {
    setCanasta((prev) =>
      prev
        .map((l) => (l.idLocal === idLocal ? { ...l, cantidad: l.cantidad + delta } : l))
        .filter((l) => l.cantidad > 0),
    )
  }

  function abrirNota(linea) {
    setNotaLineaId(linea.idLocal)
    setTextoNota(linea.observaciones ?? '')
  }

  function guardarNota() {
    const nota = textoNota.trim()
    setCanasta((prev) => {
      const idx = prev.findIndex((l) => l.idLocal === notaLineaId)
      if (idx < 0) return prev
      const linea = prev[idx]
      const copia = [...prev]
      if (linea.cantidad > 1 && nota) {
        copia[idx] = { ...linea, cantidad: linea.cantidad - 1 }
        copia.push({
          ...linea,
          idLocal: `${Date.now()}-nota`,
          cantidad: 1,
          observaciones: nota,
          paraLlevar: nota.toLowerCase().includes('para llevar') ? true : linea.paraLlevar,
        })
      } else {
        copia[idx] = {
          ...linea,
          observaciones: nota || null,
          paraLlevar: nota.toLowerCase().includes('para llevar') ? true : linea.paraLlevar,
        }
      }
      return copia
    })
    setNotaLineaId(null)
    setTextoNota('')
  }

  async function enviar() {
    if (!canasta.length || enviando) return
    const items = canasta.map((l) => ({
      tipoItem: l.tipoItem,
      comboId: l.comboId,
      componentes: l.componentes,
      cantidad: l.cantidad,
      paraLlevar: Boolean(l.paraLlevar),
      observaciones: l.observaciones,
      sustituciones: l.sustituciones,
    }))
    await onEnviar(items)
    setCanasta([])
    resetParcial()
    onOpenChange(false)
  }

  function cerrar() {
    if (enviando) return
    setCanasta([])
    resetParcial()
    onOpenChange(false)
  }

  const indicadorParcial =
    parcial.length > 0 && sticksNecesarios > 1
      ? `${modo === 'DOBLE' ? 'Doble' : 'Triple'}: ${parcial.map((p) => p.nombre).join(' + ')}${' + ___'.repeat(sticksNecesarios - parcial.length)}`
      : null

  return (
    <>
      <Sheet open={abierto} onOpenChange={(o) => !o && cerrar()}>
        <SheetPantalla titulo="Agregar al pedido" onCerrar={cerrar}>
          {cargando ? (
            <EstadoCarga />
          ) : (
            <div className="flex h-full min-h-0 flex-col gap-3">
              <div className="flex flex-wrap gap-2">
                {MODOS.map((m) => (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => {
                      setModo(m.id)
                      resetParcial()
                    }}
                    className={cn(
                      'min-h-12 rounded-app px-3 text-sm font-extrabold',
                      modo === m.id ? 'bg-brasa-600 text-white' : 'bg-hundido text-carbon',
                    )}
                  >
                    {m.etiqueta}
                  </button>
                ))}
              </div>

              {indicadorParcial ? (
                <div className="flex items-center justify-between rounded-app bg-brasa-50 px-3 py-2 text-sm font-semibold text-brasa-700">
                  <span>{indicadorParcial}</span>
                  <button type="button" className="min-h-12 min-w-12" onClick={resetParcial} aria-label="Cancelar armado">
                    <X size={22} />
                  </button>
                </div>
              ) : null}

              <div className="min-h-0 flex-1 overflow-y-auto">
                {(modo === 'SIMPLE' || modo === 'DOBLE' || modo === 'TRIPLE') && (
                  <div className="grid grid-cols-2 gap-2">
                    {anticuchos.map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => tocarAnticucho(p)}
                        className="min-h-[5.5rem] rounded-app border-2 border-borde bg-superficie px-2 py-3 text-center active:bg-brasa-50"
                      >
                        <p className="text-lg font-extrabold text-carbon">{p.nombre}</p>
                        <p className="monto text-base font-bold text-tinta">{formatoMoneda(p.precioUnitario)}</p>
                      </button>
                    ))}
                  </div>
                )}

                {modo === 'COMBO' && (
                  <div className="grid grid-cols-1 gap-2">
                    {combos.map((c) => (
                      <button
                        key={c.id}
                        type="button"
                        onClick={() => tocarCombo(c)}
                        className="min-h-16 rounded-app border-2 border-borde bg-superficie px-4 py-3 text-left active:bg-brasa-50"
                      >
                        <p className="text-lg font-extrabold">{c.nombre}</p>
                        <p className="monto text-tinta">{formatoMoneda(c.precioBase)}</p>
                      </button>
                    ))}
                  </div>
                )}

                {modo === 'BEBIDAS' && (
                  <div className="grid grid-cols-2 gap-2">
                    {bebidas.map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => tocarSimple(p, TIPO_ITEM.BEBIDA)}
                        className="min-h-[5.5rem] rounded-app border-2 border-borde bg-superficie px-2 py-3"
                      >
                        <p className="text-lg font-extrabold">{p.nombre}</p>
                        <p className="monto font-bold">{formatoMoneda(p.precioUnitario)}</p>
                      </button>
                    ))}
                  </div>
                )}

                {modo === 'EXTRAS' && (
                  <div className="grid grid-cols-2 gap-2">
                    {extras.map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => tocarSimple(p, TIPO_ITEM.EXTRA)}
                        className="min-h-[5.5rem] rounded-app border-2 border-borde bg-superficie px-2 py-3"
                      >
                        <p className="text-lg font-extrabold">{p.nombre}</p>
                        <p className="monto font-bold">{formatoMoneda(p.precioUnitario)}</p>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="shrink-0 rounded-app border-2 border-carbon/10 bg-hundido/40 p-3">
                <p className="mb-2 text-sm font-extrabold tracking-wide text-tinta">CANASTA</p>
                {canasta.length === 0 ? (
                  <p className="text-sm text-tinta">Toca un producto para agregar</p>
                ) : (
                  <ul className="flex max-h-48 flex-col gap-2 overflow-y-auto">
                    {canasta.map((l) => {
                      const etiqueta =
                        l.tipoItem === TIPO_ITEM.COMBO || l.tipoItem === TIPO_ITEM.ANTICUCHO
                          ? descripcionCocina({
                              tipoItem: l.tipoItem,
                              comboNombre: l.comboNombre,
                              componentes: (l.componentesDetalle ?? []).map((c) => ({
                                productoBaseId: c.id,
                                productoNombre: c.nombre,
                                comboSlotId: c.comboSlotId,
                                esSustitucion: c.esSustitucion,
                                productoOriginalNombre: c.productoOriginalNombre,
                              })),
                            })
                          : (l.componentesDetalle ?? []).map((c) => c.nombre).join(' + ')
                      return (
                        <li key={l.idLocal} className="flex items-start gap-1.5">
                          <div className="min-w-0 grow">
                            <p className="font-bold leading-tight text-carbon">{etiqueta}</p>
                            {l.observaciones ? (
                              <p className="text-xs text-tinta">{l.observaciones}</p>
                            ) : null}
                          </div>
                          <div className="flex shrink-0 items-center gap-0.5">
                            {l.tipoItem === TIPO_ITEM.COMBO ? (
                              <button
                                type="button"
                                className="flex min-h-12 items-center gap-1 rounded-app bg-brasa-50 px-2 text-sm font-bold text-brasa-700"
                                onClick={() => setEditandoComboId(l.idLocal)}
                              >
                                <RefreshCw size={16} />
                                Cambiar
                              </button>
                            ) : null}
                            <button
                              type="button"
                              className="flex size-12 items-center justify-center rounded-app bg-superficie"
                              onClick={() => cambiarCantidad(l.idLocal, -1)}
                              aria-label="Menos uno"
                            >
                              <Minus size={20} />
                            </button>
                            <span className="monto w-6 text-center text-lg font-extrabold">
                              {l.cantidad}
                            </span>
                            <button
                              type="button"
                              className="flex size-12 items-center justify-center rounded-app bg-superficie"
                              onClick={() => cambiarCantidad(l.idLocal, 1)}
                              aria-label="Más uno"
                            >
                              <Plus size={20} />
                            </button>
                            <button
                              type="button"
                              className="flex size-12 items-center justify-center rounded-app bg-superficie"
                              onClick={() => abrirNota(l)}
                              aria-label="Comentario"
                            >
                              <MessageCircle size={20} />
                            </button>
                          </div>
                        </li>
                      )
                    })}
                  </ul>
                )}

                <div className="mt-3 flex items-center gap-3">
                  <p className="monto grow text-xl font-extrabold">{formatoMoneda(total)}</p>
                  <Button
                    tamano="grande"
                    className="min-w-[9rem]"
                    disabled={!canasta.length || enviando}
                    onClick={enviar}
                  >
                    {enviando ? 'Agregando...' : 'AGREGAR'}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </SheetPantalla>
      </Sheet>

      {notaLineaId ? (
        <div className="fixed inset-0 z-[60] flex items-end justify-center bg-carbon/40 p-4 sm:items-center">
          <div className="w-full max-w-md rounded-app bg-superficie p-4 shadow-lg">
            <p className="mb-3 text-lg font-extrabold">Comentario</p>
            <Input value={textoNota} onChange={(e) => setTextoNota(e.target.value)} placeholder="Ej. sin ají" />
            <div className="mt-3 flex flex-wrap gap-2">
              {CHIPS_NOTA.map((chip) => (
                <button
                  key={chip}
                  type="button"
                  className="min-h-12 rounded-full bg-hundido px-3 text-sm font-semibold"
                  onClick={() =>
                    setTextoNota((t) => (t.toLowerCase().includes(chip) ? t : t ? `${t} · ${chip}` : chip))
                  }
                >
                  {chip}
                </button>
              ))}
            </div>
            <div className="mt-4 flex gap-2">
              <Button variante="secundaria" className="grow" onClick={() => setNotaLineaId(null)}>
                Cancelar
              </Button>
              <Button className="grow" onClick={guardarNota}>
                Listo
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      <ArmadorCombo
        abierto={Boolean(lineaEditandoCombo)}
        comboInicial={lineaEditandoCombo?.combo ?? null}
        sustitucionesIniciales={lineaEditandoCombo?.sustitucionesPorSlot ?? {}}
        onOpenChange={(o) => !o && setEditandoComboId(null)}
        guardando={false}
        onGuardar={(editado) => {
          setCanasta((prev) =>
            prev.map((l) =>
              l.idLocal === editandoComboId
                ? {
                    ...l,
                    comboNombre: editado.comboNombre,
                    combo: editado.combo,
                    comboId: editado.comboId,
                    componentesDetalle: editado.componentesDetalle,
                    sustituciones: editado.sustituciones,
                    sustitucionesPorSlot: editado.sustitucionesPorSlot,
                  }
                : l,
            ),
          )
          setEditandoComboId(null)
        }}
      />
    </>
  )
}
