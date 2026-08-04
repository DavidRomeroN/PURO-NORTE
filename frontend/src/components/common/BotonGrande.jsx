import { CardBoton } from '@/components/ui/card'
import { cn } from '@/utils/cn'

/**
 * Botón táctil estándar del sistema: bloque alto, etiqueta a la izquierda y
 * monto a la derecha. Es la pieza que se repite en armadores y menús.
 */
export function BotonGrande({
  etiqueta,
  detalle,
  monto,
  icono: Icono,
  seleccionado = false,
  deshabilitado = false,
  className,
  onClick,
  ...props
}) {
  return (
    <CardBoton
      seleccionada={seleccionado}
      disabled={deshabilitado}
      onClick={onClick}
      className={cn('flex min-h-16 items-center gap-3 px-4 py-3', className)}
      {...props}
    >
      {Icono ? (
        <span
          className={cn(
            'flex size-11 shrink-0 items-center justify-center rounded-app transition-colors',
            seleccionado ? 'bg-brasa-100 text-brasa-700' : 'bg-hundido text-brasa-600',
          )}
        >
          <Icono size={24} />
        </span>
      ) : null}

      <span className="min-w-0 grow">
        <span className="block text-lg font-bold leading-tight text-carbon">{etiqueta}</span>
        {detalle ? <span className="mt-0.5 block text-base text-tinta">{detalle}</span> : null}
      </span>

      {monto != null ? (
        <span className="monto shrink-0 text-xl font-extrabold text-carbon">{monto}</span>
      ) : null}
    </CardBoton>
  )
}
