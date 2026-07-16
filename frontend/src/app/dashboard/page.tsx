
'use client'
import { useEffect, useState } from 'react'
import { apiClient } from '@/lib/api/client'
import { Trip } from '@/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { formatCurrency, formatDate } from '@/lib/utils'
import { MapPin, Clock, Ticket, Wallet, Bell } from 'lucide-react'
import Link from 'next/link'
import { useAuthStore } from '@/store/auth.store'

export default function DashboardPage() {
  const { user } = useAuthStore()
  const [trips, setTrips] = useState<Trip[]>([])
  const [stats, setStats] = useState({ bookings: 3, upcoming: 1, wallet: 245.5 })

  useEffect(() => {
    const load = async () => {
      try {
        const res = await apiClient.get('/trips?status=APPROVED&size=3')
        setTrips(res.data.data?.content || res.data.content || [])
      } catch {
        setTrips([])
      }
    }
    load()
  }, [])

  return (
    <div className="container mx-auto px-4 py-8 space-y-8">
      <div className="flex justify-between items-center">
        <div><h1 className="text-3xl font-bold">Welcome, {user?.fullName?.split(' ')[0] || 'Traveler'} 👋</h1><p className="text-muted-foreground">Where would you like to go today?</p></div>
        <Link href="/rides"><Button className="rounded-full hidden md:flex">Find Trips</Button></Link>
      </div>

      <div className="grid md:grid-cols-3 gap-4">
        <Card className="rounded-2xl"><CardContent className="p-6 flex items-center gap-4"><div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center"><Ticket className="w-6 h-6 text-blue-600" /></div><div><div className="text-2xl font-bold">{stats.bookings}</div><div className="text-sm text-muted-foreground">Total Bookings</div></div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6 flex items-center gap-4"><div className="w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center"><Wallet className="w-6 h-6 text-green-600" /></div><div><div className="text-2xl font-bold">GH₵{stats.wallet}</div><div className="text-sm text-muted-foreground">Wallet Balance</div></div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6 flex items-center gap-4"><div className="w-12 h-12 rounded-xl bg-amber-100 flex items-center justify-center"><Bell className="w-6 h-6 text-amber-600" /></div><div><div className="text-2xl font-bold">{stats.upcoming}</div><div className="text-sm text-muted-foreground">Upcoming Trip</div></div></CardContent></Card>
      </div>

      <Card className="rounded-[20px]"><CardHeader><CardTitle>Quick Actions</CardTitle></CardHeader><CardContent className="grid md:grid-cols-4 gap-3">
        <Link href="/rides"><Button variant="outline" className="w-full rounded-xl h-20 flex-col"><MapPin className="w-6 h-6 mb-1" /> Search Trips</Button></Link>
        <Link href="/my-bookings"><Button variant="outline" className="w-full rounded-xl h-20 flex-col"><Ticket className="w-6 h-6 mb-1" /> My Bookings</Button></Link>
        <Link href="/wallet"><Button variant="outline" className="w-full rounded-xl h-20 flex-col"><Wallet className="w-6 h-6 mb-1" /> Wallet</Button></Link>
        <Link href="/profile"><Button variant="outline" className="w-full rounded-xl h-20 flex-col"><Clock className="w-6 h-6 mb-1" /> Profile</Button></Link>
      </CardContent></Card>
    </div>
  )
}
