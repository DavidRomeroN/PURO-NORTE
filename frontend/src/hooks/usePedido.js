import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { pedidosApi } from '@/api/pedidosApi'

/**
 * El id llega como texto desde la URL. Se normaliza a número porque las mutaciones
 * escriben en la caché con el id numérico del backend, y si las claves no coinciden
 * la pantalla se queda con datos viejos hasta recargar.
 */
export function usePedido(pedidoId) {
  const id = Number(pedidoId)
  const valido = Number.isInteger(id) && id > 0

  return useQuery({
    queryKey: ['pedido', id],
    queryFn: () => pedidosApi.obtener(id),
    enabled: valido,
  })
}

/**
 * Pedidos de la parrilla. Se refresca sola cada pocos segundos: el mozo agrega platos
 * y el parrillero tiene que verlos sin tocar nada.
 */
export function useParrilla() {
  return useQuery({
    queryKey: ['pedidos', 'parrilla'],
    queryFn: pedidosApi.listarParrilla,
    refetchInterval: 5_000,
  })
}

/** El pedido abierto de una mesa, o null si la mesa está libre. */
export function usePedidoDeMesa(mesaId) {
  const consulta = useQuery({
    queryKey: ['pedidos', 'activos'],
    queryFn: pedidosApi.listarActivos,
    enabled: mesaId != null,
  })

  const numero = Number(mesaId)
  // También encuentra la cuenta entrando por una mesa unida: si el grupo juntó la 12
  // con la 13, tocar la 13 tiene que abrir la misma cuenta y no una vacía.
  const ocupa = (pedido) =>
    pedido.mesaId === numero || (pedido.mesasUnidas ?? []).some((mesa) => mesa.id === numero)

  return {
    pedido: (consulta.data ?? []).find(ocupa) ?? null,
    cargando: mesaId != null && consulta.isPending,
    error: consulta.error,
  }
}

export function useAccionesPedido() {
  const queryClient = useQueryClient()

  const refrescar = (pedido) => {
    queryClient.invalidateQueries({ queryKey: ['pedidos'] })
    queryClient.invalidateQueries({ queryKey: ['mesas'] })
    if (pedido?.id) {
      queryClient.setQueryData(['pedido', pedido.id], pedido)
    }
  }

  const crearPedido = useMutation({
    mutationFn: (mesaId) => pedidosApi.crear(mesaId),
    onSuccess: refrescar,
  })

  const agregarItem = useMutation({
    mutationFn: ({ pedidoId, item }) => pedidosApi.agregarItem(pedidoId, item),
    onSuccess: refrescar,
  })

  const quitarItem = useMutation({
    mutationFn: ({ pedidoId, itemId }) => pedidosApi.quitarItem(pedidoId, itemId),
    onSuccess: refrescar,
  })

  const editarPrecio = useMutation({
    mutationFn: ({ pedidoId, itemId, precioFinal, motivo }) =>
      pedidosApi.editarPrecio(pedidoId, itemId, precioFinal, motivo),
    onSuccess: refrescar,
  })

  const cerrarPedido = useMutation({
    mutationFn: (pedidoId) => pedidosApi.cerrar(pedidoId),
    onSuccess: refrescar,
  })

  const anularPedido = useMutation({
    mutationFn: ({ pedidoId, motivo }) => pedidosApi.anular(pedidoId, motivo),
    onSuccess: refrescar,
  })

  const marcarDespacho = useMutation({
    mutationFn: ({ pedidoId, itemId, despachado }) =>
      pedidosApi.marcarDespachoItem(pedidoId, itemId, despachado),
    onSuccess: (pedido) => {
      refrescar(pedido)
      queryClient.invalidateQueries({ queryKey: ['pedidos', 'parrilla'] })
    },
  })

  const despacharTodo = useMutation({
    mutationFn: (pedidoId) => pedidosApi.despacharTodo(pedidoId),
    onSuccess: (pedido) => {
      refrescar(pedido)
      queryClient.invalidateQueries({ queryKey: ['pedidos', 'parrilla'] })
    },
  })

  const moverAMesa = useMutation({
    mutationFn: ({ pedidoId, mesaId }) => pedidosApi.moverAMesa(pedidoId, mesaId),
    onSuccess: refrescar,
  })

  const unirMesa = useMutation({
    mutationFn: ({ pedidoId, mesaId }) => pedidosApi.unirMesa(pedidoId, mesaId),
    onSuccess: refrescar,
  })

  const separarMesa = useMutation({
    mutationFn: ({ pedidoId, mesaId }) => pedidosApi.separarMesa(pedidoId, mesaId),
    onSuccess: refrescar,
  })

  return {
    crearPedido,
    agregarItem,
    quitarItem,
    editarPrecio,
    cerrarPedido,
    anularPedido,
    marcarDespacho,
    despacharTodo,
    moverAMesa,
    unirMesa,
    separarMesa,
  }
}
