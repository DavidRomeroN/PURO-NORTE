import { TIPO_ITEM } from './constantes'

/** Nombre corto del ítem, con el vocabulario del negocio. */
export function etiquetaItem(item) {
  if (item.tipoItem === TIPO_ITEM.COMBO) {
    return item.comboNombre ?? 'Combo'
  }

  const nombres = (item.componentes ?? []).map((componente) => componente.productoNombre)
  if (nombres.length === 0) return 'Ítem'

  const tamano = tamanoAnticucho(item)
  return tamano ? `${tamano} · ${nombres.join(' + ')}` : nombres.join(' + ')
}

/** Cuántos palitos lleva el anticucho, dicho como lo dice el mozo. */
function tamanoAnticucho(item) {
  if (item.tipoItem !== TIPO_ITEM.ANTICUCHO) return null

  const palos = (item.componentes ?? []).length
  if (palos === 2) return 'Doble'
  if (palos === 3) return 'Triple'
  if (palos > 3) return `${palos} palos`
  return null
}

/** Un pedido para llevar no tiene mesa: lo identifica su número del día. */
export function esParaLlevar(pedido) {
  return Boolean(pedido) && pedido.mesaId == null
}

/** "Mesa 11", "Mesas 12 + 13" o "Para llevar 2", para titular cualquier pantalla. */
export function nombrePedido(pedido) {
  if (!pedido) return ''
  if (esParaLlevar(pedido)) return `Para llevar ${pedido.numeroLlevar ?? ''}`.trim()

  const numeros = numerosDeMesa(pedido)
  return numeros.length > 1 ? `Mesas ${numeros.join(' + ')}` : `Mesa ${pedido.mesaNumero}`
}

/** Todas las mesas que ocupa la cuenta, empezando por la principal. */
export function numerosDeMesa(pedido) {
  if (!pedido || pedido.mesaNumero == null) return []
  return [pedido.mesaNumero, ...(pedido.mesasUnidas ?? []).map((mesa) => mesa.numero)]
}

/** Segunda línea del ítem: palitos del mixto, para llevar y observaciones. */
export function detalleItem(item) {
  const partes = []

  if (item.tipoItem === TIPO_ITEM.COMBO) {
    const palitos = (item.componentes ?? [])
      .map((componente) => componente.productoNombre)
      .filter(Boolean)
    if (palitos.length > 0) {
      partes.push(palitos.join(' + '))
    }
  }

  if (item.paraLlevar) partes.push('para llevar')
  if (item.observaciones) partes.push(item.observaciones)

  return partes.join(' · ')
}

/** "1 anticucho", "2 (doble)", "3 (triple)". */
export function contadorAnticucho(cantidad) {
  if (cantidad === 0) return 'Elige los palitos'
  if (cantidad === 1) return '1 anticucho'
  if (cantidad === 2) return '2 (doble)'
  if (cantidad === 3) return '3 (triple)'
  return `${cantidad} unidades`
}
