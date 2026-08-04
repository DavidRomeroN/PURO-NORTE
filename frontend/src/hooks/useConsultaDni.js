import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { clientesApi, ESTADO_CONSULTA } from '@/api/clientesApi'

/** Suficiente para que corregir un dígito no gaste una consulta de más. */
const ESPERA_MS = 400

/**
 * Resuelve el nombre de un DNI mientras el cajero lo escribe.
 *
 * Cada consulta cuesta un crédito del proveedor, así que espera a que el número esté
 * completo y quieto antes de salir a buscarlo, y no reintenta si falla: el nombre es una
 * comprobación visual, no un requisito para cobrar.
 */
export function useConsultaDni(dni) {
  const completo = /^\d{8}$/.test(dni)
  const [numeroEstable, setNumeroEstable] = useState(completo ? dni : '')

  useEffect(() => {
    if (!completo) {
      setNumeroEstable('')
      return undefined
    }
    const temporizador = setTimeout(() => setNumeroEstable(dni), ESPERA_MS)
    return () => clearTimeout(temporizador)
  }, [dni, completo])

  const consulta = useQuery({
    queryKey: ['cliente', numeroEstable],
    queryFn: () => clientesApi.consultarDni(numeroEstable),
    enabled: Boolean(numeroEstable),
    retry: false,
    // El nombre de una persona no cambia: una vez resuelto, no se vuelve a preguntar.
    staleTime: Infinity,
    gcTime: Infinity,
  })

  // Escribir el octavo dígito y esperar la respuesta son el mismo momento para el cajero.
  const consultando = completo && (numeroEstable !== dni || consulta.isFetching)

  if (consultando) {
    return { consultando: true, resultado: null }
  }
  if (!completo) {
    return { consultando: false, resultado: null }
  }
  // Si la petición ni siquiera llegó al backend, es igual de "no verificado" que si el
  // proveedor falló: en los dos casos se puede emitir, solo que sin confirmar el nombre.
  if (consulta.error) {
    return {
      consultando: false,
      resultado: { estado: ESTADO_CONSULTA.NO_VERIFICADO },
    }
  }
  return { consultando: false, resultado: consulta.data ?? null }
}
