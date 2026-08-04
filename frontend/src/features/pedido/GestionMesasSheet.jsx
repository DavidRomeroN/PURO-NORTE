import { useEffect, useState } from 'react'
import { ArrowLeftRight, Link2, Unlink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { CardBoton } from '@/components/ui/card'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { numerosDeMesa } from '@/utils/etiquetas'

const TITULOS = {
  menu: 'Mesas de esta cuenta',
  mover: 'Pasar la cuenta a...',
  unir: 'Unir otra mesa',
}

/**
 * Dos cosas que pasan seguido en el salón: un grupo grande se sienta en dos mesas
 * juntas y paga una sola cuenta, y un grupo se cambia de mesa a mitad de la noche.
 */
export function GestionMesasSheet({
  abierto,
  onOpenChange,
  pedido,
  mesasLibres,
  onMover,
  onUnir,
  onSeparar,
  guardando,
}) {
  const [modo, setModo] = useState('menu')

  // Al reabrir siempre se empieza por el menú, no donde se quedó la vez pasada.
  useEffect(() => {
    if (abierto) setModo('menu')
  }, [abierto])

  const unidas = pedido?.mesasUnidas ?? []
  const sinMesasLibres = mesasLibres.length === 0

  return (
    <Sheet open={abierto} onOpenChange={onOpenChange}>
      <SheetContent titulo={TITULOS[modo]}>
        {modo === 'menu' ? (
          <div className="flex flex-col gap-4 pb-2">
            <div className="flex flex-col gap-2">
              <p className="text-base font-semibold text-tinta">
                {unidas.length > 0
                  ? `Esta cuenta ocupa las mesas ${numerosDeMesa(pedido).join(', ')}.`
                  : `Esta cuenta está en la mesa ${pedido?.mesaNumero}.`}
              </p>

              {unidas.map((mesa) => (
                <div
                  key={mesa.id}
                  className="flex items-center gap-3 rounded-app border-2 border-borde bg-superficie px-4 py-3"
                >
                  <Link2 size={20} className="shrink-0 text-brasa-600" aria-hidden="true" />
                  <span className="grow text-lg font-bold text-carbon">Mesa {mesa.numero}</span>
                  <Button
                    variante="fantasma"
                    tamano="media"
                    disabled={guardando}
                    onClick={() => onSeparar(mesa.id)}
                  >
                    <Unlink size={20} />
                    Separar
                  </Button>
                </div>
              ))}
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <Button
                variante="secundaria"
                tamano="grande"
                disabled={sinMesasLibres || guardando}
                onClick={() => setModo('unir')}
              >
                <Link2 size={22} />
                Unir otra mesa
              </Button>
              <Button
                variante="secundaria"
                tamano="grande"
                disabled={sinMesasLibres || guardando}
                onClick={() => setModo('mover')}
              >
                <ArrowLeftRight size={22} />
                Cambiar de mesa
              </Button>
            </div>

            {sinMesasLibres ? (
              <p className="text-base text-tinta">
                No hay mesas libres ahora mismo. Cobra alguna para poder unirla o mudarte.
              </p>
            ) : null}
          </div>
        ) : (
          <div className="flex flex-col gap-4 pb-2">
            <p className="text-base text-tinta">
              {modo === 'mover'
                ? 'La cuenta pasa a la mesa que elijas y las que ocupa ahora quedan libres.'
                : 'La cuenta quedará ocupando las dos mesas y se paga junta.'}
            </p>

            <ul className="grid grid-cols-3 gap-3 sm:grid-cols-4">
              {mesasLibres.map((mesa) => (
                <li key={mesa.id}>
                  <CardBoton
                    disabled={guardando}
                    onClick={() => (modo === 'mover' ? onMover(mesa.id) : onUnir(mesa.id))}
                    className="flex min-h-20 items-center justify-center px-2 py-3"
                  >
                    <span className="text-3xl font-extrabold text-carbon">{mesa.numero}</span>
                  </CardBoton>
                </li>
              ))}
            </ul>

            <Button variante="secundaria" tamano="grande" onClick={() => setModo('menu')}>
              Volver
            </Button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}
