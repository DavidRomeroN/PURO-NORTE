/** "20:15" a partir de un LocalDateTime del backend. */
export function horaCorta(fechaIso) {
  if (!fechaIso) return ''
  const fecha = new Date(fechaIso)
  if (Number.isNaN(fecha.getTime())) return ''
  return fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
}

/** Fecha de hoy en formato YYYY-MM-DD, que es lo que esperan los filtros del backend. */
export function fechaDeHoy() {
  const ahora = new Date()
  const mes = String(ahora.getMonth() + 1).padStart(2, '0')
  const dia = String(ahora.getDate()).padStart(2, '0')
  return `${ahora.getFullYear()}-${mes}-${dia}`
}

export function esDeHoy(fechaIso) {
  return esDelDia(fechaIso, fechaDeHoy())
}

/** El backend serializa LocalDateTime como "2026-08-04T20:15:00", así que basta el prefijo. */
export function esDelDia(fechaIso, dia) {
  return typeof fechaIso === 'string' && fechaIso.startsWith(dia)
}

/** "hoy", "ayer" o "martes 4 de agosto", para titular la pantalla sin jerga. */
export function fechaLegible(dia) {
  const hoy = fechaDeHoy()
  if (dia === hoy) return 'hoy'

  const [ano, mes, numero] = dia.split('-').map(Number)
  const fecha = new Date(ano, mes - 1, numero)

  const ayer = new Date()
  ayer.setDate(ayer.getDate() - 1)
  ayer.setHours(0, 0, 0, 0)
  if (fecha.getTime() === ayer.getTime()) return 'ayer'

  return fecha.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' })
}
