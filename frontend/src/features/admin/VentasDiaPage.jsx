import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AppShell } from '@/components/layout/AppShell'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { pedidosApi } from '@/api/pedidosApi'
import { boletasApi } from '@/api/boletasApi'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { etiquetaItem } from '@/utils/etiquetas'
import { esDelDia, fechaDeHoy, fechaLegible } from '@/utils/fechas'
import { ESTADO_PEDIDO } from '@/utils/constantes'
import { cn } from '@/utils/cn'

export function VentasDiaPage() {
  const navigate = useNavigate()
  const hoy = fechaDeHoy()
  const [dia, setDia] = useState(hoy)

  // TODO: mover la agregación al backend cuando existan los endpoints de reportes.
  // Por ahora se traen pedidos y boletas y se suman en el cliente.
  const pedidos = useQuery({ queryKey: ['pedidos', 'todos'], queryFn: () => pedidosApi.listar() })
  const boletas = useQuery({
    queryKey: ['boletas', dia],
    queryFn: () => boletasApi.listar({ desde: dia, hasta: dia }),
  })

  const cargando = pedidos.isPending || boletas.isPending

  const delDia = (pedidos.data ?? []).filter((pedido) => esDelDia(pedido.creadoEn, dia))
  const cobrados = delDia.filter((pedido) => pedido.estado === ESTADO_PEDIDO.PAGADO)
  const total = cobrados.reduce((suma, pedido) => suma + Number(pedido.total ?? 0), 0)
  const promedio = cobrados.length > 0 ? total / cobrados.length : 0
  const masVendido = rankearItems(delDia)

  return (
    <AppShell
      titulo="Ventas del día"
      subtitulo={fechaLegible(dia)}
      onVolver={() => navigate('/admin')}
      conNav={false}
      ancho="ancho"
    >
      <div className="mb-5 flex items-center gap-3">
        <label htmlFor="dia-ventas" className="shrink-0 text-base font-semibold text-tinta">
          Ver día
        </label>
        <Input
          id="dia-ventas"
          type="date"
          value={dia}
          max={hoy}
          onChange={(e) => setDia(e.target.value || hoy)}
          className="text-base sm:max-w-xs"
        />
      </div>

      {cargando ? (
        <EstadoCarga filas={4} />
      ) : (
        <div className="flex flex-col gap-4">
          {/* El total del día sobre carbón: es el único número que se busca al entrar. */}
          <div className="relative overflow-hidden rounded-app-lg bg-carbon p-6 text-center sm:p-8">
            <div
              aria-hidden="true"
              className="absolute -right-16 -top-16 size-56 rounded-full bg-brasa-600/30 blur-3xl"
            />
            <div className="relative">
              <p className="text-base font-bold uppercase tracking-wide text-brasa-300">
                Total vendido {fechaLegible(dia)}
              </p>
              <p className="monto mt-1 text-5xl font-extrabold leading-none text-white sm:text-6xl">
                {formatoMoneda(total)}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
            <Dato etiqueta="Pedidos atendidos" valor={cobrados.length} />
            <Dato etiqueta="Boletas emitidas" valor={(boletas.data ?? []).length} />
            <Dato
              etiqueta="Gasto promedio por cliente"
              valor={formatoMoneda(promedio)}
              className="col-span-2 lg:col-span-1"
            />
          </div>

          <section>
            <h2 className="mb-3 text-xl font-bold text-carbon">
              Lo más vendido {fechaLegible(dia)}
            </h2>
            {masVendido.length === 0 ? (
              <p className="text-base text-tinta">Ese día no se registraron ventas.</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {masVendido.map((fila, indice) => (
                  <li key={fila.nombre}>
                    <Card className="relative overflow-hidden">
                      {/* La barra hace comparable de un vistazo lo que la cifra sola no. */}
                      <span
                        aria-hidden="true"
                        className="absolute inset-y-0 left-0 bg-brasa-50"
                        style={{ width: `${(fila.cantidad / masVendido[0].cantidad) * 100}%` }}
                      />
                      <div className="relative flex items-center gap-3 p-3">
                        <span className="monto flex size-8 shrink-0 items-center justify-center rounded-full bg-hundido text-sm font-extrabold text-tinta">
                          {indice + 1}
                        </span>
                        <span className="min-w-0 grow truncate text-lg font-semibold text-carbon">
                          {fila.nombre}
                        </span>
                        <span className="monto shrink-0 text-2xl font-extrabold text-carbon">
                          {fila.cantidad}
                        </span>
                      </div>
                    </Card>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </AppShell>
  )
}

function Dato({ etiqueta, valor, className }) {
  return (
    <Card className={cn('p-4', className)}>
      <p className="text-base font-semibold text-tinta">{etiqueta}</p>
      <p className="monto text-3xl font-extrabold leading-tight text-carbon">{valor}</p>
    </Card>
  )
}

function rankearItems(pedidos) {
  const conteo = new Map()
  for (const pedido of pedidos) {
    for (const item of pedido.items ?? []) {
      const nombre = etiquetaItem(item)
      conteo.set(nombre, (conteo.get(nombre) ?? 0) + item.cantidad)
    }
  }
  return [...conteo.entries()]
    .map(([nombre, cantidad]) => ({ nombre, cantidad }))
    .sort((a, b) => b.cantidad - a.cantidad)
}
