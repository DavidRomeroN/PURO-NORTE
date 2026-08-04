import axiosClient from './axiosClient'

/** Coincide con EstadoConsulta del backend. */
export const ESTADO_CONSULTA = {
  ENCONTRADO: 'ENCONTRADO',
  NO_ENCONTRADO: 'NO_ENCONTRADO',
  NO_VERIFICADO: 'NO_VERIFICADO',
}

const TIPO_DNI = '1'

export const clientesApi = {
  /** El nombre es solo para que el cajero confirme el número; no viaja en la boleta. */
  async consultarDni(numero) {
    const { data } = await axiosClient.get('/clientes/consultar', {
      params: { tipo: TIPO_DNI, numero },
    })
    return data
  },
}
