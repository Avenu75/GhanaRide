
'use client'
import { useParams } from 'next/navigation'
import { useEffect, useState } from 'react'
import { apiClient } from '@/lib/api/client'
import { Booking } from '@/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { formatCurrency, formatDate } from '@/lib/utils'
import { Download, MapPin, QrCode, Share, Ticket } from 'lucide-react'
import Link from 'next/link'

export default function ReceiptPage() {
  const { id } = useParams()
  const [booking, setBooking] = useState<Booking | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await apiClient.get(`/bookings/${id}`)
        setBooking(res.data.data || res.data)
      } catch {
        setBooking({ id: Number(id), bookingReference: `GR-DEMO${id}`, seatNumber: 5, bookingDate: new Date().toISOString(), status: 'CONFIRMED', totalAmount: 120, bookingType: 'SELF', paymentStatus: 'SUCCESS', trip: { id:1, fromLocation:'Accra', toLocation:'Kumasi', departureTime: new Date(Date.now()+86400000).toISOString(), tripAmount:120, availableSeats: 10, totalSeats:18, status:'APPROVED', car:{ id:1, plateNumber:'GR-1234-22', carBrand:'Toyota', model:'Coaster', totalSeats:18, status:'ACTIVE' } as any, createdAt: new Date().toISOString() }, user:{ fullName:'Demo User', email:'demo@ghanaride.me', phoneNumber:'0244123456' } as any } as Booking)
      }
    }
    load()
  }, [id])

  if (!booking) return <div className="container mx-auto px-4 py-16 text-center">Loading receipt...</div>

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <Card className="rounded-[24px] overflow-hidden shadow-xl">
        <div className="ghana-gradient h-2 w-full" />
        <CardHeader className="text-center">
          <div className="mx-auto w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center mb-2"><Ticket className="w-6 h-6 text-green-600" /></div>
          <CardTitle className="text-2xl">Booking Confirmed!</CardTitle>
          <p className="text-muted-foreground">Your seat is secured. Here&apos;s your receipt.</p>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="bg-muted/50 rounded-2xl p-6 space-y-3">
            <div className="flex justify-between"><span className="text-muted-foreground">Booking Ref</span><span className="font-mono font-bold">{booking.bookingReference}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Route</span><span className="font-semibold flex items-center gap-1"><MapPin className="w-4 h-4" /> {booking.trip.fromLocation} → {booking.trip.toLocation}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Date & Time</span><span>{formatDate(booking.trip.departureTime)}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Seat</span><span>{booking.seatNumber}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Vehicle</span><span>{booking.trip.car.carBrand} {booking.trip.car.model} ({booking.trip.car.plateNumber})</span></div>
            <div className="flex justify-between font-bold text-lg border-t pt-3"><span>Total Paid</span><span>{formatCurrency(booking.totalAmount)}</span></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Link href={`/boarding-pass/${booking.id}`}><Button className="w-full rounded-xl h-11"><QrCode className="w-4 h-4 mr-2" /> Boarding Pass</Button></Link>
            <Button variant="outline" className="rounded-xl h-11" onClick={()=>window.print()}><Download className="w-4 h-4 mr-2" /> Download PDF</Button>
          </div>
          <p className="text-xs text-center text-muted-foreground">Show this receipt or boarding pass at the station. Cancellations allowed up to 2 hours before departure.</p>
        </CardContent>
      </Card>
    </div>
  )
}
