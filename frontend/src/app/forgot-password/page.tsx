
'use client'
import { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { apiClient } from '@/lib/api/client'
import { toast } from 'sonner'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const submit = async (e:React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try { await apiClient.post('/auth/forgot-password', { email }); toast.success('Reset link sent to your email') } catch (err:any) { toast.error(err.response?.data?.message || 'Failed') } finally { setLoading(false) }
  }
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4">
      <Card className="w-full max-w-md rounded-[24px]"><CardHeader><CardTitle>Forgot Password</CardTitle><CardDescription>Enter your email to receive reset link</CardDescription></CardHeader><CardContent><form onSubmit={submit} className="space-y-4"><div className="space-y-2"><Label>Email</Label><Input type="email" value={email} onChange={e=>setEmail(e.target.value)} required /></div><Button type="submit" className="w-full rounded-xl" loading={loading}>Send Reset Link</Button></form></CardContent></Card>
    </div>
  )
}
