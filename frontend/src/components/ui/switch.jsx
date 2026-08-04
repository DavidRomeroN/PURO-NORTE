import * as SwitchPrimitive from '@radix-ui/react-switch'
import { cn } from '@/utils/cn'

export function Switch({ className, ...props }) {
  return (
    <SwitchPrimitive.Root
      className={cn(
        'relative inline-flex h-9 w-16 shrink-0 items-center rounded-full transition-colors duration-200',
        // El interruptor mide 36px de alto; el pseudo-elemento lleva el área táctil a 52px.
        "before:absolute before:-inset-2 before:content-['']",
        'data-[state=checked]:bg-brasa-600 data-[state=unchecked]:bg-borde-fuerte',
        'disabled:opacity-40',
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        className={cn(
          'block h-7 w-7 rounded-full bg-white shadow-media transition-transform duration-200',
          'data-[state=checked]:translate-x-8 data-[state=unchecked]:translate-x-1',
        )}
      />
    </SwitchPrimitive.Root>
  )
}
