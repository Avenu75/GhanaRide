
'use client'
import { useState, useEffect } from 'react'
import { useAuthStore } from '@/store/auth.store'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiClient } from '@/lib/api/client'
import { toast } from 'sonner'
import { User, Mail, Phone, Shield } from 'lucide-react'
import { getInitials } from '@/lib/utils'

export default function ProfilePage() {
  const { user, setUser } = useAuthStore()
  const [form, setForm] = useState({ fullName: user?.fullName||'', email: user?.email||'', phoneNumber: user?.phoneNumber||'', address: '' })
  const [loading, setLoading] = useState(false)

  useEffect(()=>{ if(user) setForm({ fullName: user.fullName, email: user.email, phoneNumber: user.phoneNumber||'', address: (user as any).address||'' }) },[user])

  const handleSave = async (e:React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await apiClient.put('/users/me', form)
      const updated = res.data.data || res.data
      setUser(updated)
      toast.success('Profile updated')
    } catch (err:any) { toast.error(err.response?.data?.message || 'Update failed') } finally { setLoading(false) }
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <h1 className="text-3xl font-bold mb-6">Profile Settings</h1>
      <div className="grid md:grid-cols-[280px_1fr] gap-6">
        <Card className="rounded-[20px] h-fit"><CardContent className="p-6 text-center">
          <div className="w-20 h-20 rounded-full bg-primary text-primary-foreground mx-auto flex items-center justify-center text-xl font-bold">{getInitials(user?.fullName||'U')}</div>
          <div className="mt-4 font-semibold">{user?.fullName}</div><div className="text-sm text-muted-foreground">{user?.email}</div>
          <div className="mt-3 inline-flex items-center gap-1 text-xs bg-green-50 text-green-700 px-3 py-1 rounded-full"><Shield className="w-3 h-3" /> {user?.role}</div>
        </CardContent></Card>
        <Card className="rounded-[20px]"><CardHeader><CardTitle>Personal Info</CardTitle></CardHeader><CardContent><form onSubmit={handleSave} className="space-y-4">
          <div className="space-y-2"><Label>Full Name</Label><div className="relative"><User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" /><Input className="pl-10" value={form.fullName} onChange={e=>setForm({...form, fullName:e.target.value})} /></div></div>
          <div className="space-y-2"><Label>Email</Label><div className="relative"><Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" /><Input className="pl-10" value={form.email} onChange={e=>setForm({...form, email:e.target.value})} /></div></div>
          <div className="space-y-2"><Label>Phone (Ghana)</Label><div className="relative"><Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" /><Input className="pl-10" value={form.phoneNumber} onChange={e=>setForm({...form, phoneNumber:e.target.value})} placeholder="0244123456" /></div></div>
          <Button type="submit" className="rounded-xl" loading={loading}>Save Changes</Button>
        </form></CardContent></Card>
      </div>
    </div>
  )
}
