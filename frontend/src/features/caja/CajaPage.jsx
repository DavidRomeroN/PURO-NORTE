import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ShoppingBag, UtensilsCrossed, Wallet } from 'lucide-react'
import { AppShell } from '@/components/layout/AppShell'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { EstadoVacio } from '@/components/common/EstadoVacio'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { CardBoton } from '@/components/ui/card'
import { pedidosApi } from '@/api/pedidosApi'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { horaCorta } from '@/utils/fechas'
import { esParaLlevar, nombrePedido } from '@/utils/etiquetas'
import { ESTADO_PEDIDO } from '@/utils/constantes'
import { cn } from '@/utils/cn'

export function CajaPage() {
  const navigate = useNavigate()

  // Un solo mozo en el local: refrescar cada 10 segundos alcanza, sin websockets.
  const { data, isPending } = useQuery({
    queryKey: ['pedidos', 'todos'],
    queryFn: () => pedidosApi.listar(),
    refetchInterval: 10_000,
  })

  // Pagado y anulado ya no se cobran. Lo que la parrilla ya pasó va primero: es lo
  // que el cliente está esperando en la mesa con la comida servida.
  const porCobrar = (data ?? [])
    .filter(
      (pedido) =>
        pedido.estado !== ESTADO_PEDIDO.PAGADO && pedido.estado !== ESTADO_PEDIDO.ANULADO,
    )
    .slice()
    .sort((a, b) => {
      const aListo = (a.pendientesDespacho ?? 0) === 0 ? 0 : 1
      const bListo = (b.pendientesDespacho ?? 0) === 0 ? 0 : 1
      if (aListo !== bListo) return aListo - bListo
      return String(a.creadoEn).localeCompare(String(b.creadoEn))
    })

  const totalPendiente = porCobrar.reduce((suma, pedido) => suma + Number(pedido.total ?? 0), 0)

  return (
    <AppShell
      titulo="Caja"
      subtitulo={
        isPending
          ? 'Cuentas por cobrar'
          : `${porCobrar.length} por cobrar · ${formatoMoneda(totalPendiente)}`
      }
      ancho="ancho"
    >
      <Button
        variante="secundaria"
        tamano="grande"
        className="mb-5 w-full sm:w-auto"
        onClick={() => navigate('/llevar/nuevo')}
      >
        <ShoppingBag size={24} />
        Nuevo pedido para llevar
      </Button>

      {isPending ? (
        <EstadoCarga filas={4} />
      ) : porCobrar.length === 0 ? (
        <EstadoVacio
          icono={Wallet}
          titulo="No hay cuentas pendientes"
          descripcion="Cuando el mozo abra una mesa, aparecerá aquí."
          accion="Ver mesas"
          onAccion={() => navigate('/mesas')}
        />
      ) : (
        <ul className="grid gap-3 lg:grid-cols-2">
          {porCobrar.map((pedido) => {
            const cerrada = pedido.estado === ESTADO_PEDIDO.CERRADO
            const yaPaso = (pedido.pendientesDespacho ?? 0) === 0 && (pedido.items?.length ?? 0) > 0
            return (
              <li key={pedido.id}>
                <CardBoton
                  onClick={() => navigate(`/cobrar/${pedido.id}`)}
                  className={cn(
                    'flex min-h-20 items-center gap-3 p-4',
                    yaPaso && 'border-hoja-300 bg-hoja-50',
                  )}
                >
                  <span
                    className={cn(
                      'flex size-11 shrink-0 items-center justify-center rounded-app',
                      yaPaso ? 'bg-hoja-100 text-hoja-700' : 'bg-hundido text-brasa-600',
                    )}
                  >
                    {esParaLlevar(pedido) ? <ShoppingBag size={24} /> : <UtensilsCrossed size={24} />}
                  </span>
                  <span className="min-w-0 grow">
                    <span className="flex flex-wrap items-center gap-2">
                      <span className="text-xl font-bold text-carbon">{nombrePedido(pedido)}</span>
                      {yaPaso ? <Badge tono="hoja">Ya pasó · cobrar</Badge> : null}
                      {cerrada ? <Badge tono="brasa">Cuenta cerrada</Badge> : null}
                    </span>
                    <span className="block text-sm text-tinta">
                      {pedido.items.length} {pedido.items.length === 1 ? 'ítem' : 'ítems'} ·{' '}
                      {horaCorta(pedido.creadoEn)}
                      {!yaPaso && (pedido.pendientesDespacho ?? 0) > 0
                        ? ` · ${pedido.pendientesDespacho} en parrilla`
                        : null}
                    </span>
                  </span>
                  <span className="monto shrink-0 text-2xl font-extrabold text-carbon">
                    {formatoMoneda(pedido.total)}
                  </span>
                </CardBoton>
              </li>
            )
          })}
        </ul>
      )}
    </AppShell>
  )
}
