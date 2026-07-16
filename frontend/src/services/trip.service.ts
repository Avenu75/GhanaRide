
import { apiClient } from '@/lib/api/client'
import { Trip, TripSearchParams } from '@/types'
export const tripService = {
  search: async (params: TripSearchParams): Promise<Trip[]> => { const res = await apiClient.get('/trips/search', { params }); return res.data.data || res.data },
  getById: async (id:number): Promise<Trip> => { const res = await apiClient.get(`/trips/${id}`); return res.data.data || res.data },
  create: async (data:any) => { const res = await apiClient.post('/trips', data); return res.data.data || res.data },
  listApproved: async () => { const res = await apiClient.get('/trips', { params:{ status:'APPROVED' } }); return res.data.data || res.data },
}
