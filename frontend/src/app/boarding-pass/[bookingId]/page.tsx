
'use client'
import { useParams } from 'next/navigation'
import { useEffect, useState } from 'react'
import { Booking } from '@/types'
import { Card, CardContent } from '@/components/ui/card'
import { formatDate } from '@/lib/utils'
import { QrCode, MapPin, Bus, Clock, User } from 'lucide-react'

export default function BoardingPassPage() {
  const { bookingId } = useParams()
  const [booking, setBooking] = useState<Booking | null>(null)
  useEffect(()=>{
    setBooking({ id: Number(bookingId), bookingReference: `GR-${bookingId}XYZ`, seatNumber: 7, bookingDate: new Date().toISOString(), status: 'CONFIRMED', totalAmount: 120, bookingType:'SELF', paymentStatus:'SUCCESS', trip:{ id:1, fromLocation:'Accra', toLocation:'Kumasi', departureTime: new Date(Date.now()+86400000).toISOString(), tripAmount:120, availableSeats:10, totalSeats:18, status:'APPROVED', car:{ id:1, plateNumber:'GR-1234-22', carBrand:'Toyota', model:'Coaster', totalSeats:18, status:'ACTIVE' } as any, createdAt: new Date().toISOString() }, user:{ fullName:'John Doe', email:'john@example.com' } as any } as Booking)
  },[])
  if(!booking) return null
  return (
    <div className="min-h-[80vh] flex items-center justify-center p-4 bg-zinc-900">
      <div className="w-full max-w-sm bg-black text-white rounded-[32px] p-8 relative overflow-hidden shadow-2xl">
        <div className="absolute -top-20 -right-20 w-60 h-60 bg-yellow-500/20 rounded-full blur-3xl" />
        <div className="relative">
          <div className="flex justify-between items-center mb-8"><div className="flex items-center gap-2 font-bold"><Bus className="w-5 h-5 text-primary" /> GhanaRide</div><div className="text-xs border border-white/20 rounded-full px-3 py-1">BOARDING PASS</div></div>
          <div className="text-center mb-6"><div className="text-3xl font-mono font-bold tracking-wider">{booking.bookingReference}</div><div className="text-xs text-zinc-400 mt-1">Present at boarding gate</div></div>
          <div className="bg-white text-black rounded-2xl p-6 space-y-4">
            <div className="flex justify-between"><div><div className="text-xs text-zinc-500">FROM</div><div className="font-bold text-lg">{booking.trip.fromLocation}</div></div><div className="text-right"><div className="text-xs text-zinc-500">TO</div><div className="font-bold text-lg">{booking.trip.toLocation}</div></div></div>
            <div className="grid grid-cols-2 gap-4 text-sm"><div><div className="text-xs text-zinc-500 flex items-center gap-1"><Clock className="w-3 h-3" /> DEPARTURE</div><div className="font-semibold">{formatDate(booking.trip.departureTime,'short')}</div></div><div><div className="text-xs text-zinc-500 flex items-center gap-1"><Bus className="w-3 h-3" /> SEAT</div><div className="font-bold text-xl">{booking.seatNumber}</div></div></div>
            <div className="flex justify-center py-4"><div className="w-40 h-40 bg-zinc-900 rounded-xl flex items-center justify-center"><QrCode className="w-24 h-24 text-white" /></div></div>
            <div className="text-center text-xs text-zinc-500">Scan at station • Vehicle: {booking.trip.car.plateNumber}</div>
          </div>
          <div className="mt-6 text-center text-xs text-zinc-500">Safe travels across Ghana 🇬🇭 • support@ghanaride.me</div>
        </div>
      </div>
    </div>
  )
}
