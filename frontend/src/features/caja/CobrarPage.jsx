import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Ban,
  CheckCircle2,
  Clock3,
  Download,
  FlaskConical,
  Loader2,
  TriangleAlert,
} from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { TotalBar } from '@/components/layout/TotalBar'
import { Button } from '@/components/ui/button'
import { Card, CardBoton } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { EditarPrecioDialog } from '@/components/common/EditarPrecioDialog'
import { ListaItemsPedido } from '@/features/pedido/ListaItemsPedido'
import { AnularCuentaDialog } from '@/features/pedido/AnularCuentaDialog'
import { EnviarBoletaDialog } from '@/features/caja/EnviarBoletaDialog'
import { boletasApi, MONTO_QUE_EXIGE_DNI } from '@/api/boletasApi'
import { ESTADO_CONSULTA } from '@/api/clientesApi'
import { mensajeDeError } from '@/api/axiosClient'
import { useAccionesPedido, usePedido } from '@/hooks/usePedido'
import { useConsultaDni } from '@/hooks/useConsultaDni'
import { useAuth } from '@/hooks/useAuth'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { nombrePedido } from '@/utils/etiquetas'
import { horaCorta } from '@/utils/fechas'
import { ESTADO_PEDIDO, ESTADO_SUNAT, MEDIOS_PAGO, TIPO_BOLETA } from '@/utils/constantes'
import { cn } from '@/utils/cn'

