import axiosClient from './axiosClient'

/** Desde este monto SUNAT exige el DNI del comprador. */
export const MONTO_QUE_EXIGE_DNI = 700

async function desenvolverErrorBlob(error) {
  const cuerpo = error.response?.data
  if (!(cuerpo instanceof Blob)) return
  try {
    error.response.data = JSON.parse(await cuerpo.text())
  } catch {
    error.response.data = null
  }
}

export const boletasApi = {
  async generar({ pedidoId, tipo, medioPago, dniCliente }) {
    const { data } = await axiosClient.post('/boletas', {
      pedidoId,
      tipo,
      formaPago: 'CONTADO',
      medioPago,
      dniCliente: dniCliente || null,
    })
    return data
  },

  async listar({ estadoSunat, desde, hasta } = {}) {
    const { data } = await axiosClient.get('/boletas', {
      params: { estadoSunat, desde, hasta },
    })
    return data
  },

  async obtener(id) {
    const { data } = await axiosClient.get(`/boletas/${id}`)
    return data
  },

  async reintentar(id) {
    const { data } = await axiosClient.post(`/boletas/${id}/reintentar`)
    return data
  },

  /** Pregunta a SUNAT en vivo en qué quedó la boleta. No cuesta créditos. */
  async sincronizar(id) {
    const { data } = await axiosClient.post(`/boletas/${id}/sincronizar`)
    return data
  },

  /**
   * El PDF llega por el backend porque la descarga necesita el token, así que no se
   * puede abrir con un enlace suelto.
   */
  async abrirPdf(id) {
    let data
    try {
      ;({ data } = await axiosClient.get(`/boletas/${id}/pdf`, { responseType: 'blob' }))
    } catch (error) {
      // Al pedir un blob, el cuerpo del error también llega como blob y el mensaje
      // del backend se perdería.
      await desenvolverErrorBlob(error)
      throw error
    }

    const url = URL.createObjectURL(data)
    window.open(url, '_blank', 'noopener')
    // Se libera después de que el navegador alcanzó a abrirlo.
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  },

  async creditos() {
    const { data } = await axiosClient.get('/boletas/creditos')
    return data
  },

  /** Se usa después de corregir la configuración del RUC, sin reiniciar el servidor. */
  async reactivarEmision() {
    const { data } = await axiosClient.post('/boletas/reactivar-emision')
    return data
  },

  async enviarCorreo(id, correo) {
    await axiosClient.post(`/boletas/${id}/enviar-correo`, { correo })
  },

  async marcarWhatsapp(id) {
    const { data } = await axiosClient.post(`/boletas/${id}/marcar-whatsapp`)
    return data
  },
}
