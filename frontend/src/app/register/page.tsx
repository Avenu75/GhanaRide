'use client'
import { useState } from 'react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { apiClient } from '@/lib/api/client'
import { toast } from 'sonner'
import { useRouter } from 'next/navigation'
import { Bus } from 'lucide-react'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'

export default function RegisterPage() {
  const [form, setForm] = useState({ fullName: '', email: '', username: '', phoneNumber: '', password: '', confirmPassword: '' })
  const [loading, setLoading] = useState(false)
  const router = useRouter()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (form.password !== form.confirmPassword) { toast.error('Passwords do not match'); return }
    setLoading(true)
    try {
      await apiClient.post('/auth/register', { fullName: form.fullName, email: form.email, username: form.username, phoneNumber: form.phoneNumber, password: form.password })
      toast.success('Account created! Please sign in.')
      router.push('/login')
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Registration failed')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-[90vh] flex items-center justify-center px-4 py-10 bg-gradient-to-br from-zinc-50 to-white">
      <Card className="w-full max-w-lg rounded-[24px] shadow-xl border-0">
        <CardHeader className="text-center">
          <div className="mx-auto w-12 h-12 rounded-xl ghana-gradient flex items-center justify-center text-white mb-2"><Bus className="w-6 h-6" /></div>
          <CardTitle>Join GhanaRide</CardTitle>
          <CardDescription>Ghana&apos;s premium travel community</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="passenger" className="w-full">
            <TabsList className="grid w-full grid-cols-3 mb-6">
              <TabsTrigger value="passenger">Passenger</TabsTrigger>
              <TabsTrigger value="driver">Driver</TabsTrigger>
              <TabsTrigger value="company">Company</TabsTrigger>
            </TabsList>
            <TabsContent value="passenger">
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2"><Label>Full Name</Label><Input value={form.fullName} onChange={e => setForm({...form, fullName: e.target.value})} required /></div>
                  <div className="space-y-2"><Label>Username</Label><Input value={form.username} onChange={e => setForm({...form, username: e.target.value})} required /></div>
                </div>
                <div className="space-y-2"><Label>Email</Label><Input type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} required /></div>
                <div className="space-y-2"><Label>Phone (Ghana)</Label><Input placeholder="0244 123456" value={form.phoneNumber} onChange={e => setForm({...form, phoneNumber: e.target.value})} required /></div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2"><Label>Password</Label><Input type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required /></div>
                  <div className="space-y-2"><Label>Confirm</Label><Input type="password" value={form.confirmPassword} onChange={e => setForm({...form, confirmPassword: e.target.value})} required /></div>
                </div>
                <Button type="submit" className="w-full h-11 rounded-xl" loading={loading}>Create Account</Button>
              </form>
            </TabsContent>
            <TabsContent value="driver">
              <div className="text-center py-6"><p className="text-sm text-muted-foreground mb-4">Driver registration requires license and ID verification.</p><Link href="/register/driver"><Button className="w-full">Continue as Driver</Button></Link></div>
            </TabsContent>
            <TabsContent value="company">
              <div className="text-center py-6"><p className="text-sm text-muted-foreground mb-4">Company registration requires business certificate.</p><Link href="/register/company"><Button className="w-full">Continue as Company</Button></Link></div>
            </TabsContent>
          </Tabs>
          <p className="text-center text-sm mt-6">Already have account? <Link href="/login" className="font-semibold text-primary">Sign in</Link></p>
        </CardContent>
      </Card>
    </div>
  )
}
