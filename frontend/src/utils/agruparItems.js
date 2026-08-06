/**
 * Misma regla que el AgrupadorItems del backend:
 * tipo + componentes (IDs ordenados) + sustituciones + comentario + para llevar.
 */
import { descripcionCocina } from './descripcionCocina'

function normalizarObs(obs) {
  if (obs == null) return null
  const t = String(obs).trim()
  return t === '' ? null : t
}

function slug(texto) {
  return texto.toLowerCase().replace(/\s+/g, '-')
}

export function claveAgrupacion(item) {
  const comps = [...(item.componentes ?? [])].sort((a, b) => {
    const sa = a.comboSlotId ?? 0
    const sb = b.comboSlotId ?? 0
    if (sa !== sb) return sa - sb
    return (a.productoBaseId ?? 0) - (b.productoBaseId ?? 0)
  })
  const componentes = comps
    .map((c) => {
      const prod = String(c.productoBaseId ?? '')
      if (c.esSustitucion && c.comboSlotId != null) return `slot${c.comboSlotId}=${prod}`
      return prod
    })
    .join('-')

  const obs = normalizarObs(item.observaciones)
  const obsClave = obs == null ? 'null' : slug(obs)
  const llevar = Boolean(item.paraLlevar)
  const tipo = item.tipoItem

  if (tipo === 'COMBO') {
    return `COMBO|${item.comboId ?? ''}|${componentes}|${obsClave}|${llevar}`
  }
  return `${tipo}|${componentes}|${obsClave}|${llevar}`
}

export function descripcionItem(item) {
  return descripcionCocina(item)
}

/** Agrupa ítems del servidor (parrilla) o líneas de canasta con misma forma. */
export function agruparItems(items) {
  if (!items?.length) return []
  const mapa = new Map()
  for (const item of items) {
    const clave = claveAgrupacion(item)
    if (!mapa.has(clave)) {
      mapa.set(clave, {
        clave,
        descripcion: descripcionItem(item),
        cantidad: 0,
        observacion: normalizarObs(item.observaciones),
        paraLlevar: Boolean(item.paraLlevar),
        items: [],
        tipoItem: item.tipoItem,
      })
    }
    const linea = mapa.get(clave)
    linea.cantidad += item.cantidad ?? 1
    linea.items.push(item)
  }
  const lineas = [...mapa.values()]
  const sin = lineas.filter((l) => !l.observacion)
  const con = lineas.filter((l) => l.observacion)
  return [...sin, ...con]
}

/** Clave de canasta local (antes de enviar). */
export function claveCanasta(linea) {
  return claveAgrupacion({
    tipoItem: linea.tipoItem,
    comboId: linea.comboId,
    componentes: (linea.componentes ?? []).map((id, i) =>
      typeof id === 'object'
        ? id
        : { productoBaseId: id, productoNombre: '', comboSlotId: linea.sustituciones?.[i]?.comboSlotId },
    ),
    observaciones: linea.observaciones,
    paraLlevar: linea.paraLlevar,
    comboNombre: linea.comboNombre,
  })
}
