import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { AvisoCambioPrecio } from '@/components/common/AvisoCambioPrecio'
import { BotonGrande } from '@/components/common/BotonGrande'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardBoton } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Sheet, SheetContent, SheetPantalla } from '@/components/ui/sheet'
import { catalogoApi } from '@/api/catalogoApi'
import { mensajeDeError } from '@/api/axiosClient'
import { useCatalogo } from '@/hooks/useCatalogo'
import { formatoMoneda, formatoNumero } from '@/utils/formatoMoneda'

const REGLA_SUSTITUCION =
  'Si un cliente cambia un anticucho por uno más caro, el precio sube la diferencia. ' +
  'Si lo cambia por uno más barato o igual, el precio se mantiene.'

export function CombosPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { combos, anticuchos, cargando } = useCatalogo()
  const [abierto, setAbierto] = useState(null)

  const refrescar = () => {
    queryClient.invalidateQueries({ queryKey: ['combos'] })
    queryClient.invalidateQueries({ queryKey: ['productos'] })
  }

  const guardarPrecio = useMutation({
    mutationFn: ({ combo, precioBase }) =>
      catalogoApi.actualizarCombo(combo.id, { nombre: combo.nombre, precioBase }),
    onSuccess: (combo) => {
      refrescar()
      toast.success(`Precio del ${combo.nombre} actualizado a ${formatoMoneda(combo.precioBase)}`)
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const cambiarContenido = useMutation({
    mutationFn: ({ combo, slot, producto }) =>
      catalogoApi.actualizarSlot(combo.id, slot.id, {
        productoBaseDefaultId: producto.id,
        orden: slot.orden,
        esCortesia: slot.esCortesia,
        esSustituible: slot.esSustituible,
      }),
    onSuccess: (combo, variables) => {
      refrescar()
      toast.success(
        `El ${combo.nombre} ahora trae ${variables.producto.nombre} en vez de ${variables.slot.productoBaseDefault.nombre}`,
      )
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  // La lista se recarga tras cada cambio: hay que releer el combo abierto de los datos frescos.
  const comboAbierto = abierto ? (combos.find((c) => c.id === abierto) ?? null) : null

  return (
    <>
      <AppShell
        titulo="Combos"
        subtitulo="Precio y contenido"
        onVolver={() => navigate('/admin')}
        conNav={false}
        ancho="ancho"
      >
        {cargando ? (
          <EstadoCarga filas={2} alto="h-32" />
        ) : (
          <ul className="grid gap-3 lg:grid-cols-2">
            {combos.map((combo) => (
              <li key={combo.id}>
                <CardBoton onClick={() => setAbierto(combo.id)} className="h-full p-4">
                  <div className="flex items-start gap-3">
                    <div className="min-w-0 grow">
                      <p className="text-xl font-bold text-carbon">{combo.nombre}</p>
                      <p className="monto text-3xl font-extrabold leading-tight text-brasa-600">
                        {formatoMoneda(combo.precioBase)}
                      </p>
                    </div>
                    <ChevronRight size={26} className="mt-1 shrink-0 text-tinta" />
                  </div>
                  <p className="mt-2 text-base text-tinta">{describirContenido(combo)}</p>
                </CardBoton>
              </li>
            ))}
          </ul>
        )}
      </AppShell>

      {comboAbierto ? (
        <DetalleCombo
          combo={comboAbierto}
          anticuchos={anticuchos}
          guardandoPrecio={guardarPrecio.isPending}
          cambiandoContenido={cambiarContenido.isPending}
          onCerrar={() => setAbierto(null)}
          onGuardarPrecio={(precioBase) =>
            guardarPrecio.mutateAsync({ combo: comboAbierto, precioBase })
          }
          onCambiarContenido={(slot, producto) =>
            cambiarContenido.mutate({ combo: comboAbierto, slot, producto })
          }
        />
      ) : null}
    </>
  )
}

function DetalleCombo({
  combo,
  anticuchos,
  onCerrar,
  onGuardarPrecio,
  onCambiarContenido,
  guardandoPrecio,
  cambiandoContenido,
}) {
  const [editandoPrecio, setEditandoPrecio] = useState(false)
  const [slotEnEdicion, setSlotEnEdicion] = useState(null)

  return (
    <Sheet open onOpenChange={(valor) => !valor && onCerrar()}>
      <SheetPantalla titulo={combo.nombre} subtitulo="Precio y contenido">
        <div className="flex flex-col gap-6">
          <section>
            <h2 className="mb-2 text-lg font-bold text-tinta">Precio del combo</h2>
            <Card className="flex items-center gap-3 p-4">
              <span className="monto grow text-3xl font-extrabold text-carbon">
                {formatoMoneda(combo.precioBase)}
              </span>
              <Button variante="secundaria" onClick={() => setEditandoPrecio(true)}>
                Cambiar
              </Button>
            </Card>
          </section>

          <section>
            <h2 className="mb-2 text-lg font-bold text-tinta">Qué incluye</h2>
            <ul className="flex flex-col gap-2">
              {combo.slots.map((slot) => {
                const bloqueado = slot.esCortesia || !slot.esSustituible

                if (bloqueado) {
                  return (
                    <li
                      key={slot.id}
                      aria-disabled="true"
                      className="flex items-center gap-3 rounded-app border border-dashed border-borde-fuerte bg-hundido/60 px-4 py-3"
                    >
                      <span className="grow">
                        <span className="block text-lg font-semibold text-tinta">
                          {slot.productoBaseDefault.nombre}
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
                      onClick={() => setSlotEnEdicion(slot)}
                      className="flex min-h-16 items-center gap-3 px-4 py-3"
                    >
                      <span className="min-w-0 grow">
                        <span className="block truncate text-lg font-bold text-carbon">
                          {slot.productoBaseDefault.nombre}
                        </span>
                        <span className="block text-sm text-tinta">Toca para cambiarlo</span>
                      </span>
                      <ChevronRight size={24} className="shrink-0 text-tinta" />
                    </CardBoton>
                  </li>
                )
              })}
            </ul>

            <p className="mt-3 rounded-app border-l-4 border-brasa-300 bg-brasa-50 p-4 text-base text-carbon">
              {REGLA_SUSTITUCION}
            </p>
          </section>
        </div>
      </SheetPantalla>

      <EditorPrecioCombo
        combo={editandoPrecio ? combo : null}
        guardando={guardandoPrecio}
        onCerrar={() => setEditandoPrecio(false)}
        onGuardar={async (precio) => {
          try {
            await onGuardarPrecio(precio)
            setEditandoPrecio(false)
          } catch {
            // El error ya se avisó con un toast; la hoja queda abierta para reintentar.
          }
        }}
      />

      <Sheet open={Boolean(slotEnEdicion)} onOpenChange={(v) => !v && setSlotEnEdicion(null)}>
        <SheetContent titulo="Cambiar por">
          <ul className="flex flex-col gap-3 pb-2">
            {anticuchos.map((producto) => (
              <li key={producto.id}>
                <BotonGrande
                  etiqueta={producto.nombre}
                  monto={formatoMoneda(producto.precioUnitario)}
                  seleccionado={slotEnEdicion?.productoBaseDefault.id === producto.id}
                  deshabilitado={cambiandoContenido}
                  onClick={() => {
                    if (slotEnEdicion.productoBaseDefault.id !== producto.id) {
                      onCambiarContenido(slotEnEdicion, producto)
                    }
                    setSlotEnEdicion(null)
                  }}
                />
              </li>
            ))}
          </ul>
        </SheetContent>
      </Sheet>
    </Sheet>
  )
}

function EditorPrecioCombo({ combo, onCerrar, onGuardar, guardando }) {
  const [precio, setPrecio] = useState('')
  const [confirmando, setConfirmando] = useState(false)

  useEffect(() => {
    if (combo) {
      setPrecio(formatoNumero(combo.precioBase))
      setConfirmando(false)
    }
  }, [combo])

  const valido = /^\d+([.,]\d{1,2})?$/.test(precio.trim()) && Number(precio.replace(',', '.')) > 0
  const valor = Number(precio.replace(',', '.'))
  const anterior = combo ? Number(combo.precioBase) : 0
  const cambia = valor !== anterior

  return (
    <Sheet open={Boolean(combo)} onOpenChange={(abierto) => !abierto && onCerrar()}>
      <SheetContent titulo="Precio del combo">
        {confirmando ? (
          <div className="flex flex-col gap-4 pb-2">
            <AvisoCambioPrecio nombre={combo.nombre} anterior={anterior} nuevo={valor} />
            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={() => setConfirmando(false)}>
                Cancelar
              </Button>
              <Button tamano="grande" disabled={guardando} onClick={() => onGuardar(valor)}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-4 pb-2">
            <div className="flex flex-col gap-2">
              <Label htmlFor="precio-combo">Precio</Label>
              <div className="flex items-center gap-2">
                <span className="text-2xl font-bold text-tinta">S/</span>
                <Input
                  id="precio-combo"
                  inputMode="decimal"
                  value={precio}
                  onChange={(e) => setPrecio(e.target.value.replace(/[^\d.,]/g, ''))}
                  className="monto text-2xl font-bold"
                />
              </div>
              {precio && !valido ? (
                <p className="text-base font-semibold text-alerta">
                  Escribe un precio mayor a cero, con máximo dos decimales.
                </p>
              ) : null}
            </div>

            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={onCerrar}>
                Cancelar
              </Button>
              <Button
                tamano="grande"
                disabled={!valido || guardando}
                onClick={() => (cambia ? setConfirmando(true) : onCerrar())}
              >
                Guardar
              </Button>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}

/** "Incluye: pollo, carne, corazón y una salchicha de cortesía." */
function describirContenido(combo) {
  const normales = combo.slots
    .filter((slot) => !slot.esCortesia)
    .map((slot) => slot.productoBaseDefault.nombre.toLowerCase())

  const cortesias = combo.slots
    .filter((slot) => slot.esCortesia)
    .map((slot) => `una ${slot.productoBaseDefault.nombre.toLowerCase()} de cortesía`)

  const partes = [...normales, ...cortesias]
  if (partes.length === 0) return 'Todavía no tiene contenido.'
  if (partes.length === 1) return `Incluye: ${partes[0]}.`

  return `Incluye: ${partes.slice(0, -1).join(', ')} y ${partes[partes.length - 1]}.`
}
