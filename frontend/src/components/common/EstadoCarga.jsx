import { Skeleton } from '@/components/ui/skeleton'

/** Skeletons en lugar de spinners: la pantalla mantiene su forma mientras carga. */
export function EstadoCarga({ filas = 4, alto = 'h-20' }) {
  return (
    <div className="flex flex-col gap-3" aria-busy="true" aria-live="polite">
      <span className="sr-only">Cargando</span>
      {Array.from({ length: filas }).map((_, indice) => (
        <Skeleton key={indice} className={alto} />
      ))}
    </div>
  )
}

export function EstadoCargaGrid({ celdas = 6 }) {
  return (
    <div
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5"
      aria-busy="true"
      aria-live="polite"
    >
      <span className="sr-only">Cargando</span>
      {Array.from({ length: celdas }).map((_, indice) => (
        <Skeleton key={indice} className="h-32" />
      ))}
    </div>
  )
}
