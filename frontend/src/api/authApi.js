import axiosClient from './axiosClient'

export const authApi = {
  async login(usuario, password) {
    const { data } = await axiosClient.post('/auth/login', { usuario, password })
    return data
  },

  async yo() {
    const { data } = await axiosClient.get('/auth/me')
    return data
  },
}
