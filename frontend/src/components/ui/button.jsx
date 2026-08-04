import { Slot } from '@radix-ui/react-slot'
import { cva } from 'class-variance-authority'
import { cn } from '@/utils/cn'

// El foco lo dibuja :focus-visible global; acá solo el reposo, el hover y el toque.
const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-app font-bold select-none ' +
    'transition-all duration-150 active:scale-[0.97] ' +
    'disabled:opacity-40 disabled:pointer-events-none',
  {
    variants: {
      variante: {
        principal: 'bg-brasa-600 text-white shadow-brasa hover:bg-brasa-500 active:bg-brasa-700',
        secundaria:
          'bg-superficie text-carbon border-2 border-borde shadow-suave hover:border-borde-fuerte hover:bg-fondo active:bg-hundido',
        suave: 'bg-brasa-50 text-brasa-700 border-2 border-brasa-100 hover:bg-brasa-100',
        fantasma: 'bg-transparent text-tinta hover:bg-hundido',
        destructiva:
          'bg-superficie text-alerta border-2 border-alerta/30 shadow-suave hover:bg-alerta-suave hover:border-alerta/50',
      },
      tamano: {
        grande: 'min-h-16 px-6 text-lg',
        media: 'min-h-12 px-4 text-base',
        icono: 'min-h-12 min-w-12 p-0',
      },
    },
    defaultVariants: { variante: 'principal', tamano: 'media' },
  },
)

export function Button({ className, variante, tamano, asChild = false, ...props }) {
  const Comp = asChild ? Slot : 'button'
  return (
    <Comp
      className={cn(buttonVariants({ variante, tamano }), className)}
      type={asChild ? undefined : (props.type ?? 'button')}
      {...props}
    />
  )
}
