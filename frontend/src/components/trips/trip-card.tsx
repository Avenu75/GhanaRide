
import { Trip } from '@/types'
import { Card, CardContent } from '@/components/ui/card'
import { formatCurrency, formatDate } from '@/lib/utils'
import { MapPin, Clock, Bus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import Link from 'next/link'

export function TripCard({ trip }: { trip: Trip }) {
  return (
    <Card className="rounded-[20px] hover:shadow-md transition">
      <CardContent className="p-5">
        <div className="flex justify-between"><div className="font-bold flex items-center gap-2"><MapPin className="w-4 h-4 text-primary" /> {trip.fromLocation} → {trip.toLocation}</div><div className="font-bold">{formatCurrency(trip.tripAmount)}</div></div>
        <div className="text-sm text-muted-foreground mt-1 flex gap-3"><span className="flex items-center gap-1"><Clock className="w-4 h-4" /> {formatDate(trip.departureTime)}</span><span className="flex items-center gap-1"><Bus className="w-4 h-4" /> {trip.car.carBrand} {trip.car.plateNumber}</span></div>
        <div className="mt-4 flex gap-2"><Link href={`/booking/${trip.id}`} className="flex-1"><Button className="w-full rounded-xl">Book Now</Button></Link></div>
      </CardContent>
    </Card>
  )
}
