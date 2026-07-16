
'use client'
import { useEffect, useState } from 'react'
import { apiClient } from '@/lib/api/client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Notification } from '@/types'
import { formatDate } from '@/lib/utils'
import { Bell, CheckCheck, Trash } from 'lucide-react'
import { toast } from 'sonner'

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([
    { id:1, type:'BOOKING_CONFIRMED', title:'Booking Confirmed', message:'Your booking GR-AB12CD for Accra → Kumasi is confirmed.', read:false, createdAt:new Date().toISOString() },
    { id:2, type:'PAYMENT_SUCCESS', title:'Payment Received', message:'GH₵120 payment successful via Mobile Money.', read:false, createdAt:new Date(Date.now()-3600000).toISOString() },
    { id:3, type:'TRIP_REMINDER', title:'Trip Departing Soon', message:'Your bus leaves in 2 hours from Circle station.', read:true, createdAt:new Date(Date.now()-86400000).toISOString() },
  ])

  useEffect(()=>{
    const load = async () => {
      try {
        const res = await apiClient.get('/notifications')
        setNotifications(res.data.data?.content || res.data.content || res.data || [])
      } catch {}
    }
    load()
  },[])

  const markAllRead = async () => {
    try { await apiClient.post('/notifications/read-all') } catch {}
    setNotifications(n=>n.map(x=>({...x, read:true})))
    toast.success('All marked as read')
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <div className="flex justify-between items-center mb-6"><h1 className="text-3xl font-bold flex items-center gap-2"><Bell className="w-7 h-7" /> Notifications</h1><Button variant="outline" size="sm" onClick={markAllRead} className="rounded-full"><CheckCheck className="w-4 h-4 mr-1" /> Mark all read</Button></div>
      <div className="space-y-3">
        {notifications.map(n=>(
          <Card key={n.id} className={`rounded-2xl ${!n.read ? 'border-primary/20 bg-primary/5' : ''}`}><CardContent className="p-4 flex gap-4"><div className={`w-10 h-10 rounded-xl flex items-center justify-center ${!n.read ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'}`}><Bell className="w-5 h-5" /></div><div className="flex-1"><div className="font-medium">{n.title}</div><div className="text-sm text-muted-foreground">{n.message}</div><div className="text-xs text-muted-foreground mt-1">{formatDate(n.createdAt)}</div></div><Button variant="ghost" size="icon" className="h-8 w-8"><Trash className="w-4 h-4" /></Button></CardContent></Card>
        ))}
      </div>
    </div>
  )
}
