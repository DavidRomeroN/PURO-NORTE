import { cn } from '@/utils/cn'

/**
 * La superficie base de toda la app. Borde fino más sombra cálida en vez de
 * borde grueso: separa igual de bien pero no ensucia la pantalla cuando hay
 * diez tarjetas juntas.
 */
export function Card({ className, children, ...props }) {
  return (
    <div
      className={cn('rounded-app border border-borde bg-superficie shadow-suave', className)}
      {...props}
    >
      {children}
    </div>
  )
}

/** Tarjeta que se toca. Se hunde al presionar para confirmar el toque sin sonido. */
export function CardBoton({ className, children, seleccionada = false, ...props }) {
  return (
    <button
      type="button"
      className={cn(
        'w-full rounded-app border bg-superficie text-left shadow-suave',
        'transition-all duration-150 active:scale-[0.98] active:shadow-none',
        'disabled:pointer-events-none disabled:opacity-45',
        seleccionada
          ? 'border-2 border-brasa-500 bg-brasa-50 shadow-none'
          : 'border-borde hover:border-borde-fuerte hover:shadow-media',
        className,
      )}
      {...props}
    >
      {children}
    </button>
  )
}
