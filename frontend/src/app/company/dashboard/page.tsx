
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import Link from 'next/link'
import { Bus, Users, DollarSign } from 'lucide-react'
import { formatCurrency } from '@/lib/utils'

export default function CompanyDashboard() {
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6"><h1 className="text-3xl font-bold">Company Dashboard</h1><div className="flex gap-2"><Link href="/company/vehicles"><Button variant="outline" className="rounded-full">Vehicles</Button></Link><Link href="/company/add-trip"><Button className="rounded-full">+ Add Trip</Button></Link></div></div>
      <div className="grid md:grid-cols-3 gap-4 mb-8">
        <Card className="rounded-2xl"><CardContent className="p-6 flex justify-between"><div><div className="text-sm text-muted-foreground">Fleet</div><div className="text-2xl font-bold">5 Vehicles</div></div><Bus className="w-8 h-8 text-muted-foreground" /></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6 flex justify-between"><div><div className="text-sm text-muted-foreground">Monthly Revenue</div><div className="text-2xl font-bold">{formatCurrency(12500)}</div></div><DollarSign className="w-8 h-8 text-green-500" /></CardContent></Card>
        <Card className="rounded-2xl"><CardContent className="p-6 flex justify-between"><div><div className="text-sm text-muted-foreground">Drivers</div><div className="text-2xl font-bold">12</div></div><Users className="w-8 h-8 text-blue-500" /></CardContent></Card>
      </div>
      <Card className="rounded-[20px]"><CardHeader><CardTitle>Recent Trips</CardTitle></CardHeader><CardContent><p className="text-muted-foreground">Manage your company trips, drivers and vehicles from here.</p></CardContent></Card>
    </div>
  )
}
