import { useNavigate } from 'react-router-dom'
import { LayoutGrid, Plus, ShoppingBag } from 'lucide-react'
import { AppShell } from '@/components/layout/AppShell'
import { EstadoCargaGrid } from '@/components/common/EstadoCarga'
import { EstadoVacio } from '@/components/common/EstadoVacio'
import { CardBoton } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { MesaCard } from './MesaCard'
import { useMesas } from '@/hooks/useMesas'
import { useAuth } from '@/hooks/useAuth'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { horaCorta } from '@/utils/fechas'

export function MesasPage() {
  const { mesas, paraLlevar, cargando } = useMesas()
  const { usuario } = useAuth()
  const navigate = useNavigate()

  const ocupadas = mesas.filter((mesa) => mesa.pedido).length

  return (
    <AppShell
      titulo="Mesas"
      subtitulo={
        cargando ? usuario?.nombre : `${ocupadas} de ${mesas.length} ocupadas · ${usuario?.nombre}`
      }
      ancho="ancho"
      conSalir
    >
      <section className="mb-7">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-bold text-carbon">Para llevar</h2>
          <Button variante="suave" onClick={() => navigate('/llevar/nuevo')}>
            <Plus size={22} />
            Nuevo
          </Button>
        </div>

        {paraLlevar.length === 0 ? (
          <CardBoton
            onClick={() => navigate('/llevar/nuevo')}
            className="flex min-h-20 items-center gap-3 border-dashed border-borde-fuerte px-4 py-3"
          >
            <span className="flex size-11 shrink-0 items-center justify-center rounded-app bg-hundido text-brasa-600">
              <ShoppingBag size={24} />
            </span>
            <span className="min-w-0">
              <span className="block text-lg font-bold text-carbon">Nuevo pedido para llevar</span>
              <span className="block text-base text-tinta">
                Para el cliente que no se sienta en mesa
              </span>
            </span>
          </CardBoton>
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {paraLlevar.map((pedido) => (
              <li key={pedido.id}>
                <CardBoton
                  onClick={() => navigate(`/llevar/${pedido.id}`)}
                  className="flex min-h-20 items-center gap-3 border-brasa-200 bg-brasa-50 px-4 py-3"
                >
                  <span className="flex size-11 shrink-0 items-center justify-center rounded-app bg-brasa-100 text-brasa-700">
                    <ShoppingBag size={24} />
                  </span>
                  <span className="min-w-0 grow">
                    <span className="block text-lg font-bold leading-tight text-carbon">
                      Para llevar {pedido.numeroLlevar}
                    </span>
                    <span className="block text-sm font-semibold text-brasa-700">
                      {horaCorta(pedido.creadoEn)}
                    </span>
                  </span>
                  <span className="monto shrink-0 text-xl font-extrabold text-carbon">
                    {formatoMoneda(pedido.total)}
                  </span>
                </CardBoton>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-bold text-carbon">En el local</h2>

        {cargando ? (
          <EstadoCargaGrid celdas={8} />
        ) : mesas.length === 0 ? (
          <EstadoVacio
            icono={LayoutGrid}
            titulo="Todavía no hay mesas"
            descripcion="Pídele al administrador que las cree."
          />
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            {mesas.map((mesa) => (
              <li key={mesa.id}>
                {/* Si la mesa está unida a otra, se entra por la principal, que es
                    donde vive la cuenta. */}
                <MesaCard
                  mesa={mesa}
                  onAbrir={() => navigate(`/pedido/${mesa.pedido?.mesaId ?? mesa.id}`)}
                />
              </li>
            ))}
          </ul>
        )}
      </section>
    </AppShell>
  )
}
