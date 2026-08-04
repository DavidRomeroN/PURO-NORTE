import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import { cn } from '@/utils/cn'

export const Dialog = DialogPrimitive.Root
export const DialogTrigger = DialogPrimitive.Trigger
export const DialogClose = DialogPrimitive.Close

export function DialogOverlay({ className }) {
  return (
    <DialogPrimitive.Overlay
      className={cn(
        'fixed inset-0 z-40 bg-carbon/55 backdrop-blur-[2px]',
        'data-[state=open]:animate-aparecer data-[state=closed]:animate-desvanecer',
        className,
      )}
    />
  )
}

export function DialogContent({ className, children, titulo, descripcion, ...props }) {
  return (
    <DialogPrimitive.Portal>
      <DialogOverlay />
      <DialogPrimitive.Content
        className={cn(
          'fixed left-1/2 top-1/2 z-50 w-[min(30rem,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2',
          'max-h-[90vh] overflow-y-auto overscroll-contain',
          'rounded-app-lg border border-borde bg-superficie p-5 shadow-alta sm:p-6',
          'data-[state=open]:animate-emerger',
          className,
        )}
        {...props}
      >
        <div className="mb-5 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <DialogPrimitive.Title className="text-xl font-bold text-carbon sm:text-2xl">
              {titulo}
            </DialogPrimitive.Title>
            {descripcion ? (
              <DialogPrimitive.Description className="mt-1.5 text-base text-tinta">
                {descripcion}
              </DialogPrimitive.Description>
            ) : (
              <DialogPrimitive.Description className="sr-only">{titulo}</DialogPrimitive.Description>
            )}
          </div>
          <DialogPrimitive.Close
            aria-label="Cerrar"
            className="-mr-2 -mt-2 flex min-h-12 min-w-12 shrink-0 items-center justify-center rounded-app text-tinta transition-colors hover:bg-hundido hover:text-carbon"
          >
            <X size={24} />
          </DialogPrimitive.Close>
        </div>
        {children}
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}
