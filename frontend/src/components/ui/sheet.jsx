import * as DialogPrimitive from '@radix-ui/react-dialog'
import { ChevronLeft, X } from 'lucide-react'
import { DialogOverlay } from './dialog'
import { cn } from '@/utils/cn'

export const Sheet = DialogPrimitive.Root
export const SheetTrigger = DialogPrimitive.Trigger
export const SheetClose = DialogPrimitive.Close

/**
 * En el celular sube desde abajo, donde llega el pulgar. En pantalla grande esa
 * franja pegada al borde inferior se ve rara y queda lejos de la vista, así que
 * se convierte en una tarjeta centrada.
 */
export function SheetContent({ className, children, titulo, ...props }) {
  return (
    <DialogPrimitive.Portal>
      <DialogOverlay />
      <DialogPrimitive.Content
        className={cn(
          'fixed z-50 flex max-h-[90vh] flex-col bg-superficie shadow-alta',
          'inset-x-0 bottom-0 rounded-t-app-lg border-t border-borde',
          'pb-[max(1rem,env(safe-area-inset-bottom))] pt-2',
          'sm:inset-x-auto sm:bottom-auto sm:left-1/2 sm:top-1/2 sm:w-[min(32rem,calc(100vw-3rem))]',
          'sm:-translate-x-1/2 sm:-translate-y-1/2 sm:rounded-app-lg sm:border sm:pb-5 sm:pt-4',
          'data-[state=open]:animate-subir sm:data-[state=open]:animate-emerger',
          className,
        )}
        {...props}
      >
        <div className="mx-auto mb-3 h-1.5 w-12 shrink-0 rounded-full bg-borde sm:hidden" />

        <div className="flex shrink-0 items-center justify-between gap-3 px-4 pb-3 sm:px-6">
          <DialogPrimitive.Title className="text-xl font-bold text-carbon sm:text-2xl">
            {titulo}
          </DialogPrimitive.Title>
          <DialogPrimitive.Close
            aria-label="Cerrar"
            className="-mr-2 flex min-h-12 min-w-12 shrink-0 items-center justify-center rounded-app text-tinta transition-colors hover:bg-hundido hover:text-carbon"
          >
            <X size={24} />
          </DialogPrimitive.Close>
        </div>

        <DialogPrimitive.Description className="sr-only">{titulo}</DialogPrimitive.Description>

        <div className="min-h-0 grow overflow-y-auto overscroll-contain px-4 sm:px-6">
          {children}
        </div>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}

/**
 * Pantalla completa con cabecera y pie fijos, para los armadores: el botón de
 * agregar tiene que estar siempre visible sin hacer scroll. En escritorio se
 * encoge a una ventana grande para no perder de vista dónde estabas.
 */
export function SheetPantalla({ titulo, subtitulo, pie, onVolver, ancho = 'normal', children }) {
  return (
    <DialogPrimitive.Portal>
      <DialogOverlay />
      <DialogPrimitive.Content
        className={cn(
          'fixed inset-0 z-50 flex flex-col bg-fondo',
          'lg:inset-auto lg:left-1/2 lg:top-1/2 lg:h-[88vh] lg:-translate-x-1/2 lg:-translate-y-1/2',
          'lg:overflow-hidden lg:rounded-app-lg lg:border lg:border-borde lg:shadow-alta',
          ancho === 'ancho'
            ? 'lg:w-[min(64rem,calc(100vw-4rem))]'
            : 'lg:w-[min(48rem,calc(100vw-4rem))]',
          'data-[state=open]:animate-subir lg:data-[state=open]:animate-emerger',
        )}
      >
        <header className="flex shrink-0 items-center gap-2 border-b border-borde bg-superficie px-2 py-2 pt-[max(0.5rem,env(safe-area-inset-top))] lg:px-4 lg:py-3 lg:pt-3">
          {onVolver ? (
            <button
              type="button"
              onClick={onVolver}
              aria-label="Volver"
              className="flex min-h-12 min-w-12 items-center justify-center rounded-app text-carbon transition-colors hover:bg-hundido"
            >
              <ChevronLeft size={28} />
            </button>
          ) : (
            <DialogPrimitive.Close
              aria-label="Volver"
              className="flex min-h-12 min-w-12 items-center justify-center rounded-app text-carbon transition-colors hover:bg-hundido"
            >
              <ChevronLeft size={28} />
            </DialogPrimitive.Close>
          )}
          <div className="min-w-0">
            <DialogPrimitive.Title className="truncate text-xl font-bold text-carbon lg:text-2xl">
              {titulo}
            </DialogPrimitive.Title>
            {subtitulo ? (
              <p className="truncate text-sm font-medium text-brasa-600 lg:text-base">{subtitulo}</p>
            ) : null}
          </div>
        </header>

        <DialogPrimitive.Description className="sr-only">{titulo}</DialogPrimitive.Description>

        <div className="min-h-0 grow overflow-y-auto overscroll-contain p-4 lg:p-6">
          <div className="mx-auto max-w-3xl">{children}</div>
        </div>

        {pie ? (
          <footer className="shrink-0 border-t border-borde bg-superficie p-4 pb-[max(1rem,env(safe-area-inset-bottom))] shadow-[0_-4px_16px_rgb(28_22_19/0.06)] lg:pb-4">
            <div className="mx-auto max-w-3xl">{pie}</div>
          </footer>
        ) : null}
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}