export function CobrarPage() {
  const { pedidoId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { puedeCobrar } = useAuth()

  const { data: pedido, isPending } = usePedido(pedidoId)
  const { editarPrecio, anularPedido } = useAccionesPedido()

  const [itemEnEdicion, setItemEnEdicion] = useState(null)
  const [anulando, setAnulando] = useState(false)
  const [tipoBoleta, setTipoBoleta] = useState(TIPO_BOLETA.CONSUMO)
  const [medioPago, setMedioPago] = useState(MEDIOS_PAGO[0].valor)
  const [dni, setDni] = useState('')
  const [boleta, setBoleta] = useState(null)
  const [enviarAbierto, setEnviarAbierto] = useState(false)

  const generarBoleta = useMutation({
    mutationFn: () =>
      boletasApi.generar({
        pedidoId: Number(pedidoId),
        tipo: tipoBoleta,
        medioPago,
        dniCliente: dni,
      }),
    onSuccess: (emitida) => {
      setBoleta(emitida)
      // Quitar la cuenta cobrada del listado activo al toque (evita mesa "fantasma").
      queryClient.setQueryData(['pedidos', 'activos'], (prev) =>
        Array.isArray(prev) ? prev.filter((p) => p.id !== Number(pedidoId)) : prev,
      )
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      queryClient.invalidateQueries({ queryKey: ['mesas'] })
      queryClient.invalidateQueries({ queryKey: ['boletas'] })
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const reintentarEnvio = useMutation({
    mutationFn: () => boletasApi.reintentar(boleta.id),
    onSuccess: (actualizada) => {
      setBoleta(actualizada)
      if (actualizada.estadoSunat === ESTADO_SUNAT.ACEPTADO) {
        toast.success('Boleta aceptada por SUNAT')
      }
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const consultarSunat = useMutation({
    mutationFn: () => boletasApi.sincronizar(boleta.id),
    onSuccess: (actualizada) => {
      setBoleta(actualizada)
      if (actualizada.estadoSunat === ESTADO_SUNAT.ACEPTADO) {
        toast.success('Boleta aceptada por SUNAT')
      } else if (actualizada.estadoSunat === ESTADO_SUNAT.PENDIENTE) {
        toast.info('SUNAT todavía no la confirma')
      }
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const descargarPdf = useMutation({
    mutationFn: () => boletasApi.abrirPdf(boleta.id),
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const { consultando: consultandoDni, resultado: clienteDni } = useConsultaDni(dni)

  if (isPending) {
    return (
      <AppShell titulo="Cobrar" conNav={false} onVolver={() => navigate('/caja')}>
        <EstadoCarga filas={4} />
      </AppShell>
    )
  }

  if (!pedido) {
    return (
      <AppShell titulo="Cobrar" conNav={false} onVolver={() => navigate('/caja')}>
        <p className="text-lg text-tinta">Esta cuenta ya no existe.</p>
      </AppShell>
    )
  }

  // --- Pantalla de cobrado ---------------------------------------------------
  if (boleta) {
    // Una boleta simulada llega como aceptada, pero no existe ante SUNAT. Se muestra
    // aparte para que nadie la entregue creyendo que es un comprobante válido.
    const simulada = Boolean(boleta.simulada)
    const aceptada = boleta.estadoSunat === ESTADO_SUNAT.ACEPTADO && !simulada
    const rechazada = boleta.estadoSunat === ESTADO_SUNAT.ERROR
    // Un rechazo de SUNAT quema ese número: no se reenvía, hay que emitir otra boleta.
    const laRechazoSunat = rechazada && Boolean(boleta.externalId)

    return (
      <AppShell titulo="Cobrado" conNav={false}>
        <Card className="mx-auto flex max-w-md flex-col items-center gap-4 px-5 py-10 text-center">
          <span
            className={cn(
              'flex size-20 items-center justify-center rounded-full',
              aceptada && 'bg-hoja-100 text-hoja-600',
              rechazada && 'bg-alerta-suave text-alerta',
              !aceptada && !rechazada && 'bg-brasa-100 text-brasa-600',
            )}
          >
            {simulada ? (
              <FlaskConical size={44} />
            ) : aceptada ? (
              <CheckCircle2 size={44} />
            ) : rechazada ? (
              <TriangleAlert size={44} />
            ) : (
              <Clock3 size={44} />
            )}
          </span>

          <div>
            <p className="monto text-5xl font-extrabold leading-none text-carbon">
              {formatoMoneda(boleta.montoTotal)}
            </p>
            <p className="mt-2 text-lg font-semibold text-tinta">{nombrePedido(pedido)}</p>
          </div>

          {boleta.serie ? (
            <p className="text-lg text-tinta">
              Boleta{' '}
              <span className="monto font-bold text-carbon">
                {boleta.serie}-{boleta.correlativo}
              </span>
            </p>
          ) : null}

          {simulada ? (
            <div className="w-full rounded-app border-2 border-brasa-200 bg-brasa-50 p-4 text-left">
              <p className="text-base font-semibold text-brasa-700">
                Boleta de prueba, sin validez fiscal.
              </p>
              <p className="mt-1 text-base text-tinta">
                El sistema no está conectado con FactuSmart, así que esta venta no tiene
                comprobante ante SUNAT. No la entregues a un cliente.
              </p>
            </div>
          ) : null}

          {/* Sin FactuSmart conectado la boleta figura aceptada pero no existe PDF. */}
          {boleta.descargable ? (
            <Button
              tamano="grande"
              className="w-full"
              disabled={descargarPdf.isPending}
              onClick={() => descargarPdf.mutate()}
            >
              <Download size={22} />
              {descargarPdf.isPending ? 'Abriendo...' : 'Ver boleta para el cliente'}
            </Button>
          ) : null}

          {aceptada && boleta.urlPublicaPdf ? (
            <Button
              tamano="grande"
              variante="secundaria"
              className="w-full"
              onClick={() => setEnviarAbierto(true)}
            >
              ENVIAR AL CLIENTE
            </Button>
          ) : !aceptada && !rechazada && !simulada ? (
            <p className="text-sm text-tinta">
              Podrás enviarla cuando SUNAT la confirme
            </p>
          ) : null}

          <EnviarBoletaDialog
            abierto={enviarAbierto}
            onClose={() => setEnviarAbierto(false)}
            boleta={boleta}
          />

          {/* SUNAT tarda en confirmar y casi siempre se resuelve sola. No es un error. */}
          {!aceptada && !rechazada && !simulada ? (
            <div className="w-full rounded-app border-2 border-brasa-200 bg-brasa-50 p-4 text-left">
              <p className="text-base font-semibold text-brasa-700">
                Cobrado. SUNAT todavía no confirma la boleta.
              </p>
              <p className="mt-1 text-base text-tinta">
                Suele resolverse solo en unos minutos. Puedes seguir atendiendo.
              </p>
              <Button
                variante="secundaria"
                tamano="grande"
                className="mt-3 w-full"
                disabled={consultarSunat.isPending}
                onClick={() => consultarSunat.mutate()}
              >
                {consultarSunat.isPending ? 'Consultando...' : 'Consultar ahora'}
              </Button>
            </div>
          ) : null}

          {rechazada ? (
            <div className="w-full rounded-app border-2 border-alerta/30 bg-alerta-suave p-4 text-left">
              <p className="text-base font-semibold text-alerta">
                La venta quedó registrada. Lo único que falló fue la boleta.
              </p>
              <p className="mt-1 text-base text-tinta">
                {laRechazoSunat
                  ? 'SUNAT la rechazó y ese número ya no se puede usar. Avisa al administrador.'
                  : 'Puedes reintentar el envío ahora o después desde Caja.'}
              </p>
              {boleta.sunatDescripcion ? (
                <p className="mt-2 text-sm text-tinta">{boleta.sunatDescripcion}</p>
              ) : null}
              {!laRechazoSunat ? (
                <Button
                  tamano="grande"
                  className="mt-3 w-full"
                  disabled={reintentarEnvio.isPending}
                  onClick={() => reintentarEnvio.mutate()}
                >
                  {reintentarEnvio.isPending ? 'Reintentando...' : 'Reintentar envío'}
                </Button>
              ) : null}
            </div>
          ) : null}

          <Button
            variante={aceptada ? 'secundaria' : 'principal'}
            tamano="grande"
            className="mt-2 w-full"
            onClick={() => navigate('/mesas')}
          >
            Volver a mesas
          </Button>
        </Card>
      </AppShell>
    )
  }

  // --- Cuenta ya pagada en otro momento --------------------------------------
  if (pedido.estado === ESTADO_PEDIDO.PAGADO) {
    return (
      <AppShell titulo="Cobrar" conNav={false} onVolver={() => navigate('/caja')}>
        <Card className="mx-auto flex max-w-md flex-col items-center gap-3 px-5 py-10 text-center">
          <span className="flex size-16 items-center justify-center rounded-full bg-hoja-100 text-hoja-600">
            <CheckCircle2 size={36} />
          </span>
          <p className="text-xl font-bold text-carbon">Esta cuenta ya está cobrada</p>
          <p className="text-base text-tinta">{nombrePedido(pedido)}</p>
          <Button tamano="grande" className="mt-2 w-full" onClick={() => navigate('/caja')}>
            Volver a Caja
          </Button>
        </Card>
      </AppShell>
    )
  }

  const abierto = pedido.estado === ESTADO_PEDIDO.ABIERTO

  // En una anticuchería casi nadie da su DNI, pero SUNAT lo exige desde S/700.
  const dniObligatorio = Number(pedido.total) >= MONTO_QUE_EXIGE_DNI
  const dniCompleto = /^\d{8}$/.test(dni)
  const dniBloqueaEmision = dni.length > 0 ? !dniCompleto : dniObligatorio

  async function guardarPrecio(precioFinal, motivo) {
    try {
      await editarPrecio.mutateAsync({
        pedidoId: pedido.id,
        itemId: itemEnEdicion.id,
        precioFinal,
        motivo,
      })
      setItemEnEdicion(null)
      toast.success('Precio actualizado')
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  async function anularCuenta(motivo) {
    try {
      await anularPedido.mutateAsync({ pedidoId: pedido.id, motivo })
      setAnulando(false)
      toast.success('Cuenta anulada. La mesa quedó libre.')
      navigate('/caja')
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  const subtitulo = [
    `Entró ${horaCorta(pedido.creadoEn)}`,
    pedido.cerradoEn ? `cerró ${horaCorta(pedido.cerradoEn)}` : null,
    `${pedido.items?.length ?? 0} ítem${(pedido.items?.length ?? 0) === 1 ? '' : 's'}`,
  ]
    .filter(Boolean)
    .join(' · ')

  const barra = (
    <TotalBar
      total={pedido.total}
      etiqueta="Total a cobrar"
      accion={generarBoleta.isPending ? 'Cobrando...' : 'Cobrar'}
      accionDeshabilitada={generarBoleta.isPending || dniBloqueaEmision}
      onAccion={() => generarBoleta.mutate()}
    />
  )

  return (
    <>
      <AppShell
        titulo={`Cobrar ${nombrePedido(pedido).toLowerCase()}`}
        subtitulo={subtitulo}
        onVolver={() => navigate('/caja')}
        conNav={false}
        barraInferior={barra}
      >
        <section className="mb-6">
          <h2 className="mb-2 text-lg font-bold text-carbon">Detalle de la mesa</h2>
          <ListaItemsPedido
            items={pedido.items}
            onEditarPrecio={abierto && puedeCobrar ? setItemEnEdicion : null}
          />
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-xl font-bold text-carbon">Tipo de boleta</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <OpcionBoleta
              titulo="Por consumo"
              descripcion="Solo el monto total"
              activa={tipoBoleta === TIPO_BOLETA.CONSUMO}
              onClick={() => setTipoBoleta(TIPO_BOLETA.CONSUMO)}
            />
            <OpcionBoleta
              titulo="Detallada"
              descripcion="Lista de todo lo consumido"
              activa={tipoBoleta === TIPO_BOLETA.DETALLADO}
              onClick={() => setTipoBoleta(TIPO_BOLETA.DETALLADO)}
            />
          </div>

          <h2 className="mt-3 text-xl font-bold text-carbon">¿Cómo paga?</h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {MEDIOS_PAGO.map((medio) => (
              <CardBoton
                key={medio.valor}
                seleccionada={medioPago === medio.valor}
                aria-pressed={medioPago === medio.valor}
                onClick={() => setMedioPago(medio.valor)}
                className="min-h-16 px-4 text-center text-lg font-bold text-carbon"
              >
                {medio.etiqueta}
              </CardBoton>
            ))}
          </div>

          <h2 className="mt-3 text-xl font-bold text-carbon">
            DNI del cliente{dniObligatorio ? '' : ' (opcional)'}
          </h2>
          <Input
            value={dni}
            onChange={(evento) => setDni(evento.target.value.replace(/\D/g, '').slice(0, 8))}
            inputMode="numeric"
            autoComplete="off"
            placeholder="8 dígitos"
            aria-invalid={dniBloqueaEmision}
            className="monto max-w-xs text-2xl tracking-widest"
          />

          <VerificacionDni consultando={consultandoDni} resultado={clienteDni} />

          <p className="text-base text-tinta">
            {dniObligatorio
              ? 'Desde S/700 SUNAT exige identificar al comprador.'
              : 'Solo si el cliente quiere la boleta a su nombre.'}
          </p>
        </section>

        <Button
          variante="destructiva"
          tamano="grande"
          className="mt-8 w-full"
          onClick={() => setAnulando(true)}
        >
          <Ban size={22} />
          Anular cuenta sin cobrar
        </Button>
      </AppShell>

      <EditarPrecioDialog
        item={itemEnEdicion}
        onOpenChange={(valor) => !valor && setItemEnEdicion(null)}
        onGuardar={guardarPrecio}
        guardando={editarPrecio.isPending}
      />

      <AnularCuentaDialog
        abierto={anulando}
        onOpenChange={setAnulando}
        pedido={pedido}
        onAnular={anularCuenta}
        anulando={anularPedido.isPending}
      />
    </>
  )
}

/**
 * Confirma en voz alta a quién pertenece el DNI antes de emitir. Nunca bloquea el cobro:
 * la boleta viaja solo con el número, que es lo único que espera FactuSmart.
 */
function VerificacionDni({ consultando, resultado }) {
  if (consultando) {
    return (
      <p className="flex items-center gap-2 text-base text-tinta" role="status">
        <Loader2 size={18} className="animate-spin" />
        Buscando el nombre...
      </p>
    )
  }

  if (!resultado) return null

  if (resultado.estado === ESTADO_CONSULTA.ENCONTRADO) {
    return (
      <p className="flex items-start gap-2 text-lg font-bold text-hoja-600" role="status">
        <CheckCircle2 size={22} className="mt-0.5 shrink-0" />
        {resultado.nombreCompleto}
      </p>
    )
  }

  if (resultado.estado === ESTADO_CONSULTA.NO_ENCONTRADO) {
    return (
      <p className="flex items-start gap-2 text-base font-semibold text-alerta" role="status">
        <TriangleAlert size={20} className="mt-0.5 shrink-0" />
        <span>
          No se encontró ese DNI. Verifica el número.
          {/* El padrón público no tiene a todo el mundo, así que "no está" no siempre
              significa "está mal". Si el cliente insiste, se emite igual. */}
          <span className="block font-normal text-tinta">
            Si el cliente confirma que es correcto, puedes emitir igual.
          </span>
        </span>
      </p>
    )
  }

  return (
    <p className="flex items-start gap-2 text-base text-tinta" role="status">
      <TriangleAlert size={20} className="mt-0.5 shrink-0 text-brasa-600" />
      No se pudo verificar el nombre, pero puedes emitir la boleta igual.
    </p>
  )
}

function OpcionBoleta({ titulo, descripcion, activa, onClick }) {
  return (
    <CardBoton
      seleccionada={activa}
      aria-pressed={activa}
      onClick={onClick}
      className="min-h-16 px-4 py-3"
    >
      <span className="block text-lg font-bold text-carbon">{titulo}</span>
      <span className="block text-base text-tinta">{descripcion}</span>
    </CardBoton>
  )
}
