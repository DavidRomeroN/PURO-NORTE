import { useQuery } from '@tanstack/react-query'
import { catalogoApi } from '@/api/catalogoApi'
import { TIPO_PRODUCTO } from '@/utils/constantes'

// El catálogo casi no cambia durante el servicio: se cachea y no se refresca solo.
const OPCIONES_CACHE = {
  staleTime: 5 * 60 * 1000,
  refetchOnWindowFocus: false,
}

export function useProductos({ incluirInactivos = false } = {}) {
  return useQuery({
    queryKey: ['productos', { incluirInactivos }],
    queryFn: () => catalogoApi.listarProductos({ incluirInactivos }),
    ...OPCIONES_CACHE,
  })
}

export function useCombos({ incluirInactivos = false } = {}) {
  return useQuery({
    queryKey: ['combos', { incluirInactivos }],
    queryFn: () => catalogoApi.listarCombos({ incluirInactivos }),
    ...OPCIONES_CACHE,
  })
}

/** Catálogo listo para armar un pedido, separado por tipo. */
export function useCatalogo() {
  const productos = useProductos()
  const combos = useCombos()

  const lista = productos.data ?? []

  return {
    cargando: productos.isPending || combos.isPending,
    error: productos.error ?? combos.error,
    anticuchos: lista.filter((p) => p.tipo === TIPO_PRODUCTO.ANTICUCHO),
    bebidas: lista.filter((p) => p.tipo === TIPO_PRODUCTO.BEBIDA),
    extras: lista.filter((p) => p.tipo === TIPO_PRODUCTO.EXTRA),
    combos: combos.data ?? [],
  }
}
