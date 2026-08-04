/** Formatea un monto como "S/ 18.00". Tolera strings, porque el backend serializa BigDecimal. */
export function formatoMoneda(valor) {
  const numero = Number(valor ?? 0)
  const seguro = Number.isFinite(numero) ? numero : 0
  return `S/ ${seguro.toFixed(2)}`
}

/** Solo el número, para cuando el "S/" ya está en la etiqueta de al lado. */
export function formatoNumero(valor) {
  const numero = Number(valor ?? 0)
  return (Number.isFinite(numero) ? numero : 0).toFixed(2)
}
