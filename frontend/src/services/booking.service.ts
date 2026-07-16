
import { apiClient } from '@/lib/api/client'
import { Booking } from '@/types'
export const bookingService = {
  create: async (tripId:number, data:any): Promise<Booking> => { const res = await apiClient.post(`/bookings/trip/${tripId}`, data); return res.data.data || res.data },
  myBookings: async (): Promise<Booking[]> => { const res = await apiClient.get('/bookings/my'); return res.data.data?.content || res.data.content || res.data },
  cancel: async (id:number) => { const res = await apiClient.post(`/bookings/${id}/cancel`); return res.data },
}
