
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { TRIP_STATUS_COLORS } from '@/lib/constants'

export default function AdminTrips() {
  const trips = [{ id:1, from:'Accra', to:'Kumasi', driver:'Kwame', status:'PENDING', amount:120 }, { id:2, from:'Accra', to:'Takoradi', driver:'Ama Company', status:'APPROVED', amount:150 }]
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Manage Trips</h1>
      <Card className="rounded-[20px]"><CardHeader><CardTitle>All Trips</CardTitle></CardHeader><CardContent className="space-y-2">
        {trips.map(t=>(
          <div key={t.id} className="flex justify-between items-center p-3 rounded-xl bg-muted/50"><div><div className="font-medium">{t.from} → {t.to} • {t.driver}</div><div className="text-xs text-muted-foreground">GH₵{t.amount} • <Badge className={TRIP_STATUS_COLORS[t.status]}>{t.status}</Badge></div></div><div className="flex gap-2"><Button size="sm" className="rounded-full bg-green-600">Approve</Button><Button size="sm" variant="outline" className="rounded-full">Reject</Button></div></div>
        ))}
      </CardContent></Card>
    </div>
  )
}
