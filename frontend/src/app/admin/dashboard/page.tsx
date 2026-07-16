
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Users, Bus, Ticket, DollarSign, TrendingUp } from 'lucide-react'
import { formatCurrency } from '@/lib/utils'

export default function AdminDashboard() {
  const stats = { users: 1250, trips: 342, bookings: 5890, revenue: 452000, pendingTrips: 12 }
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Admin Dashboard</h1>
      <div className="grid md:grid-cols-5 gap-4 mb-8">
        <Card className="rounded-2xl"><CardContent className="p-6"><Users className="w-6 h-6 text-blue-500 mb-2" /><div className="text-2xl font-bold">{stats.users}</div><div className="text-xs text-muted-foreground">Users</div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><Bus className="w-6 h-6 text-primary mb-2" /><div className="text-2xl font-bold">{stats.trips}</div><div className="text-xs text-muted-foreground">Trips</div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><Ticket className="w-6 h-6 text-green-500 mb-2" /><div className="text-2xl font-bold">{stats.bookings}</div><div className="text-xs text-muted-foreground">Bookings</div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><DollarSign className="w-6 h-6 text-amber-500 mb-2" /><div className="text-2xl font-bold">{formatCurrency(stats.revenue)}</div><div className="text-xs text-muted-foreground">Revenue</div></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6"><TrendingUp className="w-6 h-6 text-red-500 mb-2" /><div className="text-2xl font-bold">{stats.pendingTrips}</div><div className="text-xs text-muted-foreground">Pending Approval</div></CardContent></Card>
      </div>
      <div className="grid md:grid-cols-2 gap-6">
        <Card className="rounded-[20px]"><CardHeader><CardTitle>Recent Activity</CardTitle></CardHeader><CardContent><ul className="space-y-2 text-sm"><li>• New driver registration: Kwame Asante – needs verification</li><li>• Trip Accra→Kumasi pending approval</li><li>• Booking GR-XYZ cancelled • refund due</li></ul></CardContent></Card>
        <Card className="rounded-[20px]"><CardHeader><CardTitle>System Health</CardTitle></CardHeader><CardContent><div className="space-y-2"><div className="flex justify-between"><span>DB</span><span className="text-green-600">Healthy</span></div><div className="flex justify-between"><span>Paystack</span><span className="text-green-600">Online</span></div><div className="flex justify-between"><span>Storage</span><span>68% used</span></div></div></CardContent></Card>
      </div>
    </div>
  )
}
