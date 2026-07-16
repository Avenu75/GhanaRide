'use client'
import { useParams } from 'next/navigation'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

export default function TripPassengersPage() {
  const { id } = useParams()
  const passengers = [
    { id:1, name:'Kwame Asante', phone:'0244123456', seat:'5', bookingRef:'GR-AB12CD', status:'CONFIRMED' },
    { id:2, name:'Ama Serwaa', phone:'0244987654', seat:'8', bookingRef:'GR-EF34GH', status:'CONFIRMED' },
  ]
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Passengers • Trip #{id}</h1>
      <Card className="rounded-[20px]"><CardHeader><CardTitle>{passengers.length} Passengers</CardTitle></CardHeader><CardContent className="space-y-2">
        {passengers.map(p=>(
          <div key={p.id} className="flex justify-between items-center p-3 rounded-xl bg-muted/50"><div><div className="font-medium">{p.name} • Seat {p.seat}</div><div className="text-xs text-muted-foreground">{p.phone} • {p.bookingRef} • {p.status}</div></div><Button size="sm" variant="outline" className="rounded-full">Contact</Button></div>
        ))}
      </CardContent></Card>
    </div>
  )
}
