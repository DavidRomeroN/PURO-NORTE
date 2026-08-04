/**
 * Preview del precio mientras se arma el ítem, para no ir al servidor en cada toque.
 *
 * REGLA ABSOLUTA: esto es solo visual. El precio que se guarda es siempre el que
 * devuelve el backend al crear el ítem; nunca se envía un precio calculado aquí.
 */

function aNumero(valor) {
  const numero = Number(valor ?? 0)
  return Number.isFinite(numero) ? numero : 0
}

function redondear(monto) {
  return Math.round((monto + Number.EPSILON) * 100) / 100
}

/** Anticucho: suma de los componentes elegidos. Admite repetidos. */
export function calcularPrecioAnticucho(componentes) {
  const total = (componentes ?? []).reduce(
    (suma, producto) => suma + aNumero(producto?.precioUnitario),
    0,
  )
  return redondear(total)
}

/** Combo: precio base + Σ max(0, precioNuevo − precioOriginalDelSlot). */
export function calcularPrecioCombo(combo, sustitucionesPorSlot) {
  if (!combo) return 0

  const total = (combo.slots ?? []).reduce((suma, slot) => {
    const reemplazo = sustitucionesPorSlot?.[slot.id]
    if (!reemplazo) return suma
    return suma + diferenciaDeSustitucion(slot, reemplazo)
  }, aNumero(combo.precioBase))

  return redondear(total)
}

/** Cuánto suma cambiar un slot. Nunca es negativo: un cambio más barato no descuenta. */
export function diferenciaDeSustitucion(slot, productoNuevo) {
  const original = aNumero(slot?.productoBaseDefault?.precioUnitario)
  const nuevo = aNumero(productoNuevo?.precioUnitario)
  return redondear(Math.max(0, nuevo - original))
}

export function useCalculoPrecio() {
  return { calcularPrecioAnticucho, calcularPrecioCombo, diferenciaDeSustitucion }
}
