import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { BarraAccion } from '@/components/layout/BarraAccion'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { EstadoVacio } from '@/components/common/EstadoVacio'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { AvisoCambioPrecio } from '@/components/common/AvisoCambioPrecio'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardBoton } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { catalogoApi } from '@/api/catalogoApi'
import { mensajeDeError } from '@/api/axiosClient'
import { useProductos } from '@/hooks/useCatalogo'
import { formatoMoneda, formatoNumero } from '@/utils/formatoMoneda'
import { INFO_TIPOS_PRODUCTO, TIPO_PRODUCTO } from '@/utils/constantes'
import { cn } from '@/utils/cn'

const PESTANAS = [TIPO_PRODUCTO.ANTICUCHO, TIPO_PRODUCTO.BEBIDA, TIPO_PRODUCTO.EXTRA]

export function ProductosPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data, isPending } = useProductos({ incluirInactivos: true })

  const [pestana, setPestana] = useState(TIPO_PRODUCTO.ANTICUCHO)
  const [editando, setEditando] = useState(null)
  const [porCambiarVisibilidad, setPorCambiarVisibilidad] = useState(null)

  const refrescarCatalogo = () => queryClient.invalidateQueries({ queryKey: ['productos'] })

  const guardar = useMutation({
    mutationFn: ({ id, producto }) =>
      id ? catalogoApi.actualizarProducto(id, producto) : catalogoApi.crearProducto(producto),
    onSuccess: (guardado, variables) => {
      refrescarCatalogo()
      setEditando(null)
      toast.success(mensajeDeGuardado(guardado, variables.precioAnterior))
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const cambiarVisibilidad = useMutation({
    mutationFn: ({ id, activo }) => catalogoApi.cambiarEstadoProducto(id, activo),
    onSuccess: (producto) => {
      refrescarCatalogo()
      setPorCambiarVisibilidad(null)
      toast.success(
        producto.activo
          ? `${producto.nombre} vuelve al menú`
          : `${producto.nombre} ya no aparece en el menú`,
      )
    },
    onError: (error) => {
      setPorCambiarVisibilidad(null)
      toast.error(mensajeDeError(error))
    },
  })

  const visibles = ordenar((data ?? []).filter((p) => p.tipo === pestana))

  return (
    <>
      <AppShell
        titulo="Precios y productos"
        onVolver={() => navigate('/admin')}
        conNav={false}
        ancho="ancho"
        barraInferior={
          <BarraAccion
            etiqueta="Agregar producto"
            icono={Plus}
            onAccion={() => setEditando({ tipo: pestana })}
          />
        }
      >
        {/* Control segmentado: se ve de un vistazo cuál de los tres grupos está abierto. */}
        <div className="mb-5 flex rounded-app border border-borde bg-hundido p-1">
          {PESTANAS.map((tipo) => (
            <button
              key={tipo}
              type="button"
              aria-pressed={pestana === tipo}
              onClick={() => setPestana(tipo)}
              className={cn(
                'min-h-12 flex-1 rounded-[0.625rem] px-2 text-base font-bold transition-all',
                pestana === tipo
                  ? 'bg-superficie text-brasa-700 shadow-suave'
                  : 'text-tinta hover:text-carbon',
              )}
            >
              {INFO_TIPOS_PRODUCTO[tipo].plural}
            </button>
          ))}
        </div>

        {isPending ? (
          <EstadoCarga filas={6} alto="h-16" />
        ) : visibles.length === 0 ? (
          <EstadoVacio
            titulo={`Todavía no hay ${INFO_TIPOS_PRODUCTO[pestana].plural.toLowerCase()}`}
            descripcion={`Agrega el primero: ${INFO_TIPOS_PRODUCTO[pestana].ejemplo}`}
          />
        ) : (
          <ul className="grid gap-2 lg:grid-cols-2">
            {visibles.map((producto) => (
              <li key={producto.id}>
                <Card
                  className={cn(
                    'flex items-center gap-3 p-3',
                    !producto.activo && 'bg-hundido/50 opacity-75',
                  )}
                >
                  <button
                    type="button"
                    onClick={() => setEditando(producto)}
                    className="min-w-0 grow rounded-app py-1 text-left"
                  >
                    <span className="block truncate text-lg font-bold text-carbon">
                      {producto.nombre}
                    </span>
                    <span className="text-sm text-tinta">
                      {producto.activo ? 'Toca para editar' : null}
                    </span>
                    {!producto.activo ? <Badge className="mt-1">Oculto</Badge> : null}
                  </button>

                  <span className="monto shrink-0 text-right text-2xl font-extrabold text-carbon">
                    {formatoMoneda(producto.precioUnitario)}
                  </span>

                  <Switch
                    checked={producto.activo}
                    aria-label={`${producto.activo ? 'Ocultar' : 'Mostrar'} ${producto.nombre}`}
                    onCheckedChange={() => setPorCambiarVisibilidad(producto)}
                  />
                </Card>
              </li>
            ))}
          </ul>
        )}
      </AppShell>

      <EditorProducto
        producto={editando}
        guardando={guardar.isPending}
        onCerrar={() => setEditando(null)}
        onGuardar={(producto, precioAnterior) =>
          guardar.mutate({ id: editando?.id, producto, precioAnterior })
        }
      />

      <ConfirmDialog
        abierto={Boolean(porCambiarVisibilidad)}
        onOpenChange={(abierto) => !abierto && setPorCambiarVisibilidad(null)}
        titulo={
          porCambiarVisibilidad?.activo
            ? `¿Ocultar ${porCambiarVisibilidad?.nombre} del menú?`
            : `¿Mostrar ${porCambiarVisibilidad?.nombre} en el menú?`
        }
        descripcion={
          porCambiarVisibilidad?.activo
            ? 'Ya no aparecerá al tomar pedidos, pero se mantiene en las ventas anteriores.'
            : 'Volverá a aparecer al tomar pedidos.'
        }
        textoConfirmar={porCambiarVisibilidad?.activo ? 'Sí, ocultar' : 'Sí, mostrar'}
        destructivo={porCambiarVisibilidad?.activo}
        enProceso={cambiarVisibilidad.isPending}
        onConfirmar={() =>
          cambiarVisibilidad.mutate({
            id: porCambiarVisibilidad.id,
            activo: !porCambiarVisibilidad.activo,
          })
        }
      />
    </>
  )
}

function EditorProducto({ producto, onCerrar, onGuardar, guardando }) {
  const esNuevo = !producto?.id
  const [nombre, setNombre] = useState('')
  const [precio, setPrecio] = useState('')
  const [tipo, setTipo] = useState(TIPO_PRODUCTO.ANTICUCHO)
  const [confirmando, setConfirmando] = useState(false)

  useEffect(() => {
    if (producto) {
      setNombre(producto.nombre ?? '')
      setPrecio(producto.precioUnitario != null ? formatoNumero(producto.precioUnitario) : '')
      setTipo(producto.tipo ?? TIPO_PRODUCTO.ANTICUCHO)
      setConfirmando(false)
    }
  }, [producto])

  const nombreValido = nombre.trim().length > 0
  const precioValido = /^\d+([.,]\d{1,2})?$/.test(precio.trim()) && Number(precio.replace(',', '.')) > 0
  const valor = Number(precio.replace(',', '.'))
  const precioAnterior = producto?.precioUnitario != null ? Number(producto.precioUnitario) : null
  const cambiaElPrecio = !esNuevo && precioAnterior !== null && valor !== precioAnterior

  function continuar() {
    if (cambiaElPrecio && !confirmando) {
      setConfirmando(true)
      return
    }
    onGuardar({ nombre: nombre.trim(), tipo, precioUnitario: valor }, precioAnterior)
  }

  return (
    <Sheet open={Boolean(producto)} onOpenChange={(abierto) => !abierto && onCerrar()}>
      <SheetContent titulo={esNuevo ? 'Agregar producto' : 'Editar producto'}>
        {confirmando ? (
          <div className="flex flex-col gap-4 pb-2">
            <AvisoCambioPrecio nombre={nombre.trim()} anterior={precioAnterior} nuevo={valor} />

            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={() => setConfirmando(false)}>
                Cancelar
              </Button>
              <Button tamano="grande" disabled={guardando} onClick={continuar}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-4 pb-2">
            {esNuevo ? (
              <div className="flex flex-col gap-2">
                <Label>¿Qué es?</Label>
                {PESTANAS.map((opcion) => (
                  <CardBoton
                    key={opcion}
                    seleccionada={tipo === opcion}
                    aria-pressed={tipo === opcion}
                    onClick={() => setTipo(opcion)}
                    className="min-h-16 px-4 py-2"
                  >
                    <span className="block text-lg font-bold text-carbon">
                      {INFO_TIPOS_PRODUCTO[opcion].etiqueta}
                    </span>
                    <span className="block text-base text-tinta">
                      {INFO_TIPOS_PRODUCTO[opcion].ejemplo}
                    </span>
                  </CardBoton>
                ))}
              </div>
            ) : null}

            <div className="flex flex-col gap-2">
              <Label htmlFor="nombre-producto">Nombre</Label>
              <Input
                id="nombre-producto"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                maxLength={50}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="precio-producto">Precio</Label>
              <div className="flex items-center gap-2">
                <span className="text-2xl font-bold text-tinta">S/</span>
                <Input
                  id="precio-producto"
                  inputMode="decimal"
                  value={precio}
                  onChange={(e) => setPrecio(e.target.value.replace(/[^\d.,]/g, ''))}
                  className="monto text-2xl font-bold"
                  placeholder="0.00"
                />
              </div>
              {precio && !precioValido ? (
                <p className="text-base font-semibold text-alerta">
                  Escribe un precio mayor a cero, con máximo dos decimales.
                </p>
              ) : null}
            </div>

            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={onCerrar}>
                Cancelar
              </Button>
              <Button
                tamano="grande"
                disabled={!nombreValido || !precioValido || guardando}
                onClick={continuar}
              >
                {guardando ? 'Guardando...' : 'Guardar'}
              </Button>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}

/** Visibles primero y alfabéticos; los ocultos al final. */
function ordenar(productos) {
  return [...productos].sort((a, b) => {
    if (a.activo !== b.activo) return a.activo ? -1 : 1
    return a.nombre.localeCompare(b.nombre, 'es')
  })
}

function mensajeDeGuardado(producto, precioAnterior) {
  if (precioAnterior == null) {
    return `Se agregó ${producto.nombre} al menú`
  }
  if (Number(producto.precioUnitario) !== precioAnterior) {
    return `Precio de ${producto.nombre} actualizado a ${formatoMoneda(producto.precioUnitario)}`
  }
  return `${producto.nombre} actualizado`
}
