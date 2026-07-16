'use client'
import { Button } from '@/components/ui/button'
export default function Error({ error, reset }: { error: Error; reset: () => void }) {
  return <div className="container mx-auto px-4 py-20 text-center"><h2 className="text-2xl font-bold mb-4">Something went wrong</h2><p className="text-muted-foreground mb-6">{error.message}</p><Button onClick={reset} className="rounded-full">Try Again</Button></div>
}
