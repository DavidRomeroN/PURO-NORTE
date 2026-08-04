import axiosClient from './axiosClient'

export const usuariosApi = {
  async listar({ incluirInactivos = true } = {}) {
    const { data } = await axiosClient.get('/usuarios', {
      params: { incluirInactivos: incluirInactivos || undefined },
    })
    return data
  },

  async crear(usuario) {
    const { data } = await axiosClient.post('/usuarios', usuario)
    return data
  },

  async actualizar(id, { nombre, rol }) {
    const { data } = await axiosClient.put(`/usuarios/${id}`, { nombre, rol })
    return data
  },

  async cambiarPassword(id, password) {
    await axiosClient.patch(`/usuarios/${id}/password`, { password })
  },

  async cambiarEstado(id, activo) {
    const { data } = await axiosClient.patch(`/usuarios/${id}/estado`, { activo })
    return data
  },
}
