import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Check, CheckCheck, ChevronLeft, Flame, RotateCcw, Wallet } from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { Button } from '@/components/ui/button'
import { CardBoton } from '@/components/ui/card'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { EstadoVacio } from '@/components/common/EstadoVacio'
import { mensajeDeError } from '@/api/axiosClient'
import { useAccionesPedido, useParrilla } from '@/hooks/usePedido'
import { useAuth } from '@/hooks/useAuth'
import { detalleItem, etiquetaItem, nombrePedido } from '@/utils/etiquetas'
import { horaCorta } from '@/utils/fechas'
import { ESTADO_DESPACHO } from '@/utils/constantes'
import { cn } from '@/utils/cn'

/**
 * Pantalla del parrillero. Pensada para tablet a distancia: tipografía grande,
 * contraste alto y botones grandes. Se actualiza sola cada 5 segundos.
 *
 * Flujo:
 * 1. Lista de pedidos con platos pendientes (el más viejo primero).
 * 2. Tocás uno → ves qué falta y qué ya pasó.
 * 3. Tocás un plato para marcarlo, o "Pasar todo" si salió junto.
 * 4. Los que faltan siguen arriba; los pasados quedan abajo por si hay que deshacer.
 */
export function ParrillaPage() {
  const navigate = useNavigate()
  const { puedeCobrar } = useAuth()
  const { data: pedidos = [], isPending } = useParrilla()
  const { marcarDespacho, despacharTodo } = useAccionesPedido()
  const [pedidoId, setPedidoId] = useState(null)
  const [mostrarListos, setMostrarListos] = useState(false)

  const pendientes = useMemo(
    () => pedidos.filter((pedido) => (pedido.pendientesDespacho ?? 0) > 0),
    [pedidos],
  )
  const listos = useMemo(
    () => pedidos.filter((pedido) => (pedido.pendientesDespacho ?? 0) === 0),
    [pedidos],
  )

  const pedido = pedidos.find((p) => p.id === pedidoId) ?? null

  // Si el pedido elegido se cobró o anuló y desapareció del listado, vuelve a la lista.
  useEffect(() => {
    if (pedidoId && !isPending && !pedido) {
      setPedidoId(null)
    }
  }, [pedidoId, isPending, pedido])

  async function pasarItem(item, despachado) {
    try {
      await marcarDespacho.mutateAsync({
        pedidoId: pedido.id,
        itemId: item.id,
        despachado,
      })
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  async function pasarTodo() {
    try {
      await despacharTodo.mutateAsync(pedido.id)
      toast.success(
        puedeCobrar
          ? 'Todo pasado. Ya puedes cobrarlo en Caja.'
          : 'Todo el pedido marcado como pasado',
      )
      // Queda en "Ya pasaron": el parrillero sigue la lista; caja ve el aviso verde.
      setMostrarListos(true)
      setPedidoId(null)
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  if (pedido) {
    return (
      <DetallePedido
        pedido={pedido}
        onVolver={() => setPedidoId(null)}
        onPasarItem={pasarItem}
        onPasarTodo={pasarTodo}
        pasando={marcarDespacho.isPending || despacharTodo.isPending}
        puedeCobrar={puedeCobrar}
        onCobrar={() => navigate(`/cobrar/${pedido.id}`)}
      />
    )
  }

  const lista = mostrarListos ? listos : pendientes

  return (
    <AppShell
      titulo="Parrilla"
      subtitulo={
        isPending
          ? 'Cargando...'
          : pendientes.length === 0
            ? 'Nada pendiente'
            : `${pendientes.length} pedido${pendientes.length === 1 ? '' : 's'} por pasar`
      }
      ancho="ancho"
    >
      <div className="mb-4 flex gap-2">
        <FiltroLista
          etiqueta="Por pasar"
          cantidad={pendientes.length}
          activo={!mostrarListos}
          onClick={() => setMostrarListos(false)}
        />
        <FiltroLista
          etiqueta="Ya pasaron"
          cantidad={listos.length}
          activo={mostrarListos}
          onClick={() => setMostrarListos(true)}
        />
      </div>

      {isPending ? (
        <EstadoCarga filas={4} />
      ) : lista.length === 0 ? (
        <EstadoVacio
          icono={Flame}
          titulo={mostrarListos ? 'Ningún pedido pasado todavía' : 'La parrilla está al día'}
          descripcion={
            mostrarListos
              ? 'Cuando marques un pedido completo, aparece acá.'
              : 'Cuando el mozo anote un plato, sale acá solo.'
          }
        />
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {lista.map((item) => (
            <TarjetaPedido
              key={item.id}
              pedido={item}
              onClick={() => setPedidoId(item.id)}
              puedeCobrar={puedeCobrar}
              onCobrar={() => navigate(`/cobrar/${item.id}`)}
            />
          ))}
        </div>
      )}
    </AppShell>
  )
}

function FiltroLista({ etiqueta, cantidad, activo, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'min-h-14 flex-1 rounded-app border-2 px-4 text-lg font-bold transition-colors',
        activo
          ? 'border-brasa-500 bg-brasa-50 text-brasa-700'
          : 'border-borde bg-superficie text-tinta hover:border-borde-fuerte',
      )}
    >
      {etiqueta}
      <span className="ml-2 tabular-nums opacity-80">({cantidad})</span>
    </button>
  )
}

function TarjetaPedido({ pedido, onClick, puedeCobrar, onCobrar }) {
  const pendientes = pedido.pendientesDespacho ?? 0
  const total = pedido.items?.length ?? 0
  const listo = pendientes === 0

  return (
    <CardBoton
      onClick={onClick}
      className={cn(
        'flex min-h-36 flex-col justify-between gap-3 p-5',
        listo ? 'border-hoja-300 bg-hoja-50' : 'border-brasa-200',
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-3xl font-extrabold leading-none tracking-tight text-carbon">
            {nombrePedido(pedido)}
          </p>
          <p className="mt-2 text-lg text-tinta">
            {horaCorta(pedido.creadoEn)}
            <span className="mx-2 text-borde-fuerte">·</span>
            {minutosEspera(pedido.creadoEn)}
          </p>
        </div>
        <span
          className={cn(
            'flex min-h-14 min-w-14 shrink-0 items-center justify-center rounded-full text-2xl font-extrabold tabular-nums',
            listo ? 'bg-hoja-600 text-white' : 'bg-brasa-600 text-white',
          )}
          aria-label={listo ? 'Todo pasado' : `${pendientes} pendientes`}
        >
          {listo ? <Check size={28} strokeWidth={3} /> : pendientes}
        </span>
      </div>

      <p className="text-xl font-bold text-carbon">
        {listo
          ? `Todo pasado · ${total} plato${total === 1 ? '' : 's'}`
          : `${pendientes} de ${total} por pasar`}
      </p>

      {!listo ? (
        <p className="line-clamp-2 text-lg leading-snug text-tinta">
          {(pedido.items ?? [])
            .filter((item) => item.estadoDespacho !== ESTADO_DESPACHO.DESPACHADO)
            .map((item) => `${item.cantidad > 1 ? `${item.cantidad}× ` : ''}${etiquetaItem(item)}`)
            .join(' · ')}
        </p>
      ) : null}

      {listo && puedeCobrar ? (
        <Button
          tamano="grande"
          className="w-full"
          onClick={(evento) => {
            evento.stopPropagation()
            onCobrar()
          }}
        >
          <Wallet size={24} />
          Cobrar
        </Button>
      ) : null}
    </CardBoton>
  )
}

function DetallePedido({
  pedido,
  onVolver,
  onPasarItem,
  onPasarTodo,
  pasando,
  puedeCobrar,
  onCobrar,
}) {
  const pendientes = (pedido.items ?? []).filter(
    (item) => item.estadoDespacho !== ESTADO_DESPACHO.DESPACHADO,
  )
  const pasados = (pedido.items ?? []).filter(
    (item) => item.estadoDespacho === ESTADO_DESPACHO.DESPACHADO,
  )

  return (
    <AppShell
      titulo={nombrePedido(pedido)}
      subtitulo={`${horaCorta(pedido.creadoEn)} · ${minutosEspera(pedido.creadoEn)}`}
      onVolver={onVolver}
      conNav={false}
      ancho="ancho"
      barraInferior={
        <div className="border-t border-borde bg-superficie px-4 py-3 pt-[max(0.75rem,env(safe-area-inset-bottom))] shadow-[0_-8px_24px_rgba(28,22,19,0.08)] lg:pl-64">
          <div className="mx-auto flex w-full max-w-6xl gap-3">
            <Button
              variante="secundaria"
              tamano="grande"
              className="min-w-0 flex-1"
              onClick={onVolver}
            >
              <ChevronLeft size={24} />
              Lista
            </Button>
            {pendientes.length > 0 ? (
              <Button
                tamano="grande"
                className="min-w-0 flex-[2]"
                disabled={pasando}
                onClick={onPasarTodo}
              >
                <CheckCheck size={26} />
                {pasando ? 'Marcando...' : 'Pasar todo'}
              </Button>
            ) : puedeCobrar ? (
              <Button tamano="grande" className="min-w-0 flex-[2]" onClick={onCobrar}>
                <Wallet size={26} />
                Cobrar
              </Button>
            ) : null}
          </div>
        </div>
      }
    >
      {pendientes.length === 0 ? (
        <div className="mb-6 rounded-app-lg border-2 border-hoja-300 bg-hoja-50 px-5 py-8 text-center">
          <p className="text-3xl font-extrabold text-hoja-700">Todo pasó</p>
          <p className="mt-2 text-xl text-tinta">
            {puedeCobrar
              ? 'Listo para cobrar. La cuenta sigue abierta en Caja.'
              : 'Este pedido ya no tiene platos pendientes.'}
          </p>
          {puedeCobrar ? (
            <Button tamano="grande" className="mt-6 w-full max-w-md" onClick={onCobrar}>
              <Wallet size={26} />
              Cobrar ahora
            </Button>
          ) : (
            <Button tamano="grande" className="mt-6 w-full max-w-md" onClick={onVolver}>
              Volver a la lista
            </Button>
          )}
        </div>
      ) : (
        <section className="mb-8">
          <h2 className="mb-3 text-2xl font-extrabold text-carbon">
            Por pasar
            <span className="ml-2 text-brasa-600">({pendientes.length})</span>
          </h2>
          <div className="flex flex-col gap-3">
            {pendientes.map((item) => (
              <FilaPlato
                key={item.id}
                item={item}
                pendiente
                disabled={pasando}
                onClick={() => onPasarItem(item, true)}
              />
            ))}
          </div>
        </section>
      )}

      {pasados.length > 0 ? (
        <section>
          <h2 className="mb-3 text-2xl font-extrabold text-tinta">
            Ya pasaron
            <span className="ml-2">({pasados.length})</span>
          </h2>
          <div className="flex flex-col gap-3">
            {pasados.map((item) => (
              <FilaPlato
                key={item.id}
                item={item}
                pendiente={false}
                disabled={pasando}
                onClick={() => onPasarItem(item, false)}
              />
            ))}
          </div>
        </section>
      ) : null}
    </AppShell>
  )
}

function FilaPlato({ item, pendiente, disabled, onClick }) {
  const detalle = detalleItem(item)

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={cn(
        'flex min-h-24 w-full items-center gap-4 rounded-app-lg border-2 px-4 py-4 text-left',
        'transition-all duration-150 active:scale-[0.99]',
        'disabled:pointer-events-none disabled:opacity-50',
        pendiente
          ? 'border-brasa-300 bg-superficie shadow-media hover:border-brasa-500 hover:bg-brasa-50'
          : 'border-hoja-200 bg-hoja-50/60 text-tinta',
      )}
    >
      <span
        className={cn(
          'flex size-16 shrink-0 items-center justify-center rounded-full text-2xl font-extrabold tabular-nums',
          pendiente ? 'bg-brasa-600 text-white' : 'bg-hoja-600 text-white',
        )}
      >
        {pendiente ? item.cantidad : <Check size={32} strokeWidth={3} />}
      </span>

      <span className="min-w-0 flex-1">
        <span
          className={cn(
            'block text-2xl font-extrabold leading-tight',
            pendiente ? 'text-carbon' : 'text-tinta line-through decoration-2',
          )}
        >
          {item.cantidad > 1 && pendiente ? `${item.cantidad}× ` : null}
          {etiquetaItem(item)}
        </span>
        {detalle ? (
          <span className="mt-1 block text-lg font-semibold text-brasa-700">{detalle}</span>
        ) : null}
        {!pendiente && item.despachadoEn ? (
          <span className="mt-1 block text-base text-tinta">
            Pasó a las {horaCorta(item.despachadoEn)}
          </span>
        ) : null}
      </span>

      <span
        className={cn(
          'shrink-0 rounded-app px-3 py-2 text-base font-bold',
          pendiente ? 'bg-brasa-100 text-brasa-700' : 'bg-superficie text-tinta',
        )}
      >
        {pendiente ? (
          'Pasar'
        ) : (
          <span className="inline-flex items-center gap-1">
            <RotateCcw size={18} />
            Deshacer
          </span>
        )}
      </span>
    </button>
  )
}

/** "hace 3 min" — el parrillero ve de un vistazo quién lleva más rato esperando. */
function minutosEspera(fechaIso) {
  if (!fechaIso) return ''
  const fecha = new Date(fechaIso)
  if (Number.isNaN(fecha.getTime())) return ''
  const minutos = Math.max(0, Math.floor((Date.now() - fecha.getTime()) / 60_000))
  if (minutos < 1) return 'recién'
  if (minutos === 1) return 'hace 1 min'
  return `hace ${minutos} min`
}
