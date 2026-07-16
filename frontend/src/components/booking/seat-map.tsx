
'use client'
import { Seat } from '@/types'
import { cn } from '@/lib/utils'

export function SeatMap({ seats, selected, onSelect }: { seats: Seat[]; selected?: string; onSelect: (seat:string)=>void }) {
  return (
    <div className="grid grid-cols-4 gap-2 max-w-sm mx-auto">
      {seats.map(s=>(
        <button key={s.seatNumber} disabled={s.status!=='AVAILABLE'} onClick={()=>onSelect(s.seatNumber)} className={cn("h-12 rounded-xl border font-medium", s.status!=='AVAILABLE' ? "bg-zinc-100 text-zinc-400" : selected===s.seatNumber ? "bg-primary text-primary-foreground" : "bg-white hover:border-primary")}>{s.seatNumber}</button>
      ))}
    </div>
  )
}
