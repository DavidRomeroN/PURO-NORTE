import { cn } from '@/utils/cn'

export function Input({ className, ...props }) {
  return (
    <input
      className={cn(
        'min-h-14 w-full rounded-app border-2 border-borde bg-superficie px-4 text-lg text-carbon',
        'shadow-suave transition-all placeholder:text-tinta/50',
        'hover:border-borde-fuerte',
        'focus:border-brasa-500 focus:outline-none focus:ring-4 focus:ring-brasa-500/15',
        'disabled:bg-hundido disabled:opacity-60',
        className,
      )}
      {...props}
    />
  )
}
