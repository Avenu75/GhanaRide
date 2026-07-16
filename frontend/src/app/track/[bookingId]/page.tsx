
'use client'
import { useParams } from 'next/navigation'
import { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { MapPin, Bus, Clock, Navigation } from 'lucide-react'

export default function TrackPage() {
  const { bookingId } = useParams()
  const [live, setLive] = useState({ lat:5.6037, lng:-0.1870, speed:78, eta:42, next:'Nkawkaw', status:'EN_ROUTE' })

  useEffect(()=>{
    const interval = setInterval(()=>{
      setLive(l=>({ ...l, lat: l.lat + (Math.random()-0.5)*0.01, lng: l.lng + (Math.random()-0.5)*0.01 }))
    }, 3000)
    return ()=>clearInterval(interval)
  },[])

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Live Tracking • Booking #{bookingId}</h1>
      <div className="grid md:grid-cols-[1fr_380px] gap-6">
        <Card className="rounded-[20px] overflow-hidden">
          <div className="h-[500px] bg-zinc-100 dark:bg-zinc-900 relative flex items-center justify-center">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(252,209,22,0.1),transparent_70%)]" />
            <div className="text-center">
              <div className="w-20 h-20 rounded-full bg-primary mx-auto flex items-center justify-center animate-pulse"><Navigation className="w-10 h-10 text-primary-foreground" /></div>
              <p className="mt-4 font-medium">Live Map - OSM / Google Maps</p>
              <p className="text-sm text-muted-foreground">Lat: {live.lat.toFixed(4)}, Lng: {live.lng.toFixed(4)}</p>
              <div className="mt-4 inline-flex gap-2 text-xs bg-white px-3 py-1 rounded-full shadow"><Bus className="w-4 h-4" /> Moving • {live.speed} km/h • ETA {live.eta} min</div>
            </div>
            {/* simulated bus dot */}
            <div className="absolute top-1/2 left-1/2 w-4 h-4 bg-ghana-red rounded-full animate-ping" />
            <div className="absolute top-1/2 left-1/2 w-3 h-3 bg-ghana-red rounded-full" />
          </div>
        </Card>
        <div className="space-y-4">
          <Card className="rounded-[20px]"><CardHeader><CardTitle>Status</CardTitle></CardHeader><CardContent className="space-y-3">
            <div className="flex justify-between"><span className="text-muted-foreground">Status</span><span className="font-medium text-green-600">{live.status}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Next Stop</span><span className="font-medium">{live.next}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Speed</span><span className="font-medium">{live.speed} km/h</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">ETA</span><span className="font-medium">{live.eta} mins</span></div>
          </CardContent></Card>
          <Card className="rounded-[20px]"><CardContent className="p-4 space-y-3">
            <div className="flex gap-3"><div className="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center"><MapPin className="w-4 h-4 text-green-600" /></div><div><div className="font-medium">Accra - Circle</div><div className="text-xs text-muted-foreground">Departed • 6:30 AM</div></div></div>
            <div className="ml-4 border-l-2 border-dashed h-8" />
            <div className="flex gap-3"><div className="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center animate-pulse"><Bus className="w-4 h-4 text-amber-600" /></div><div><div className="font-medium">En Route</div><div className="text-xs text-muted-foreground">Live • {live.next}</div></div></div>
            <div className="ml-4 border-l-2 border-dashed h-8" />
            <div className="flex gap-3"><div className="w-8 h-8 rounded-full bg-zinc-100 flex items-center justify-center"><MapPin className="w-4 h-4" /></div><div><div className="font-medium">Kumasi - Kejetia</div><div className="text-xs text-muted-foreground">Arriving in {live.eta} mins</div></div></div>
          </CardContent></Card>
        </div>
      </div>
    </div>
  )
}
