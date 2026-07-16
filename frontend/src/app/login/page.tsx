'use client'
import { useState } from 'react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { useAuthStore } from '@/store/auth.store'
import { apiClient } from '@/lib/api/client'
import { toast } from 'sonner'
import { useRouter } from 'next/navigation'
import { Bus, Eye, EyeOff } from 'lucide-react'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [loading, setLoading] = useState(false)
  const { setAuth } = useAuthStore()
  const router = useRouter()

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await apiClient.post('/auth/login', { email, password })
      const data = res.data.data || res.data
      setAuth(data.user, data.token, data.refreshToken)
      toast.success(`Welcome back, ${data.user.fullName}!`)
      const role = data.user.role
      if (role === 'ADMIN') router.push('/admin/dashboard')
      else if (role === 'DRIVER') router.push('/driver/dashboard')
      else if (role === 'COMPANY') router.push('/company/dashboard')
      else router.push('/dashboard')
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Login failed. Check credentials.')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-gradient-to-br from-zinc-50 to-white dark:from-zinc-950 dark:to-zinc-900">
      <Card className="w-full max-w-md rounded-[24px] shadow-xl border-0">
        <CardHeader className="space-y-4 text-center">
          <div className="mx-auto w-12 h-12 rounded-xl ghana-gradient flex items-center justify-center text-white"><Bus className="w-6 h-6" /></div>
          <CardTitle className="text-2xl">Welcome back</CardTitle>
          <CardDescription>Sign in to your GhanaRide account</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleLogin} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="email">Email or Username</Label>
              <Input id="email" type="text" placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} required />
            </div>
            <div className="space-y-2">
              <div className="flex justify-between"><Label htmlFor="password">Password</Label><Link href="/forgot-password" className="text-xs text-primary hover:underline">Forgot?</Link></div>
              <div className="relative">
                <Input id="password" type={show ? 'text' : 'password'} placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)} required />
                <button type="button" onClick={() => setShow(!show)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">{show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}</button>
              </div>
            </div>
            <Button type="submit" className="w-full h-11 rounded-xl" loading={loading}>Sign In</Button>
            <div className="relative my-6"><div className="absolute inset-0 flex items-center"><div className="w-full border-t" /></div><div className="relative flex justify-center text-xs uppercase"><span className="bg-card px-2 text-muted-foreground">Or</span></div></div>
            <div className="text-center text-sm">No account? <Link href="/register" className="font-semibold text-primary hover:underline">Create one</Link></div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
