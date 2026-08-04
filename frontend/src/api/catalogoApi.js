import axiosClient from './axiosClient'

export const catalogoApi = {
  async listarProductos({ tipo, incluirInactivos = false } = {}) {
    const { data } = await axiosClient.get('/productos', {
      params: { tipo, incluirInactivos: incluirInactivos || undefined },
    })
    return data
  },

  async crearProducto(producto) {
    const { data } = await axiosClient.post('/productos', producto)
    return data
  },

  async actualizarProducto(id, producto) {
    const { data } = await axiosClient.put(`/productos/${id}`, producto)
    return data
  },

  async cambiarEstadoProducto(id, activo) {
    const { data } = await axiosClient.patch(`/productos/${id}/estado`, { activo })
    return data
  },

  async listarCombos({ incluirInactivos = false } = {}) {
    const { data } = await axiosClient.get('/combos', {
      params: { incluirInactivos: incluirInactivos || undefined },
    })
    return data
  },

  async actualizarCombo(id, combo) {
    const { data } = await axiosClient.put(`/combos/${id}`, combo)
    return data
  },

  async cambiarEstadoCombo(id, activo) {
    const { data } = await axiosClient.patch(`/combos/${id}/estado`, { activo })
    return data
  },

  async actualizarSlot(comboId, slotId, slot) {
    const { data } = await axiosClient.put(`/combos/${comboId}/slots/${slotId}`, slot)
    return data
  },

  async listarMesas() {
    const { data } = await axiosClient.get('/mesas')
    return data
  },

  async crearMesa(numero) {
    const { data } = await axiosClient.post('/mesas', { numero })
    return data
  },

  async actualizarMesa(id, numero) {
    const { data } = await axiosClient.put(`/mesas/${id}`, { numero })
    return data
  },

  async eliminarMesa(id) {
    await axiosClient.delete(`/mesas/${id}`)
  },
}
