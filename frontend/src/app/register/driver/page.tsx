'use client'
import { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { toast } from 'sonner'
import { apiClient } from '@/lib/api/client'
import { useRouter } from 'next/navigation'

export default function DriverRegisterPage() {
  const [form, setForm] = useState({ fullName:'', email:'', username:'', password:'', phoneNumber:'', licenseNumber:'', vehiclePlate:'', vehicleModel:'' })
  const [loading, setLoading] = useState(false)
  const router = useRouter()
  const submit = async (e:React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const fd = new FormData()
      fd.append('fullName', form.fullName)
      fd.append('email', form.email)
      fd.append('username', form.username)
      fd.append('password', form.password)
      fd.append('phoneNumber', form.phoneNumber)
      fd.append('licenseNumber', form.licenseNumber)
      // file uploads would be appended here: licenseFile, idFile
      await apiClient.post('/auth/register/driver', fd, { headers: { 'Content-Type':'multipart/form-data' } })
      toast.success('Driver application submitted! Awaiting verification.')
      router.push('/login')
    } catch (err:any) { toast.error(err.response?.data?.message || 'Registration failed') } finally { setLoading(false) }
  }
  return (
    <div className="container mx-auto px-4 py-10 max-w-2xl">
      <Card className="rounded-[24px]">
        <CardHeader><CardTitle>Drive with GhanaRide</CardTitle><CardDescription>Earn by driving intercity routes. Verified drivers get priority trips.</CardDescription></CardHeader>
        <CardContent><form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>Full Name</Label><Input value={form.fullName} onChange={e=>setForm({...form, fullName:e.target.value})} required /></div><div className="space-y-2"><Label>Username</Label><Input value={form.username} onChange={e=>setForm({...form, username:e.target.value})} required /></div></div>
          <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>Email</Label><Input type="email" value={form.email} onChange={e=>setForm({...form, email:e.target.value})} required /></div><div className="space-y-2"><Label>Phone</Label><Input value={form.phoneNumber} onChange={e=>setForm({...form, phoneNumber:e.target.value})} required /></div></div>
          <div className="space-y-2"><Label>Password</Label><Input type="password" value={form.password} onChange={e=>setForm({...form, password:e.target.value})} required /></div>
          <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>License Number</Label><Input value={form.licenseNumber} onChange={e=>setForm({...form, licenseNumber:e.target.value})} /></div><div className="space-y-2"><Label>Vehicle Plate</Label><Input value={form.vehiclePlate} onChange={e=>setForm({...form, vehiclePlate:e.target.value})} /></div></div>
          <div className="space-y-2"><Label>Vehicle Model</Label><Input value={form.vehicleModel} onChange={e=>setForm({...form, vehicleModel:e.target.value})} placeholder="Toyota Coaster 2022" /></div>
          <div className="space-y-2"><Label>License Document (upload)</Label><Input type="file" /></div>
          <div className="space-y-2"><Label>ID / Passport (upload)</Label><Input type="file" /></div>
          <Button type="submit" className="w-full h-12 rounded-xl" loading={loading}>Submit Driver Application</Button>
        </form></CardContent>
      </Card>
    </div>
  )
}
