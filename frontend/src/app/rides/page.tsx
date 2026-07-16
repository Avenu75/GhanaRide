
'use client'
import { useState, useEffect } from 'react'
import { useSearchParams } from 'next/navigation'
import { apiClient } from '@/lib/api/client'
import { Trip, Seat } from '@/types'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { formatCurrency, formatDate, GHANA_CITIES } from '@/lib/utils'
import { MapPin, Clock, Bus, Users, ArrowRight, Search, Filter } from 'lucide-react'
import Link from 'next/link'
import { toast } from 'sonner'

export default function RidesPage() {
  const searchParams = useSearchParams()
  const [trips, setTrips] = useState<Trip[]>([])
  const [loading, setLoading] = useState(true)
  const [from, setFrom] = useState(searchParams.get('from') || '')
  const [to, setTo] = useState(searchParams.get('to') || '')
  const [date, setDate] = useState('')

  const fetchTrips = async () => {
    setLoading(true)
    try {
      const res = await apiClient.get('/trips/search', { params: { from, to, date } })
      const data = res.data.data || res.data
      setTrips(Array.isArray(data) ? data : data.content || [])
    } catch (e:any) {
      // fallback mock for demo if backend not running
      setTrips([
        { id: 1, fromLocation: from || 'Accra', toLocation: to || 'Kumasi', departureTime: new Date(Date.now()+3600000*2).toISOString(), tripAmount: 120, availableSeats: 12, totalSeats: 18, status: 'APPROVED', car: { id:1, plateNumber:'GR-1234-22', carBrand:'Toyota', model:'Coaster', totalSeats:18, status:'ACTIVE', imagePath:'' } as any, createdAt: new Date().toISOString() },
        { id: 2, fromLocation: from || 'Accra', toLocation: to || 'Cape Coast', departureTime: new Date(Date.now()+3600000*5).toISOString(), tripAmount: 80, availableSeats: 5, totalSeats: 18, status: 'APPROVED', car: { id:2, plateNumber:'GR-5678-21', carBrand:'Nissan', model:'Civilian', totalSeats:18, status:'ACTIVE', imagePath:'' } as any, createdAt: new Date().toISOString() },
      ])
    } finally { setLoading(false) }
  }

  useEffect(() => { fetchTrips() }, [])

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Find your ride</h1>
        <p className="text-muted-foreground">Trusted trips across Ghana, verified drivers, instant booking</p>
      </div>

      <Card className="rounded-[20px] shadow-sm mb-8">
        <CardContent className="p-6">
          <div className="grid md:grid-cols-4 gap-4">
            <div className="space-y-2"><Label>From</Label><select value={from} onChange={e=>setFrom(e.target.value)} className="w-full h-11 rounded-xl border px-3 bg-background"><option value="">Any</option>{GHANA_CITIES.map(c=><option key={c} value={c}>{c}</option>)}</select></div>
            <div className="space-y-2"><Label>To</Label><select value={to} onChange={e=>setTo(e.target.value)} className="w-full h-11 rounded-xl border px-3 bg-background"><option value="">Any</option>{GHANA_CITIES.map(c=><option key={c} value={c}>{c}</option>)}</select></div>
            <div className="space-y-2"><Label>Date</Label><Input type="date" value={date} onChange={e=>setDate(e.target.value)} /></div>
            <div className="flex items-end"><Button onClick={fetchTrips} className="w-full h-11 rounded-xl"><Search className="w-4 h-4 mr-2" /> Search</Button></div>
          </div>
        </CardContent>
      </Card>

      {loading ? <div className="space-y-4">{[1,2,3].map(i=><Skeleton key={i} className="h-40 w-full rounded-2xl" />)}</div> : (
        <div className="grid gap-4">
          {trips.length===0 ? <div className="text-center py-16 border border-dashed rounded-2xl"><Bus className="w-12 h-12 mx-auto text-muted-foreground mb-3" /><p className="font-medium">No trips found</p><p className="text-sm text-muted-foreground">Try adjusting your search</p></div> :
            trips.map(trip=>(
              <Card key={trip.id} className="rounded-[20px] hover:shadow-md transition hover-lift overflow-hidden">
                <CardContent className="p-0">
                  <div className="p-6 flex flex-col md:flex-row gap-6">
                    <div className="flex-1">
                      <div className="flex items-start justify-between mb-4">
                        <div className="space-y-1">
                          <div className="flex items-center gap-2 text-lg font-bold"><MapPin className="w-5 h-5 text-primary" /> {trip.fromLocation} <ArrowRight className="w-4 h-4 text-muted-foreground" /> {trip.toLocation}</div>
                          <div className="flex items-center gap-4 text-sm text-muted-foreground"><span className="flex items-center gap-1"><Clock className="w-4 h-4" /> {formatDate(trip.departureTime)}</span><span className="flex items-center gap-1"><Bus className="w-4 h-4" /> {trip.car.carBrand} {trip.car.model} ({trip.car.plateNumber})</span></div>
                        </div>
                        <div className="text-right"><div className="text-2xl font-bold">{formatCurrency(trip.tripAmount)}</div><div className="text-xs text-muted-foreground">per seat</div></div>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1 text-sm bg-green-50 text-green-700 px-3 py-1 rounded-full"><Users className="w-4 h-4" /> {trip.availableSeats} seats left</div>
                        <div className="text-xs text-muted-foreground">Total {trip.totalSeats} seats • {trip.car.totalSeats} capacity</div>
                      </div>
                    </div>
                    <div className="md:w-48 flex flex-col justify-center gap-2">
                      <Link href={`/booking/${trip.id}`}><Button className="w-full rounded-xl h-11">Book Now</Button></Link>
                      <Link href={`/rides`}><Button variant="outline" className="w-full rounded-xl">View Details</Button></Link>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))
          }
        </div>
      )}
    </div>
  )
}
