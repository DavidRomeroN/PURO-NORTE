import { Dialog, DialogContent } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

export function ConfirmDialog({
  abierto,
  onOpenChange,
  titulo,
  descripcion,
  textoConfirmar = 'Sí, continuar',
  textoCancelar = 'Cancelar',
  destructivo = false,
  enProceso = false,
  onConfirmar,
}) {
  return (
    <Dialog open={abierto} onOpenChange={onOpenChange}>
      <DialogContent titulo={titulo} descripcion={descripcion}>
        {/* La acción destructiva va abajo y separada, para no tocarla por error. */}
        <div className="mt-2 flex flex-col gap-3">
          <Button variante="secundaria" tamano="grande" onClick={() => onOpenChange(false)}>
            {textoCancelar}
          </Button>
          <Button
            variante={destructivo ? 'destructiva' : 'principal'}
            tamano="grande"
            disabled={enProceso}
            onClick={onConfirmar}
          >
            {enProceso ? 'Un momento...' : textoConfirmar}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
