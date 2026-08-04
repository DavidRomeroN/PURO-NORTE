import { cn } from '@/utils/cn'

export function Skeleton({ className }) {
  return <div className={cn('animate-brillo rounded-app bg-hundido', className)} />
}
