import { Flame, LayoutGrid, Settings, Wallet } from 'lucide-react'

/** Una sola definición de las secciones, para que el nav de celular y el de escritorio no se separen. */
export function seccionesVisibles({ puedeCobrar, esAdmin }) {
  return [
    { a: '/mesas', etiqueta: 'Mesas', Icono: LayoutGrid, visible: true },
    { a: '/parrilla', etiqueta: 'Parrilla', Icono: Flame, visible: true },
    { a: '/caja', etiqueta: 'Caja', Icono: Wallet, visible: puedeCobrar },
    { a: '/admin', etiqueta: 'Admin', Icono: Settings, visible: esAdmin },
  ].filter((seccion) => seccion.visible)
}
