import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { BarraAccion } from '@/components/layout/BarraAccion'
import { EstadoCargaGrid } from '@/components/common/EstadoCarga'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { Button } from '@/components/ui/button'
import { CardBoton } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { catalogoApi } from '@/api/catalogoApi'
import { mensajeDeError } from '@/api/axiosClient'
import { useMesas } from '@/hooks/useMesas'
import { ESTADO_MESA } from '@/utils/constantes'
import { cn } from '@/utils/cn'

export function MesasAdminPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { mesas, cargando } = useMesas()

  const [enEdicion, setEnEdicion] = useState(null)
  const [porEliminar, setPorEliminar] = useState(null)

  const refrescar = () => queryClient.invalidateQueries({ queryKey: ['mesas'] })
  const alFallar = (error) => toast.error(mensajeDeError(error))

  const guardar = useMutation({
    mutationFn: ({ id, numero }) =>
      id ? catalogoApi.actualizarMesa(id, numero) : catalogoApi.crearMesa(numero),
    onSuccess: (mesa, variables) => {
      refrescar()
      setEnEdicion(null)
      toast.success(variables.id ? `Ahora es la mesa ${mesa.numero}` : `Mesa ${mesa.numero} agregada`)
    },
    onError: alFallar,
  })

  const eliminar = useMutation({
    mutationFn: (id) => catalogoApi.eliminarMesa(id),
    onSuccess: () => {
      const numero = porEliminar?.numero
      refrescar()
      setPorEliminar(null)
      setEnEdicion(null)
      toast.success(`Mesa ${numero} eliminada`)
    },
    onError: (error) => {
      setPorEliminar(null)
      alFallar(error)
    },
  })

  const ocupada = (mesa) => Boolean(mesa.pedido) || mesa.estado === ESTADO_MESA.OCUPADA

  return (
    <>
      <AppShell
        titulo="Mesas"
        subtitulo="Toca una mesa para cambiar su número"
        onVolver={() => navigate('/admin')}
        conNav={false}
        ancho="ancho"
        barraInferior={
          <BarraAccion etiqueta="Agregar mesa" icono={Plus} onAccion={() => setEnEdicion({})} />
        }
      >
        {cargando ? (
          <EstadoCargaGrid celdas={8} />
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            {mesas.map((mesa) => (
              <li key={mesa.id}>
                <CardBoton
                  onClick={() => setEnEdicion(mesa)}
                  className={cn(
                    'flex min-h-28 flex-col justify-center px-4 py-4',
                    ocupada(mesa) && 'border-brasa-200 bg-brasa-50',
                  )}
                >
                  <span className="text-2xl font-extrabold text-carbon">Mesa {mesa.numero}</span>
                  <span
                    className={cn(
                      'mt-1 flex items-center gap-1.5 text-base font-bold',
                      ocupada(mesa) ? 'text-brasa-700' : 'text-hoja-700',
                    )}
                  >
                    <span
                      aria-hidden="true"
                      className={cn(
                        'size-2 rounded-full',
                        ocupada(mesa) ? 'bg-brasa-500' : 'bg-hoja-600',
                      )}
                    />
                    {ocupada(mesa) ? 'En uso' : 'Libre'}
                  </span>
                </CardBoton>
              </li>
            ))}
          </ul>
        )}
      </AppShell>

      <EditorMesa
        mesa={enEdicion}
        ocupada={enEdicion ? ocupada(enEdicion) : false}
        guardando={guardar.isPending}
        onCerrar={() => setEnEdicion(null)}
        onGuardar={(numero) => guardar.mutate({ id: enEdicion?.id, numero })}
        onEliminar={() => setPorEliminar(enEdicion)}
      />

      <ConfirmDialog
        abierto={Boolean(porEliminar)}
        onOpenChange={(abierto) => !abierto && setPorEliminar(null)}
        titulo={`¿Eliminar la mesa ${porEliminar?.numero}?`}
        descripcion="Dejará de aparecer al tomar pedidos."
        textoConfirmar="Sí, eliminar"
        destructivo
        enProceso={eliminar.isPending}
        onConfirmar={() => eliminar.mutate(porEliminar.id)}
      />
    </>
  )
}

function EditorMesa({ mesa, ocupada, onCerrar, onGuardar, onEliminar, guardando }) {
  const esNueva = !mesa?.id
  const [numero, setNumero] = useState('')

  useEffect(() => {
    if (mesa) setNumero(mesa.numero ? String(mesa.numero) : '')
  }, [mesa])

  const valor = Number(numero)
  const valido = numero !== '' && Number.isInteger(valor) && valor > 0

  return (
    <Sheet open={Boolean(mesa)} onOpenChange={(abierto) => !abierto && onCerrar()}>
      <SheetContent titulo={esNueva ? 'Agregar mesa' : `Mesa ${mesa?.numero}`}>
        <div className="flex flex-col gap-4 pb-2">
          <div className="flex flex-col gap-2">
            <Label htmlFor="numero-mesa">Número de la mesa</Label>
            <Input
              id="numero-mesa"
              inputMode="numeric"
              value={numero}
              onChange={(e) => setNumero(e.target.value.replace(/\D/g, ''))}
              className="monto text-2xl font-bold"
            />
          </div>

          <Button
            tamano="grande"
            disabled={!valido || guardando}
            onClick={() => onGuardar(valor)}
          >
            {guardando ? 'Guardando...' : 'Guardar'}
          </Button>

          {!esNueva ? (
            <div className="flex flex-col gap-2 border-t border-borde pt-4">
              <Button
                variante="destructiva"
                tamano="grande"
                disabled={ocupada}
                onClick={onEliminar}
              >
                <Trash2 size={22} />
                Eliminar mesa
              </Button>
              {ocupada ? (
                <p className="text-base font-semibold text-tinta">
                  Mesa en uso. Cóbrala antes de eliminarla.
                </p>
              ) : null}
            </div>
          ) : null}

          <Button variante="secundaria" tamano="grande" onClick={onCerrar}>
            Cancelar
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
