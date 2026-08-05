import axiosClient from './axiosClient'

export const pedidosApi = {
  async crear(mesaId) {
    const { data } = await axiosClient.post('/pedidos', { mesaId })
    return data
  },

  async listar(estado) {
    const { data } = await axiosClient.get('/pedidos', { params: { estado } })
    return data
  },

  async listarActivos() {
    const { data } = await axiosClient.get('/pedidos/activos')
    return data
  },

  /** Pedidos vivos para la tablet del parrillero, del más viejo al más nuevo. */
  async listarParrilla() {
    const { data } = await axiosClient.get('/pedidos/parrilla')
    return data
  },

  async marcarDespachoItem(pedidoId, itemId, despachado) {
    const { data } = await axiosClient.patch(`/pedidos/${pedidoId}/items/${itemId}/despacho`, {
      despachado,
    })
    return data
  },

  async despacharTodo(pedidoId) {
    const { data } = await axiosClient.patch(`/pedidos/${pedidoId}/despachar`)
    return data
  },

  async obtener(id) {
    const { data } = await axiosClient.get(`/pedidos/${id}`)
    return data
  },

  async agregarItem(pedidoId, item) {
    const { data } = await axiosClient.post(`/pedidos/${pedidoId}/items`, item)
    return data
  },

  async agregarItemsLote(pedidoId, items) {
    const { data } = await axiosClient.post(`/pedidos/${pedidoId}/items/lote`, { items })
    return data
  },

  async quitarItem(pedidoId, itemId) {
    const { data } = await axiosClient.delete(`/pedidos/${pedidoId}/items/${itemId}`)
    return data
  },

  async editarPrecio(pedidoId, itemId, precioFinal, motivo) {
    const { data } = await axiosClient.patch(`/pedidos/${pedidoId}/items/${itemId}/precio`, {
      precioFinal,
      motivo: motivo?.trim() ? motivo.trim() : null,
    })
    return data
  },

  async cerrar(pedidoId) {
    const { data } = await axiosClient.patch(`/pedidos/${pedidoId}/cerrar`)
    return data
  },

  /** Descarta la cuenta sin cobrarla y libera sus mesas. No borra nada: queda ANULADA. */
  async anular(pedidoId, motivo) {
    const { data } = await axiosClient.patch(`/pedidos/${pedidoId}/anular`, { motivo })
    return data
  },

  /** El grupo se cambió de mesa: las anteriores quedan libres. */
  async moverAMesa(pedidoId, mesaId) {
    const { data } = await axiosClient.patch(`/pedidos/${pedidoId}/mesa`, { mesaId })
    return data
  },

  /** El grupo juntó otra mesa: la cuenta pasa a ocupar las dos. */
  async unirMesa(pedidoId, mesaId) {
    const { data } = await axiosClient.post(`/pedidos/${pedidoId}/mesas`, { mesaId })
    return data
  },

  async separarMesa(pedidoId, mesaId) {
    const { data } = await axiosClient.delete(`/pedidos/${pedidoId}/mesas/${mesaId}`)
    return data
  },
}
