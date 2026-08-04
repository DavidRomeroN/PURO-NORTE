import { Button } from '@/components/ui/button'

/** Un estado vacío es una invitación a actuar, no un mensaje de error. */
export function EstadoVacio({ icono: Icono, titulo, descripcion, accion, onAccion }) {
  return (
    <div className="flex flex-col items-center gap-4 rounded-app-lg border border-dashed border-borde-fuerte bg-superficie px-6 py-12 text-center">
      {Icono ? (
        <span className="flex size-16 items-center justify-center rounded-full bg-brasa-50 text-brasa-500">
          <Icono size={32} />
        </span>
      ) : null}
      <div>
        <p className="text-xl font-bold text-carbon">{titulo}</p>
        {descripcion ? <p className="mt-1 text-base text-tinta">{descripcion}</p> : null}
      </div>
      {accion ? (
        <Button tamano="grande" className="w-full max-w-xs" onClick={onAccion}>
          {accion}
        </Button>
      ) : null}
    </div>
  )
}
