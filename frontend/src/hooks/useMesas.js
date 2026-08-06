import { useQuery } from '@tanstack/react-query'
import { catalogoApi } from '@/api/catalogoApi'
import { pedidosApi } from '@/api/pedidosApi'

/**
 * Mesas con el total acumulado de su pedido abierto, para responder la pregunta
 * más frecuente de la noche ("¿cuánto es la mesa 7?") sin entrar a la mesa.
 */
export function useMesas() {
  const mesas = useQuery({
    queryKey: ['mesas'],
    queryFn: catalogoApi.listarMesas,
    staleTime: 60 * 1000,
  })

  const activos = useQuery({
    queryKey: ['pedidos', 'activos'],
    queryFn: pedidosApi.listarActivos,
    refetchInterval: 60_000,
  })

  const pedidos = activos.data ?? []

  // Una cuenta puede ocupar varias mesas cuando el grupo las juntó, así que todas
  // apuntan al mismo pedido. `unidaA` distingue la principal, que es donde se lleva
  // la cuenta, de las que se le sumaron.
  const porMesa = new Map()
  for (const pedido of pedidos) {
    if (pedido.mesaId == null) continue
    porMesa.set(pedido.mesaId, { pedido, unidaA: null })
    for (const unida of pedido.mesasUnidas ?? []) {
      porMesa.set(unida.id, { pedido, unidaA: pedido.mesaNumero })
    }
  }

  const conPedido = (mesas.data ?? []).map((mesa) => {
    const ocupacion = porMesa.get(mesa.id)
    return {
      ...mesa,
      pedido: ocupacion?.pedido ?? null,
      unidaA: ocupacion?.unidaA ?? null,
    }
  })

  return {
    cargando: mesas.isPending,
    error: mesas.error ?? activos.error,
    mesas: conPedido,
    // Libre = sin cuenta viva. No depender del flag `estado` (puede quedar desfasado).
    libres: conPedido.filter((mesa) => !mesa.pedido),
    // Los pedidos para llevar no ocupan mesa, así que se listan aparte.
    paraLlevar: pedidos.filter((pedido) => pedido.mesaId == null),
  }
}
