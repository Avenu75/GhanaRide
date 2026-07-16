'use client'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useAuthStore } from '@/store/auth.store'
import { Button } from '@/components/ui/button'
import { MapPin, Bus, Menu, X, User, LogOut, Bell, Wallet, Ticket, LayoutDashboard } from 'lucide-react'
import { useState } from 'react'
import { cn, getInitials } from '@/lib/utils'

const navLinks = [
  { href: '/', label: 'Home' },
  { href: '/rides', label: 'Find Trips' },
  { href: '/about', label: 'About' },
  { href: '/contact', label: 'Contact' },
]

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const pathname = usePathname()
  const { user, isAuthenticated, logout } = useAuthStore()

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto px-4 flex h-16 items-center justify-between">
        <Link href="/" className="flex items-center gap-2 font-bold text-xl">
          <div className="w-9 h-9 rounded-xl ghana-gradient flex items-center justify-center text-white">
            <Bus className="w-5 h-5" />
          </div>
          <span className="font-display">Ghana<span className="text-primary">Ride</span></span>
        </Link>

        <nav className="hidden md:flex items-center gap-6">
          {navLinks.map(link => (
            <Link key={link.href} href={link.href} className={cn("text-sm font-medium transition-colors hover:text-primary", pathname === link.href ? "text-primary" : "text-muted-foreground")}>
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          {isAuthenticated && user ? (
            <>
              <Link href="/notifications" className="hidden md:flex relative p-2 rounded-xl hover:bg-accent">
                <Bell className="w-5 h-5" />
              </Link>
              <Link href="/wallet" className="hidden md:flex p-2 rounded-xl hover:bg-accent">
                <Wallet className="w-5 h-5" />
              </Link>
              <div className="hidden md:flex items-center gap-2">
                <Link href={user.role === 'DRIVER' ? '/driver/dashboard' : user.role === 'COMPANY' ? '/company/dashboard' : user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'}>
                  <Button variant="ghost" size="sm">
                    <LayoutDashboard className="w-4 h-4 mr-2" /> Dashboard
                  </Button>
                </Link>
                <Link href="/profile" className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-xs font-bold">
                    {getInitials(user.fullName)}
                  </div>
                </Link>
              </div>
            </>
          ) : (
            <div className="hidden md:flex items-center gap-2">
              <Link href="/login"><Button variant="ghost" size="sm">Sign In</Button></Link>
              <Link href="/register"><Button size="sm" className="rounded-full">Get Started</Button></Link>
            </div>
          )}

          <button className="md:hidden p-2" onClick={() => setMobileOpen(!mobileOpen)}>
            {mobileOpen ? <X /> : <Menu />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="md:hidden border-t bg-background p-4 space-y-4 animate-in slide-in-from-top">
          {navLinks.map(link => (
            <Link key={link.href} href={link.href} onClick={() => setMobileOpen(false)} className="block py-2 font-medium">{link.label}</Link>
          ))}
          <div className="pt-4 border-t space-y-2">
            {isAuthenticated ? (
              <>
                <Link href="/dashboard" onClick={() => setMobileOpen(false)} className="flex items-center gap-2 py-2"><LayoutDashboard className="w-4 h-4" /> Dashboard</Link>
                <Link href="/my-bookings" onClick={() => setMobileOpen(false)} className="flex items-center gap-2 py-2"><Ticket className="w-4 h-4" /> My Bookings</Link>
                <Link href="/wallet" onClick={() => setMobileOpen(false)} className="flex items-center gap-2 py-2"><Wallet className="w-4 h-4" /> Wallet</Link>
                <Link href="/profile" onClick={() => setMobileOpen(false)} className="flex items-center gap-2 py-2"><User className="w-4 h-4" /> Profile</Link>
                <button onClick={() => { logout(); setMobileOpen(false) }} className="flex items-center gap-2 py-2 text-destructive"><LogOut className="w-4 h-4" /> Logout</button>
              </>
            ) : (
              <>
                <Link href="/login" onClick={() => setMobileOpen(false)}><Button variant="outline" className="w-full">Sign In</Button></Link>
                <Link href="/register" onClick={() => setMobileOpen(false)}><Button className="w-full">Get Started</Button></Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  )
}
