export const ROLES = {
  MOZO: 'MOZO',
  CAJA: 'CAJA',
  ADMIN: 'ADMIN',
}

export const ESTADO_MESA = {
  LIBRE: 'LIBRE',
  OCUPADA: 'OCUPADA',
}

export const ESTADO_PEDIDO = {
  ABIERTO: 'ABIERTO',
  CERRADO: 'CERRADO',
  PAGADO: 'PAGADO',
  ANULADO: 'ANULADO',
}

/** Si el plato ya salió de la parrilla. */
export const ESTADO_DESPACHO = {
  PENDIENTE: 'PENDIENTE',
  DESPACHADO: 'DESPACHADO',
}

export const TIPO_ITEM = {
  ANTICUCHO: 'ANTICUCHO',
  COMBO: 'COMBO',
  BEBIDA: 'BEBIDA',
  EXTRA: 'EXTRA',
}

export const TIPO_PRODUCTO = {
  ANTICUCHO: 'ANTICUCHO',
  BEBIDA: 'BEBIDA',
  EXTRA: 'EXTRA',
}

/** Los roles nunca se muestran en crudo: la dueña no sabe qué es "CAJA". */
export const INFO_ROLES = {
  MOZO: { etiqueta: 'Mozo', descripcion: 'Toma pedidos en las mesas' },
  CAJA: { etiqueta: 'Cajero', descripcion: 'Cobra y emite boletas' },
  ADMIN: { etiqueta: 'Administrador', descripcion: 'Acceso total, ve las ganancias' },
}

export const INFO_TIPOS_PRODUCTO = {
  ANTICUCHO: { etiqueta: 'Anticucho', plural: 'Anticuchos', ejemplo: 'carne, pollo, corazón...' },
  BEBIDA: { etiqueta: 'Bebida', plural: 'Bebidas', ejemplo: 'gaseosa, mate...' },
  EXTRA: { etiqueta: 'Extra', plural: 'Extras', ejemplo: 'taper, papa...' },
}

export const TIPO_BOLETA = {
  CONSUMO: 'CONSUMO',
  DETALLADO: 'DETALLADO',
}

export const ESTADO_SUNAT = {
  PENDIENTE: 'PENDIENTE',
  ACEPTADO: 'ACEPTADO',
  OBSERVADO: 'OBSERVADO',
  ERROR: 'ERROR',
}

export const MEDIOS_PAGO = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'TARJETA', etiqueta: 'Tarjeta' },
  { valor: 'YAPE', etiqueta: 'Yape' },
  { valor: 'PLIN', etiqueta: 'Plin' },
]

/** Envases que se ofrecen cuando el pedido es para llevar. */
export const NOMBRES_ENVASE = ['Taper', 'Bandeja']

export const CLAVE_TOKEN = 'puronorte.token'
export const CLAVE_USUARIO = 'puronorte.usuario'
