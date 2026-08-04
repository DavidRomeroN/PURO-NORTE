import * as LabelPrimitive from '@radix-ui/react-label'
import { cn } from '@/utils/cn'

export function Label({ className, ...props }) {
  return (
    <LabelPrimitive.Root
      className={cn('block text-base font-semibold text-carbon', className)}
      {...props}
    />
  )
}
