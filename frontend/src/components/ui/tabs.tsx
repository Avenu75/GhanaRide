'use client'
import * as React from "react"
import { cn } from "@/lib/utils"

export function Tabs({ defaultValue, children, className }: { defaultValue?: string; children: React.ReactNode; className?: string }) {
  const [active, setActive] = React.useState(defaultValue)
  return (
    <div className={className}>
      {React.Children.map(children, child => {
        if (React.isValidElement(child)) {
          return React.cloneElement(child as any, { active, setActive })
        }
        return child
      })}
    </div>
  )
}
export function TabsList({ children, className, active, setActive }: any) {
  return (
    <div className={cn("inline-flex h-11 items-center justify-center rounded-xl bg-muted p-1 text-muted-foreground", className)}>
      {React.Children.map(children, (child: any) => React.cloneElement(child, { active, setActive }))}
    </div>
  )
}
export function TabsTrigger({ value, children, active, setActive }: any) {
  const isActive = active === value
  return (
    <button
      onClick={() => setActive(value)}
      className={cn("inline-flex items-center justify-center whitespace-nowrap rounded-lg px-4 py-2 text-sm font-medium transition-all", isActive ? "bg-background text-foreground shadow-sm" : "")}
    >
      {children}
    </button>
  )
}
export function TabsContent({ value, children, active }: any) {
  if (active !== value) return null
  return <div className="mt-4 ring-offset-background focus-visible:outline-none">{children}</div>
}
