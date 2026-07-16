import { Skeleton } from '@/components/ui/skeleton'

export function LoadingTrips() {
  return (
    <div className="grid gap-4">
      {[1,2,3].map(i => (
        <div key={i} className="border rounded-2xl p-5 space-y-3">
          <Skeleton className="h-6 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
          <div className="flex gap-2"><Skeleton className="h-8 w-20" /><Skeleton className="h-8 w-24" /></div>
        </div>
      ))}
    </div>
  )
}

export function LoadingPage() {
  return (
    <div className="container mx-auto px-4 py-8 space-y-6">
      <Skeleton className="h-12 w-1/3" />
      <Skeleton className="h-64 w-full" />
    </div>
  )
}
