import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeftRight, Ban, Plus, ShoppingBag, UtensilsCrossed } from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { TotalBar } from '@/components/layout/TotalBar'
import { Button } from '@/components/ui/button'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { EstadoVacio } from '@/components/common/EstadoVacio'
import { EditarPrecioDialog } from '@/components/common/EditarPrecioDialog'
import { ListaItemsPedido } from './ListaItemsPedido'
import { SelectorTipoItem } from './SelectorTipoItem'
import { ArmadorAnticucho } from './ArmadorAnticucho'
import { ArmadorCombo } from './ArmadorCombo'
import { SelectorSimple } from './SelectorSimple'
import { GestionMesasSheet } from './GestionMesasSheet'
import { AnularCuentaDialog } from './AnularCuentaDialog'
import { useAccionesPedido, usePedido, usePedidoDeMesa } from '@/hooks/usePedido'
import { useMesas } from '@/hooks/useMesas'
import { useAuth } from '@/hooks/useAuth'
import { mensajeDeError } from '@/api/axiosClient'
import { horaCorta } from '@/utils/fechas'
import { nombrePedido } from '@/utils/etiquetas'
import { TIPO_ITEM } from '@/utils/constantes'

export function PedidoPage({ paraLlevar = false }) {
  const { mesaId, pedidoId } = useParams()
  const navigate = useNavigate()
  const { puedeCobrar } = useAuth()
  const { mesas, libres: mesasLibres } = useMesas()

  // Para llevar se entra por /llevar/nuevo y la URL recién toma el id al guardar el primer plato.
  const idExistente = paraLlevar && pedidoId !== 'nuevo' ? pedidoId : null
  const deLlevar = usePedido(idExistente)
  const deMesa = usePedidoDeMesa(paraLlevar ? null : mesaId)

  const pedido = paraLlevar ? (deLlevar.data ?? null) : deMesa.pedido
  const cargando = paraLlevar ? Boolean(idExistente) && deLlevar.isPending : deMesa.cargando

  const {
    crearPedido,
    agregarItem,
    quitarItem,
    editarPrecio,
    anularPedido,
    moverAMesa,
    unirMesa,
    separarMesa,
  } = useAccionesPedido()

  const [selectorAbierto, setSelectorAbierto] = useState(false)
  const [armador, setArmador] = useState(null)
  const [itemEnEdicion, setItemEnEdicion] = useState(null)
  const [gestionandoMesas, setGestionandoMesas] = useState(false)
  const [anulando, setAnulando] = useState(false)

  const mesa = mesas.find((m) => m.id === Number(mesaId))
  const items = pedido?.items ?? []
  const guardando = crearPedido.isPending || agregarItem.isPending

  /**
   * El pedido se crea recién con el primer ítem. Así, entrar a una mesa por
   * curiosidad y salir no la deja ocupada con una cuenta vacía.
   */
  async function guardarItems(nuevos) {
    try {
      let id = pedido?.id
      if (!id) {
        const creado = await crearPedido.mutateAsync(paraLlevar ? null : Number(mesaId))
        id = creado.id
        if (paraLlevar) navigate(`/llevar/${id}`, { replace: true })
      }
      for (const item of nuevos) {
        await agregarItem.mutateAsync({ pedidoId: id, item })
      }
      setArmador(null)
      toast.success('Agregado al pedido')
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  async function quitar(item) {
    try {
      await quitarItem.mutateAsync({ pedidoId: pedido.id, itemId: item.id })
      toast.success('Ítem quitado')
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

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
      toast.success(paraLlevar ? 'Pedido anulado' : 'Cuenta anulada. La mesa quedó libre.')
      navigate('/mesas')
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  /** Cambia una mesa de la cuenta y avisa el resultado en las palabras del salón. */
  async function cambiarMesas(mutacion, mesaId, mensaje) {
    try {
      await mutacion.mutateAsync({ pedidoId: pedido.id, mesaId })
      setGestionandoMesas(false)
      toast.success(mensaje)
      return true
    } catch (error) {
      toast.error(mensajeDeError(error))
      return false
    }
  }

  const titulo = paraLlevar
    ? pedido?.numeroLlevar
      ? `Para llevar ${pedido.numeroLlevar}`
      : 'Para llevar'
    : pedido
      ? nombrePedido(pedido)
      : mesa
        ? `Mesa ${mesa.numero}`
        : 'Mesa'

  const barra = (
    <TotalBar
      total={pedido?.total ?? 0}
      accion={puedeCobrar ? 'Cobrar' : 'Listo'}
      accionDeshabilitada={puedeCobrar && !pedido}
      onAccion={() => {
        if (puedeCobrar && pedido) {
          navigate(`/cobrar/${pedido.id}`)
        } else {
          navigate('/mesas')
        }
      }}
    />
  )

  return (
    <>
      <AppShell
        titulo={titulo}
        subtitulo={pedido ? `Abierto ${horaCorta(pedido.creadoEn)}` : null}
        onVolver={() => navigate('/mesas')}
        conNav={false}
        barraInferior={barra}
      >
        {cargando ? (
          <EstadoCarga filas={3} />
        ) : (
          <div className="flex flex-col gap-4">
            {items.length === 0 ? (
              <EstadoVacio
                icono={paraLlevar ? ShoppingBag : UtensilsCrossed}
                titulo={
                  paraLlevar ? 'Pedido para llevar' : 'Esta mesa no tiene nada todavía'
                }
                descripcion="Agrega el primer plato para abrir la cuenta."
                accion="+ Agregar"
                onAccion={() => setSelectorAbierto(true)}
              />
            ) : (
              <>
                <ListaItemsPedido
                  items={items}
                  onQuitar={quitar}
                  onEditarPrecio={setItemEnEdicion}
                  quitando={quitarItem.isPending}
                />
                <Button tamano="grande" className="w-full" onClick={() => setSelectorAbierto(true)}>
                  <Plus size={24} />
                  Agregar
                </Button>

                {/* Los grupos juntan mesas o se cambian de sitio a mitad de la noche. */}
                {!paraLlevar ? (
                  <Button
                    variante="secundaria"
                    tamano="grande"
                    className="w-full"
                    onClick={() => setGestionandoMesas(true)}
                  >
                    <ArrowLeftRight size={22} />
                    Cambiar o unir mesa
                  </Button>
                ) : null}
              </>
            )}

            {/*
              También cuando la cuenta quedó sin ítems: quitarlos todos no libera la mesa,
              y sin esto una mesa abierta por error se pierde hasta que alguien la cobre.
            */}
            {pedido ? (
              <Button
                variante="destructiva"
                tamano="grande"
                className="w-full"
                onClick={() => setAnulando(true)}
              >
                <Ban size={22} />
                {paraLlevar ? 'Anular pedido' : 'Anular cuenta y liberar la mesa'}
              </Button>
            ) : null}
          </div>
        )}
      </AppShell>

      <SelectorTipoItem
        abierto={selectorAbierto}
        onOpenChange={setSelectorAbierto}
        onElegir={(tipo) => {
          setSelectorAbierto(false)
          setArmador(tipo)
        }}
      />

      <ArmadorAnticucho
        abierto={armador === TIPO_ITEM.ANTICUCHO}
        onOpenChange={(valor) => !valor && setArmador(null)}
        onAgregar={guardarItems}
        guardando={guardando}
      />

      <ArmadorCombo
        abierto={armador === TIPO_ITEM.COMBO}
        onOpenChange={(valor) => !valor && setArmador(null)}
        onAgregar={guardarItems}
        guardando={guardando}
      />

      <SelectorSimple
        abierto={armador === TIPO_ITEM.BEBIDA || armador === TIPO_ITEM.EXTRA}
        tipo={armador === TIPO_ITEM.EXTRA ? TIPO_ITEM.EXTRA : TIPO_ITEM.BEBIDA}
        onOpenChange={(valor) => !valor && setArmador(null)}
        onAgregar={guardarItems}
        guardando={guardando}
      />

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

      <GestionMesasSheet
        abierto={gestionandoMesas}
        onOpenChange={setGestionandoMesas}
        pedido={pedido}
        mesasLibres={mesasLibres}
        guardando={moverAMesa.isPending || unirMesa.isPending || separarMesa.isPending}
        onMover={async (mesaId) => {
          const numero = mesasLibres.find((m) => m.id === mesaId)?.numero
          // La mesa de la URL dejó de ser la de la cuenta, así que hay que seguirla.
          if (await cambiarMesas(moverAMesa, mesaId, `La cuenta pasó a la mesa ${numero}`)) {
            navigate(`/pedido/${mesaId}`, { replace: true })
          }
        }}
        onUnir={(mesaId) => {
          const numero = mesasLibres.find((m) => m.id === mesaId)?.numero
          cambiarMesas(unirMesa, mesaId, `Mesa ${numero} unida a esta cuenta`)
        }}
        onSeparar={(mesaId) => cambiarMesas(separarMesa, mesaId, 'Mesa separada')}
      />
    </>
  )
}
