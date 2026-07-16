import * as React from "react"
import { cn } from "@/lib/utils"

export function Select({ children, value, onValueChange, ...props }: { children: React.ReactNode; value?: string; onValueChange?: (v: string) => void }) {
  return (
    <select value={value} onChange={(e) => onValueChange?.(e.target.value)} className={cn("flex h-11 w-full rounded-xl border border-input bg-background px-4 py-2 text-sm")} {...props}>
      {children}
    </select>
  )
}
export function SelectItem({ children, value }: { children: React.ReactNode; value: string }) {
  return <option value={value}>{children}</option>
}
