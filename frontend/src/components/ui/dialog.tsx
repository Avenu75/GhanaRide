'use client'
import * as React from "react"
import { cn } from "@/lib/utils"

interface DialogProps { open?: boolean; onOpenChange?: (open: boolean) => void; children: React.ReactNode }
export function Dialog({ open, onOpenChange, children }: DialogProps) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => onOpenChange?.(false)} />
      <div className="relative z-10 w-full max-w-lg">{children}</div>
    </div>
  )
}
export function DialogContent({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn("bg-background rounded-2xl shadow-xl border p-6 m-4 max-h-[90vh] overflow-auto", className)}>{children}</div>
}
export function DialogHeader({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn("flex flex-col space-y-2 mb-4", className)}>{children}</div>
}
export function DialogTitle({ children, className }: { children: React.ReactNode; className?: string }) {
  return <h3 className={cn("text-lg font-semibold", className)}>{children}</h3>
}
