import { LucideIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import Link from 'next/link'

export function EmptyState({ icon: Icon, title, description, actionLabel, actionHref }: { icon?: LucideIcon; title: string; description?: string; actionLabel?: string; actionHref?: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6 text-center border border-dashed rounded-2xl bg-muted/30">
      {Icon && <div className="w-16 h-16 rounded-2xl bg-muted flex items-center justify-center mb-4"><Icon className="w-8 h-8 text-muted-foreground" /></div>}
      <h3 className="text-lg font-semibold">{title}</h3>
      {description && <p className="text-sm text-muted-foreground mt-2 max-w-sm">{description}</p>}
      {actionLabel && actionHref && <Link href={actionHref} className="mt-6"><Button>{actionLabel}</Button></Link>}
    </div>
  )
}
