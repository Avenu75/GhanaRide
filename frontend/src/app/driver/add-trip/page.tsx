
'use client'
import { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { GHANA_CITIES } from '@/lib/utils'
import { toast } from 'sonner'
import { apiClient } from '@/lib/api/client'
import { useRouter } from 'next/navigation'

export default function AddTripPage() {
  const [form, setForm] = useState({ fromLocation:'', toLocation:'', departureTime:'', tripAmount:'', totalSeats:'18', description:'' })
  const [loading, setLoading] = useState(false)
  const router = useRouter()

  const submit = async (e:React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await apiClient.post('/trips', { ...form, tripAmount: parseFloat(form.tripAmount), totalSeats: parseInt(form.totalSeats), availableSeats: parseInt(form.totalSeats) })
      toast.success('Trip created! Awaiting admin approval.')
      router.push('/driver/dashboard')
    } catch (err:any) { toast.error(err.response?.data?.message || 'Failed to create trip') } finally { setLoading(false) }
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      <h1 className="text-3xl font-bold mb-6">Add New Trip</h1>
      <Card className="rounded-[20px]"><CardHeader><CardTitle>Trip Details</CardTitle></CardHeader><CardContent><form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>From</Label><select value={form.fromLocation} onChange={e=>setForm({...form, fromLocation:e.target.value})} className="w-full h-11 rounded-xl border px-3 bg-background" required><option value="">Select</option>{GHANA_CITIES.map(c=><option key={c} value={c}>{c}</option>)}</select></div><div className="space-y-2"><Label>To</Label><select value={form.toLocation} onChange={e=>setForm({...form, toLocation:e.target.value})} className="w-full h-11 rounded-xl border px-3 bg-background" required><option value="">Select</option>{GHANA_CITIES.map(c=><option key={c} value={c}>{c}</option>)}</select></div></div>
        <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>Departure Time</Label><Input type="datetime-local" value={form.departureTime} onChange={e=>setForm({...form, departureTime:e.target.value})} required /></div><div className="space-y-2"><Label>Price per Seat (GHS)</Label><Input type="number" step="0.01" value={form.tripAmount} onChange={e=>setForm({...form, tripAmount:e.target.value})} required /></div></div>
        <div className="space-y-2"><Label>Total Seats</Label><Input type="number" value={form.totalSeats} onChange={e=>setForm({...form, totalSeats:e.target.value})} /></div>
        <div className="space-y-2"><Label>Description / Pickup station</Label><Textarea value={form.description} onChange={e=>setForm({...form, description:e.target.value})} placeholder="Pickup at Circle station, near..." /></div>
        <Button type="submit" className="w-full h-12 rounded-xl" loading={loading}>Create Trip</Button>
      </form></CardContent></Card>
    </div>
  )
}
