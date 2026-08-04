import { cn } from '@/utils/cn'

const TONOS = {
  neutro: 'bg-hundido text-tinta',
  brasa: 'bg-brasa-100 text-brasa-700',
  hoja: 'bg-hoja-100 text-hoja-700',
  alerta: 'bg-alerta-suave text-alerta',
}

/** Etiqueta corta de estado. Siempre lleva texto: el color solo no informa. */
export function Badge({ tono = 'neutro', className, children }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-bold uppercase tracking-wide',
        TONOS[tono],
        className,
      )}
    >
      {children}
    </span>
  )
}
