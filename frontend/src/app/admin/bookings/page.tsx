
'use client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function AdminBookings() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Manage Bookings</h1>
      <Card className="rounded-[20px]"><CardHeader><CardTitle>Recent Bookings</CardTitle></CardHeader><CardContent><p className="text-muted-foreground">Admin can view all bookings, cancel, and process refunds.</p></CardContent></Card>
    </div>
  )
}
