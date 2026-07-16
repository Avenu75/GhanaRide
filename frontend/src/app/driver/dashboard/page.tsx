
'use client'
import { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Trip } from '@/types'
import { Bus, Users, DollarSign, Clock } from 'lucide-react'
import { formatCurrency, formatDate } from '@/lib/utils'
import Link from 'next/link'

export default function DriverDashboard() {
  const [trips, setTrips] = useState<Trip[]>([
    { id:1, fromLocation:'Accra', toLocation:'Kumasi', departureTime: new Date(Date.now()+3600000*2).toISOString(), tripAmount:120, availableSeats:5, totalSeats:18, status:'APPROVED', car:{ id:1, plateNumber:'GR-1234', carBrand:'Toyota', model:'Coaster', totalSeats:18, status:'ACTIVE' } as any, createdAt: new Date().toISOString(), bookingsCount:13 },
  ])
  const stats = { todayTrips:2, totalEarnings: 2450, totalPassengers: 156, pendingApproval:1 }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6"><h1 className="text-3xl font-bold">Driver Dashboard</h1><Link href="/driver/add-trip"><Button className="rounded-full">+ Add Trip</Button></Link></div>
      <div className="grid md:grid-cols-4 gap-4 mb-8">
        <Card className="rounded-2xl"><CardContent className="p-6"><div className="flex justify-between"><div><div className="text-sm text-muted-foreground">Today Trips</div><div className="text-2xl font-bold">{stats.todayTrips}</div></div><Bus className="w-8 h-8 text-muted-foreground" /></div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><div className="flex justify-between"><div><div className="text-sm text-muted-foreground">Earnings</div><div className="text-2xl font-bold">{formatCurrency(stats.totalEarnings)}</div></div><DollarSign className="w-8 h-8 text-green-500" /></div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><div className="flex justify-between"><div><div className="text-sm text-muted-foreground">Passengers</div><div className="text-2xl font-bold">{stats.totalPassengers}</div></div><Users className="w-8 h-8 text-blue-500" /></div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><div className="flex justify-between"><div><div className="text-sm text-muted-foreground">Pending</div><div className="text-2xl font-bold">{stats.pendingApproval}</div></div><Clock className="w-8 h-8 text-amber-500" /></div></CardContent></Card>
      </div>
      <Card className="rounded-[20px]"><CardHeader><CardTitle>My Trips</CardTitle></CardHeader><CardContent className="space-y-3">
        {trips.map(t=>(
          <div key={t.id} className="flex justify-between items-center p-4 rounded-xl bg-muted/50"><div><div className="font-semibold">{t.fromLocation} → {t.toLocation}</div><div className="text-sm text-muted-foreground">{formatDate(t.departureTime)} • {t.bookingsCount}/{t.totalSeats} booked • {formatCurrency(t.tripAmount)}</div></div><div className="flex gap-2"><Link href={`/driver/dashboard`}><Button size="sm" variant="outline" className="rounded-full">Passengers</Button></Link><Button size="sm" variant="outline" className="rounded-full">Manage</Button></div></div>
        ))}
      </CardContent></Card>
    </div>
  )
}
