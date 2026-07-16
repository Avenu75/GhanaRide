
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'

export default function ResetPasswordPage() {
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4">
      <Card className="w-full max-w-md rounded-[24px]"><CardHeader><CardTitle>Reset Password</CardTitle></CardHeader><CardContent><form className="space-y-4"><div className="space-y-2"><Label>New Password</Label><Input type="password" required /></div><div className="space-y-2"><Label>Confirm Password</Label><Input type="password" required /></div><Button className="w-full rounded-xl">Reset Password</Button></form></CardContent></Card>
    </div>
  )
}
