
'use client'
import { useEffect, useState } from 'react'
import { apiClient } from '@/lib/api/client'
import { Booking } from '@/types'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, formatDate } from '@/lib/utils'
import { BOOKING_STATUS_COLORS } from '@/lib/constants'
import { MapPin, Ticket, Clock, Download, XCircle } from 'lucide-react'
import Link from 'next/link'
import { toast } from 'sonner'

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await apiClient.get('/bookings/my')
        setBookings(res.data.data?.content || res.data.content || res.data || [])
      } catch {
        setBookings([
          { id: 101, bookingReference: 'GR-AB12CD', seatNumber: 5, bookingDate: new Date().toISOString(), status: 'CONFIRMED', totalAmount: 120, bookingType: 'SELF', paymentStatus: 'SUCCESS', trip: { id:1, fromLocation:'Accra', toLocation:'Kumasi', departureTime: new Date(Date.now()+86400000).toISOString(), tripAmount:120, availableSeats:10, totalSeats:18, status:'APPROVED', car:{ id:1, plateNumber:'GR-1234', carBrand:'Toyota', model:'Coaster', totalSeats:18, status:'ACTIVE' } as any, createdAt: new Date().toISOString() }, user:{} as any } as Booking,
          { id: 102, bookingReference: 'GR-EF34GH', seatNumber: 8, bookingDate: new Date(Date.now()-86400000*2).toISOString(), status: 'COMPLETED', totalAmount: 80, bookingType: 'SELF', paymentStatus: 'SUCCESS', trip: { id:2, fromLocation:'Accra', toLocation:'Cape Coast', departureTime: new Date(Date.now()-86400000).toISOString(), tripAmount:80, availableSeats:5, totalSeats:18, status:'APPROVED', car:{ id:2, plateNumber:'GR-5678', carBrand:'Nissan', model:'Civilian', totalSeats:18, status:'ACTIVE' } as any, createdAt: new Date().toISOString() }, user:{} as any } as Booking,
        ])
      } finally { setLoading(false) }
    }
    load()
  }, [])

  const cancelBooking = async (id:number) => {
    if (!confirm('Cancel this booking? Seats will be released.')) return
    try {
      await apiClient.post(`/bookings/${id}/cancel`)
      toast.success('Booking cancelled')
      setBookings(b=>b.map(x=>x.id===id ? {...x, status:'CANCELLED' as any} : x))
    } catch (e:any) { toast.error(e.response?.data?.message || 'Cancel failed') }
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6"><h1 className="text-3xl font-bold">My Bookings</h1><Link href="/rides"><Button variant="outline" className="rounded-full">Find More Trips</Button></Link></div>
      <div className="space-y-4">
        {bookings.map(b=>(
          <Card key={b.id} className="rounded-[20px] overflow-hidden">
            <CardContent className="p-6">
              <div className="flex flex-col md:flex-row gap-6">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2"><span className="font-mono font-bold">{b.bookingReference}</span><Badge className={BOOKING_STATUS_COLORS[b.status] || ''}>{b.status}</Badge><span className="text-sm text-muted-foreground">Seat {b.seatNumber || b.displaySeat || '-'}</span></div>
                  <div className="flex items-center gap-2 font-semibold"><MapPin className="w-4 h-4 text-primary" /> {b.trip.fromLocation} → {b.trip.toLocation}</div>
                  <div className="text-sm text-muted-foreground flex items-center gap-4 mt-1"><span className="flex items-center gap-1"><Clock className="w-4 h-4" /> {formatDate(b.trip.departureTime)}</span><span>{b.trip.car.carBrand} {b.trip.car.model} • {b.trip.car.plateNumber}</span></div>
                </div>
                <div className="md:w-64 flex flex-col gap-2">
                  <div className="text-xl font-bold">{formatCurrency(b.totalAmount)}</div>
                  <div className="flex gap-2">
                    <Link href={`/booking/receipt/${b.id}`} className="flex-1"><Button variant="outline" size="sm" className="w-full rounded-xl"><Download className="w-4 h-4 mr-1" /> Receipt</Button></Link>
                    <Link href={`/boarding-pass/${b.id}`} className="flex-1"><Button variant="outline" size="sm" className="w-full rounded-xl"><Ticket className="w-4 h-4 mr-1" /> Pass</Button></Link>
                  </div>
                  {(b.status==='ACTIVE' || b.status==='CONFIRMED' || b.status==='PENDING_PAYMENT') && <Button variant="ghost" size="sm" onClick={()=>cancelBooking(b.id)} className="text-destructive hover:bg-destructive/10 rounded-xl"><XCircle className="w-4 h-4 mr-1" /> Cancel</Button>}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
