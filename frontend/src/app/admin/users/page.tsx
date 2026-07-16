
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export default function AdminUsers() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Manage Users</h1>
      <Card className="rounded-[20px]"><CardHeader className="flex flex-row justify-between items-center"><CardTitle>Users</CardTitle><Input placeholder="Search users..." className="max-w-xs" /></CardHeader><CardContent className="space-y-2">
        {[1,2,3].map(i=><div key={i} className="flex justify-between items-center p-3 rounded-xl bg-muted/50"><div><div className="font-medium">User {i} • user{i}@example.com</div><div className="text-xs text-muted-foreground">Role: USER • Joined 2024</div></div><Button variant="outline" size="sm" className="rounded-full">View</Button></div>)}
      </CardContent></Card>
    </div>
  )
}
