import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { Sheet, SheetPantalla } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Card, CardBoton } from '@/components/ui/card'
import { Switch } from '@/components/ui/switch'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { useCatalogo } from '@/hooks/useCatalogo'
import { calcularPrecioAnticucho } from '@/hooks/useCalculoPrecio'
import { formatoMoneda } from '@/utils/formatoMoneda'
import { contadorAnticucho } from '@/utils/etiquetas'
import { NOMBRES_ENVASE, TIPO_ITEM } from '@/utils/constantes'
import { cn } from '@/utils/cn'

/** Pedidos frecuentes que el mozo anota sin escribir. Se pueden combinar. */
const NOTAS_RAPIDAS = ['Papa aparte', 'Ensalada', 'Sin ají', 'Bien cocido']

/**
 * La pantalla más usada de la noche. Cada toque suma un palito y el precio
 * se recalcula al instante: ese feedback es lo que da confianza al mozo.
 */
export function ArmadorAnticucho({ abierto, onOpenChange, onAgregar, guardando }) {
  const { anticuchos, extras, cargando } = useCatalogo()
  const [seleccion, setSeleccion] = useState([])
  const [paraLlevar, setParaLlevar] = useState(false)
  const [envases, setEnvases] = useState([])
  const [nota, setNota] = useState('')

  useEffect(() => {
    if (!abierto) {
      setSeleccion([])
      setParaLlevar(false)
      setEnvases([])
      setNota('')
    }
  }, [abierto])

  const envasesDisponibles = extras.filter((extra) => NOMBRES_ENVASE.includes(extra.nombre))
  const precio = calcularPrecioAnticucho(seleccion)
  const precioEnvases = envases.reduce((suma, envase) => suma + Number(envase.precioUnitario), 0)
  const total = precio + precioEnvases

  function alternarEnvase(envase) {
    setEnvases((actuales) =>
      actuales.some((e) => e.id === envase.id)
        ? actuales.filter((e) => e.id !== envase.id)
        : [...actuales, envase],
    )
  }

  function alternarNotaRapida(texto) {
    setNota((actual) => {
      const partes = actual
        .split('·')
        .map((parte) => parte.trim())
        .filter(Boolean)
      if (partes.some((parte) => parte.toLowerCase() === texto.toLowerCase())) {
        return partes.filter((parte) => parte.toLowerCase() !== texto.toLowerCase()).join(' · ')
      }
      return [...partes, texto].join(' · ')
    })
  }

  function agregar() {
    const observaciones = nota.trim() || null
    const items = [
      {
        tipoItem: TIPO_ITEM.ANTICUCHO,
        componentes: seleccion.map((producto) => producto.id),
        cantidad: 1,
        paraLlevar,
        observaciones,
      },
      ...envases.map((envase) => ({
        tipoItem: TIPO_ITEM.EXTRA,
        componentes: [envase.id],
        cantidad: 1,
        paraLlevar,
      })),
    ]
    onAgregar(items)
  }

  return (
    <Sheet open={abierto} onOpenChange={onOpenChange}>
      <SheetPantalla
        titulo="Anticuchos"
        subtitulo={contadorAnticucho(seleccion.length)}
        ancho="ancho"
        pie={
          <Button
            tamano="grande"
            className="w-full"
            disabled={seleccion.length === 0 || guardando}
            onClick={agregar}
          >
            {guardando ? 'Agregando...' : `Agregar — ${formatoMoneda(total)}`}
          </Button>
        }
      >
        {cargando ? (
          <EstadoCarga filas={6} alto="h-20" />
        ) : (
          <div className="flex flex-col gap-6">
            {/* Lo elegido va arriba y se quita tocándolo: corregir es tan fácil como elegir. */}
            {seleccion.length > 0 ? (
              <Card className="bg-brasa-50/60 p-3">
                <p className="mb-2 text-sm font-bold uppercase tracking-wide text-brasa-700">
                  En este plato · toca para quitar
                </p>
                <ul className="flex flex-wrap gap-2">
                  {seleccion.map((producto, indice) => (
                    <li key={`${producto.id}-${indice}`}>
                      <button
                        type="button"
                        onClick={() => setSeleccion((actual) => actual.filter((_, i) => i !== indice))}
                        className="flex min-h-12 items-center gap-2 rounded-full border-2 border-brasa-500 bg-superficie pl-4 pr-3 text-base font-bold text-brasa-700 transition-colors hover:bg-brasa-100"
                      >
                        {producto.nombre}
                        <X size={20} aria-label={`Quitar ${producto.nombre}`} />
                      </button>
                    </li>
                  ))}
                </ul>
              </Card>
            ) : null}

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
              {anticuchos.map((producto) => (
                <CardBoton
                  key={producto.id}
                  onClick={() => setSeleccion((actual) => [...actual, producto])}
                  className="flex min-h-24 flex-col justify-center px-4 py-3"
                >
                  <span className="text-lg font-bold leading-tight text-carbon">
                    {producto.nombre}
                  </span>
                  <span className="monto mt-1 block text-base font-bold text-brasa-600">
                    {formatoMoneda(producto.precioUnitario)}
                  </span>
                </CardBoton>
              ))}
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="nota-anticucho" className="text-lg">
                Nota para cocina <span className="font-normal text-tinta">(opcional)</span>
              </Label>
              <div className="flex flex-wrap gap-2">
                {NOTAS_RAPIDAS.map((texto) => {
                  const activa = nota
                    .split('·')
                    .map((parte) => parte.trim().toLowerCase())
                    .includes(texto.toLowerCase())
                  return (
                    <button
                      key={texto}
                      type="button"
                      onClick={() => alternarNotaRapida(texto)}
                      className={cn(
                        'min-h-12 rounded-full border-2 px-4 text-base font-bold transition-colors',
                        activa
                          ? 'border-brasa-500 bg-brasa-50 text-brasa-700'
                          : 'border-borde bg-superficie text-carbon hover:border-borde-fuerte',
                      )}
                    >
                      {texto}
                    </button>
                  )
                })}
              </div>
              <Input
                id="nota-anticucho"
                value={nota}
                onChange={(evento) => setNota(evento.target.value.slice(0, 255))}
                placeholder="Ej: papa aparte, ensalada..."
                maxLength={255}
              />
            </div>

            <Card className="flex items-center justify-between gap-3 px-4 py-3">
              <Label htmlFor="para-llevar" className="text-lg">
                Para llevar
              </Label>
              <Switch id="para-llevar" checked={paraLlevar} onCheckedChange={setParaLlevar} />
            </Card>

            {paraLlevar && envasesDisponibles.length > 0 ? (
              <div className="flex flex-col gap-2">
                <p className="text-base font-bold text-carbon">¿Lleva envase?</p>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                  {envasesDisponibles.map((envase) => (
                    <CardBoton
                      key={envase.id}
                      seleccionada={envases.some((e) => e.id === envase.id)}
                      aria-pressed={envases.some((e) => e.id === envase.id)}
                      onClick={() => alternarEnvase(envase)}
                      className="flex min-h-20 flex-col justify-center px-4 py-2"
                    >
                      <span className="text-base font-bold text-carbon">{envase.nombre}</span>
                      <span className="monto text-sm font-bold text-tinta">
                        {formatoMoneda(envase.precioUnitario)}
                      </span>
                    </CardBoton>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        )}
      </SheetPantalla>
    </Sheet>
  )
}
