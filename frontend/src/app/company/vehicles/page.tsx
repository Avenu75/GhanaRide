
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Bus } from 'lucide-react'

export default function CompanyVehicles() {
  const vehicles = [
    { id:1, plate:'GR-1234-22', brand:'Toyota', model:'Coaster', seats: 20, status:'ACTIVE' },
    { id:2, plate:'GR-5678-21', brand:'Nissan', model:'Civilian', seats: 18, status:'MAINTENANCE' },
  ]
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6"><h1 className="text-3xl font-bold">Fleet Management</h1><Button className="rounded-full">+ Add Vehicle</Button></div>
      <div className="grid md:grid-cols-2 gap-4">
        {vehicles.map(v=>(
          <Card key={v.id} className="rounded-[20px]"><CardContent className="p-6 flex gap-4"><div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center"><Bus className="w-6 h-6 text-primary" /></div><div className="flex-1"><div className="font-bold">{v.brand} {v.model}</div><div className="text-sm text-muted-foreground">{v.plate} • {v.seats} seats • {v.status}</div></div><Button variant="outline" size="sm" className="rounded-full">Manage</Button></CardContent></Card>
        ))}
      </div>
    </div>
  )
}
