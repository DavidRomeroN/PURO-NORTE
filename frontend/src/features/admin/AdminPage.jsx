import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  CalendarDays,
  ChevronRight,
  LayoutGrid,
  ReceiptText,
  Tag,
  TriangleAlert,
  UsersRound,
  UtensilsCrossed,
} from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { Button } from '@/components/ui/button'
import { Card, CardBoton } from '@/components/ui/card'
import { boletasApi } from '@/api/boletasApi'
import { mensajeDeError } from '@/api/axiosClient'

// El subtítulo no es decorativo: es lo que permite elegir sección sin adivinar.
const SECCIONES = [
  {
    a: '/admin/ventas',
    titulo: 'Ventas del día',
    subtitulo: 'Cuánto se vendió hoy',
    Icono: CalendarDays,
  },
  {
    a: '/admin/productos',
    titulo: 'Precios y productos',
    subtitulo: 'Cambiar precios, agregar o quitar del menú',
    Icono: Tag,
  },
  {
    a: '/admin/combos',
    titulo: 'Combos',
    subtitulo: 'Precio y contenido del mixto y el especial',
    Icono: UtensilsCrossed,
  },
  {
    a: '/admin/usuarios',
    titulo: 'Personal',
    subtitulo: 'Quién puede usar el sistema',
    Icono: UsersRound,
  },
  {
    a: '/admin/mesas',
    titulo: 'Mesas',
    subtitulo: 'Agregar o quitar mesas del local',
    Icono: LayoutGrid,
  },
]

export function AdminPage() {
  const navigate = useNavigate()

  return (
    <AppShell titulo="Administración" subtitulo="El sistema por dentro" ancho="ancho" conSalir>
      <EstadoFacturacion />

      <ul className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {SECCIONES.map(({ a, titulo, subtitulo, Icono }) => (
          <li key={a}>
            <CardBoton
              onClick={() => navigate(a)}
              className="flex h-full items-center gap-4 px-4 py-4"
            >
              <span className="flex size-12 shrink-0 items-center justify-center rounded-app bg-brasa-50 text-brasa-600">
                <Icono size={26} />
              </span>
              <span className="min-w-0 grow">
                <span className="block text-xl font-bold leading-tight text-carbon">{titulo}</span>
                <span className="mt-0.5 block text-base text-tinta">{subtitulo}</span>
              </span>
              <ChevronRight size={26} className="shrink-0 text-tinta" />
            </CardBoton>
          </li>
        ))}
      </ul>
    </AppShell>
  )
}

/**
 * Cada boleta emitida gasta un crédito y no hay recarga automática. Con unas veinte al
 * mes los cien créditos duran meses, así que basta con verlos acá.
 */
function EstadoFacturacion() {
  const queryClient = useQueryClient()

  const consulta = useQuery({
    queryKey: ['boletas', 'creditos'],
    queryFn: boletasApi.creditos,
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

  const reactivar = useMutation({
    mutationFn: boletasApi.reactivarEmision,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boletas', 'creditos'] })
      toast.success('Emisión reactivada')
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  if (consulta.isPending || consulta.isError) {
    return null
  }

  const { creditosDisponibles, emisionActiva, motivoSuspension } = consulta.data
  const quedanPocos = creditosDisponibles <= 10

  return (
    <Card className="flex flex-col gap-3 px-4 py-4">
      <div className="flex items-center gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-app bg-brasa-50 text-brasa-600">
          <ReceiptText size={26} />
        </span>
        <div className="min-w-0 grow">
          <p className="text-base text-tinta">Boletas que se pueden emitir</p>
          <p className="monto text-2xl font-extrabold leading-tight text-carbon">
            {creditosDisponibles}
          </p>
        </div>
      </div>

      {quedanPocos ? (
        <p className="text-base font-semibold text-brasa-700">
          Quedan pocas. Conviene recargar créditos en FactuSmart antes de que se acaben.
        </p>
      ) : null}

      {!emisionActiva ? (
        <div className="rounded-app border-2 border-alerta/30 bg-alerta-suave p-3">
          <p className="flex items-center gap-2 text-base font-semibold text-alerta">
            <TriangleAlert size={20} />
            Emisión de boletas suspendida
          </p>
          <p className="mt-1 text-base text-tinta">
            Las ventas se siguen registrando, pero no salen boletas hasta corregir la
            configuración del RUC en FactuSmart. {motivoSuspension}
          </p>
          <Button
            variante="secundaria"
            tamano="grande"
            className="mt-3 w-full sm:w-auto"
            disabled={reactivar.isPending}
            onClick={() => reactivar.mutate()}
          >
            {reactivar.isPending ? 'Reactivando...' : 'Ya lo corregí, reactivar'}
          </Button>
        </div>
      ) : null}
    </Card>
  )
}
