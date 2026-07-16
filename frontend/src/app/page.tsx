'use client'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { MapPin, Shield, Clock, Wallet, Star, ArrowRight, Bus, Users, Award } from 'lucide-react'
import { motion } from 'framer-motion'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { GHANA_CITIES } from '@/lib/utils'

export default function LandingPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const router = useRouter()

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const params = new URLSearchParams()
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    router.push(`/rides?${params.toString()}`)
  }

  return (
    <div className="flex flex-col">
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-zinc-950 via-zinc-900 to-zinc-950 text-white">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(252,209,22,0.15),transparent_50%)]" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_80%,rgba(206,17,38,0.15),transparent_50%)]" />
        <div className="container mx-auto px-4 py-20 md:py-28 relative">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }} className="space-y-6">
              <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-sm backdrop-blur">
                <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                Ghana&apos;s #1 Intercity Platform
              </div>
              <h1 className="text-5xl md:text-6xl font-bold leading-[1.1] tracking-tight">
                Travel Ghana with <span className="text-primary">Confidence</span>
              </h1>
              <p className="text-lg text-zinc-300 leading-relaxed">
                Book verified buses and shared rides across Ghana. Safe, affordable, and built for Ghanaian travelers. MTN MoMo, Vodafone Cash supported.
              </p>

              {/* Search Card */}
              <Card className="bg-white text-zinc-900 shadow-2xl border-0 rounded-[20px] overflow-hidden">
                <CardContent className="p-6">
                  <form onSubmit={handleSearch} className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-xs font-semibold uppercase tracking-wider text-zinc-500">From</label>
                        <div className="relative">
                          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-400" />
                          <select value={from} onChange={e => setFrom(e.target.value)} className="w-full h-12 pl-10 pr-4 rounded-xl border bg-zinc-50 focus:bg-white focus:ring-2 focus:ring-primary outline-none transition">
                            <option value="">Select origin</option>
                            {GHANA_CITIES.map(c => <option key={c} value={c}>{c}</option>)}
                          </select>
                        </div>
                      </div>
                      <div className="space-y-2">
                        <label className="text-xs font-semibold uppercase tracking-wider text-zinc-500">To</label>
                        <div className="relative">
                          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-400" />
                          <select value={to} onChange={e => setTo(e.target.value)} className="w-full h-12 pl-10 pr-4 rounded-xl border bg-zinc-50 focus:bg-white focus:ring-2 focus:ring-primary outline-none transition">
                            <option value="">Select destination</option>
                            {GHANA_CITIES.map(c => <option key={c} value={c}>{c}</option>)}
                          </select>
                        </div>
                      </div>
                    </div>
                    <Button type="submit" size="lg" className="w-full rounded-xl h-12 text-base">Search Rides <ArrowRight className="ml-2 w-4 h-4" /></Button>
                  </form>
                </CardContent>
              </Card>

              <div className="flex items-center gap-6 text-sm">
                <span className="flex items-center gap-2"><Users className="w-4 h-4 text-primary" /> 10k+ travelers</span>
                <span className="flex items-center gap-2"><Star className="w-4 h-4 text-yellow-400 fill-yellow-400" /> 4.9 rating</span>
                <span className="flex items-center gap-2"><Shield className="w-4 h-4 text-green-400" /> Verified drivers</span>
              </div>
            </motion.div>

            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.6, delay: 0.2 }} className="relative hidden md:block">
              <div className="relative rounded-[32px] overflow-hidden shadow-2xl aspect-[4/3] bg-zinc-800">
                <img src="https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800&q=80" alt="GhanaRide" className="w-full h-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                <div className="absolute bottom-6 left-6 right-6">
                  <div className="bg-white/95 backdrop-blur rounded-2xl p-4 flex items-center justify-between">
                    <div><p className="text-sm text-zinc-500">Next departure</p><p className="font-bold">Accra → Kumasi • 6:30 AM</p></div>
                    <div className="w-12 h-12 rounded-xl ghana-gradient flex items-center justify-center text-white"><Bus className="w-6 h-6" /></div>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-20 bg-background">
        <div className="container mx-auto px-4">
          <div className="text-center mb-12 max-w-2xl mx-auto">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">Why 10,000+ Ghanaians trust GhanaRide</h2>
            <p className="text-muted-foreground">We built the most reliable transport booking experience for Ghana, inspired by Uber, Bolt, and Airbnb.</p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {[
              { icon: Shield, title: 'Verified Drivers', desc: 'Every driver passes strict background checks and vehicle inspection.' },
              { icon: Clock, title: 'On-Time Guarantee', desc: 'Real-time tracking and departure reminders keep you in control.' },
              { icon: Wallet, title: 'Pay Your Way', desc: 'Wallet, MTN MoMo, Vodafone Cash, Card, or Cash – all in GHS.' },
            ].map((f, i) => (
              <Card key={i} className="rounded-[24px] border-0 shadow-sm bg-muted/50 hover-lift">
                <CardContent className="p-8">
                  <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center mb-5"><f.icon className="w-7 h-7 text-primary" /></div>
                  <h3 className="font-semibold text-lg mb-2">{f.title}</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">{f.desc}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="py-20 bg-zinc-950 text-white">
        <div className="container mx-auto px-4">
          <div className="grid md:grid-cols-3 gap-8">
            {[
              { step: '01', title: 'Search & Compare', desc: 'Enter your route and date. Compare verified trips instantly.' },
              { step: '02', title: 'Choose Seat & Pay', desc: 'Pick your seat on visual seat map. Pay with MoMo or Wallet.' },
              { step: '03', title: 'Board with QR', desc: 'Show QR boarding pass. Track your bus live to destination.' },
            ].map(s => (
              <div key={s.step} className="space-y-3">
                <div className="text-7xl font-bold text-zinc-800">{s.step}</div>
                <h3 className="text-xl font-semibold">{s.title}</h3>
                <p className="text-zinc-400">{s.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20">
        <div className="container mx-auto px-4">
          <div className="rounded-[32px] ghana-gradient p-10 md:p-16 text-white relative overflow-hidden">
            <div className="absolute inset-0 bg-black/20" />
            <div className="relative grid md:grid-cols-2 gap-8 items-center">
              <div>
                <h2 className="text-3xl md:text-4xl font-bold mb-4">Ready to ride with Ghana?</h2>
                <p className="text-white/80 mb-6">Join thousands of happy travelers. Book in 30 seconds.</p>
                <div className="flex gap-3">
                  <Link href="/register"><Button variant="secondary" size="lg" className="rounded-full bg-white text-black hover:bg-zinc-100">Start Booking</Button></Link>
                  <Link href="/register/driver"><Button variant="outline" size="lg" className="rounded-full border-white text-white hover:bg-white/10">Drive & Earn</Button></Link>
                </div>
              </div>
              <div className="hidden md:flex justify-end">
                <div className="flex -space-x-3"><div className="w-12 h-12 rounded-full bg-white/20 border-2 border-white/30" /><div className="w-12 h-12 rounded-full bg-white/30 border-2 border-white/30" /><div className="w-12 h-12 rounded-full bg-white/40 border-2 border-white/30" /><div className="w-12 h-12 rounded-full bg-white flex items-center justify-center text-black font-bold text-sm">+10k</div></div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
