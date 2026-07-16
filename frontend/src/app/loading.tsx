import { Skeleton } from '@/components/ui/skeleton'
export default function Loading() {
  return <div className="container mx-auto px-4 py-8 space-y-4"><Skeleton className="h-12 w-1/3" /><Skeleton className="h-64 w-full rounded-2xl" /><div className="grid md:grid-cols-3 gap-4"><Skeleton className="h-32 rounded-2xl" /><Skeleton className="h-32 rounded-2xl" /><Skeleton className="h-32 rounded-2xl" /></div></div>
}
