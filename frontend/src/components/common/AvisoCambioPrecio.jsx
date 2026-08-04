import { formatoMoneda } from '@/utils/formatoMoneda'

/**
 * La segunda línea es la que importa: al cambiar un precio la duda natural de la dueña
 * es si se le va a mover el historial de ventas.
 */
export function AvisoCambioPrecio({ nombre, anterior, nuevo }) {
  return (
    <p className="rounded-app border-l-4 border-brasa-500 bg-brasa-50 p-4 text-lg text-carbon">
      El precio de <strong>{nombre}</strong> pasará de{' '}
      <span className="monto font-bold">{formatoMoneda(anterior)}</span> a{' '}
      <span className="monto font-bold">{formatoMoneda(nuevo)}</span>.
      <span className="mt-2 block text-base text-tinta">
        Los pedidos y boletas de días anteriores no cambian.
      </span>
    </p>
  )
}
