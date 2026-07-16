'use client'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { useState } from 'react'
import { toast } from 'sonner'
import { apiClient } from '@/lib/api/client'
import { useRouter } from 'next/navigation'

export default function CompanyRegisterPage() {
  const [form, setForm] = useState({ fullName:'', email:'', companyName:'', companyRegistrationNo:'', phoneNumber:'', password:'', companyLocation:'', companyDescription:'' })
  const [loading, setLoading] = useState(false)
  const router = useRouter()
  const submit = async (e:React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const fd = new FormData()
      Object.entries(form).forEach(([k,v])=>fd.append(k,v))
      await apiClient.post('/auth/register/company', fd, { headers:{ 'Content-Type':'multipart/form-data' } })
      toast.success('Company application submitted!')
      router.push('/login')
    } catch (err:any) { toast.error('Failed') } finally { setLoading(false) }
  }
  return (
    <div className="container mx-auto px-4 py-10 max-w-2xl">
      <Card className="rounded-[24px]">
        <CardHeader><CardTitle>Company Partnership</CardTitle><CardDescription>Manage fleet, drivers, and intercity routes with GhanaRide business dashboard.</CardDescription></CardHeader>
        <CardContent><form onSubmit={submit} className="space-y-4">
          <div className="space-y-2"><Label>Company Name</Label><Input value={form.companyName} onChange={e=>setForm({...form, companyName:e.target.value})} required /></div>
          <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>Contact Person Name</Label><Input value={form.fullName} onChange={e=>setForm({...form, fullName:e.target.value})} required /></div><div className="space-y-2"><Label>Registration No</Label><Input value={form.companyRegistrationNo} onChange={e=>setForm({...form, companyRegistrationNo:e.target.value})} /></div></div>
          <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label>Email</Label><Input type="email" value={form.email} onChange={e=>setForm({...form, email:e.target.value})} required /></div><div className="space-y-2"><Label>Phone</Label><Input value={form.phoneNumber} onChange={e=>setForm({...form, phoneNumber:e.target.value})} required /></div></div>
          <div className="space-y-2"><Label>Password</Label><Input type="password" value={form.password} onChange={e=>setForm({...form, password:e.target.value})} required /></div>
          <div className="space-y-2"><Label>Location</Label><Input value={form.companyLocation} onChange={e=>setForm({...form, companyLocation:e.target.value})} /></div>
          <div className="space-y-2"><Label>Description</Label><Textarea value={form.companyDescription} onChange={e=>setForm({...form, companyDescription:e.target.value})} /></div>
          <div className="space-y-2"><Label>Business Certificate (upload)</Label><Input type="file" /></div>
          <Button type="submit" className="w-full h-12 rounded-xl" loading={loading}>Submit Company Application</Button>
        </form></CardContent>
      </Card>
    </div>
  )
}
