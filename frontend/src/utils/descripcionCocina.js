/**
 * Textos de cocina/parrilla: como los dice el parrillero, no como sumas de precios.
 */

function lower(nombre) {
  return String(nombre ?? '').toLowerCase()
}

function listaConY(nombres) {
  const limpios = nombres.map(lower).filter(Boolean)
  if (limpios.length === 0) return ''
  if (limpios.length === 1) return limpios[0]
  if (limpios.length === 2) return `${limpios[0]} y ${limpios[1]}`
  return `${limpios.slice(0, -1).join(', ')} y ${limpios[limpios.length - 1]}`
}

function componentesOrdenadosCombo(item) {
  return [...(item.componentes ?? [])].sort((a, b) => {
    const sa = a.comboSlotId ?? 0
    const sb = b.comboSlotId ?? 0
    if (sa !== sb) return sa - sb
    return (a.productoBaseId ?? 0) - (b.productoBaseId ?? 0)
  })
}

/** Anticucho: "doble de carne y corazón", "triple de a, b y c". */
function descripcionAnticucho(item) {
  // Orden de armado (como los eligió el mozo), no por id.
  const nombres = (item.componentes ?? []).map((c) => c.productoNombre).filter(Boolean)
  if (!nombres.length) return 'Anticucho'
  if (nombres.length === 1) return `Anticucho de ${lower(nombres[0])}`
  if (nombres.length === 2) return `doble de ${listaConY(nombres)}`
  if (nombres.length === 3) return `triple de ${listaConY(nombres)}`
  return `${nombres.length} palos de ${listaConY(nombres)}`
}

/**
 * Mixto: 1 cambio → "mixto pollo por carne" (lo que quitaron por lo que pusieron);
 * 2+ cambios → listar los palitos; sin cambios → solo el nombre.
 */
function descripcionCombo(item) {
  const nombre = item.comboNombre || 'Mixto'
  const comps = componentesOrdenadosCombo(item)
  const cambios = comps.filter((c) => c.esSustitucion)

  if (cambios.length === 1) {
    const cambio = cambios[0]
    const nuevo = lower(cambio.productoNombre)
    const original = lower(cambio.productoOriginalNombre)
    if (nuevo && original) {
      const base = /especial/i.test(nombre) ? 'mixto especial' : 'mixto'
      return `${base} ${original} por ${nuevo}`
    }
  }

  if (cambios.length >= 2) {
    const palitos = comps.map((c) => c.productoNombre).filter(Boolean)
    return palitos.length ? `${nombre} · ${palitos.join(' + ')}` : nombre
  }

  return nombre
}

export function descripcionCocina(item) {
  if (item?.tipoItem === 'COMBO') return descripcionCombo(item)
  if (item?.tipoItem === 'ANTICUCHO') return descripcionAnticucho(item)
  const nombres = (item.componentes ?? []).map((c) => c.productoNombre).filter(Boolean)
  return nombres.join(' + ') || item?.tipoItem || 'Ítem'
}
