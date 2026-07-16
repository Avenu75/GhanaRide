import Link from 'next/link'
import { Bus, Mail, Phone, MapPin, Facebook, Twitter, Instagram } from 'lucide-react'

export function Footer() {
  return (
    <footer className="bg-zinc-950 text-zinc-200">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="space-y-4">
            <Link href="/" className="flex items-center gap-2 font-bold text-xl text-white">
              <div className="w-9 h-9 rounded-xl ghana-gradient flex items-center justify-center"><Bus className="w-5 h-5" /></div>
              <span>Ghana<span className="text-primary">Ride</span></span>
            </Link>
            <p className="text-sm text-zinc-400 leading-relaxed">Ghana&apos;s premium intercity booking super-app. Safe, reliable, and built for Ghana.</p>
            <div className="flex gap-3">
              <a href="#" className="w-9 h-9 rounded-full bg-zinc-800 flex items-center justify-center hover:bg-zinc-700"><Facebook className="w-4 h-4" /></a>
              <a href="#" className="w-9 h-9 rounded-full bg-zinc-800 flex items-center justify-center hover:bg-zinc-700"><Twitter className="w-4 h-4" /></a>
              <a href="#" className="w-9 h-9 rounded-full bg-zinc-800 flex items-center justify-center hover:bg-zinc-700"><Instagram className="w-4 h-4" /></a>
            </div>
          </div>

          <div>
            <h4 className="font-semibold mb-4">Company</h4>
            <ul className="space-y-2 text-sm text-zinc-400">
              <li><Link href="/about" className="hover:text-white">About Us</Link></li>
              <li><Link href="/contact" className="hover:text-white">Contact</Link></li>
              <li><Link href="/faq" className="hover:text-white">FAQ</Link></li>
              <li><Link href="/privacy" className="hover:text-white">Privacy Policy</Link></li>
              <li><Link href="/terms" className="hover:text-white">Terms</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold mb-4">Services</h4>
            <ul className="space-y-2 text-sm text-zinc-400">
              <li><Link href="/rides" className="hover:text-white">Find Trips</Link></li>
              <li><Link href="/register/driver" className="hover:text-white">Become a Driver</Link></li>
              <li><Link href="/register/company" className="hover:text-white">Company Partnership</Link></li>
              <li><Link href="/refunds" className="hover:text-white">Refunds</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold mb-4">Get in Touch</h4>
            <ul className="space-y-3 text-sm text-zinc-400">
              <li className="flex items-start gap-2"><MapPin className="w-4 h-4 mt-0.5" /> 123 Transport Ave, East Legon, Accra</li>
              <li className="flex items-center gap-2"><Mail className="w-4 h-4" /> support@ghanaride.me</li>
              <li className="flex items-center gap-2"><Phone className="w-4 h-4" /> +233 20 123 4567</li>
            </ul>
          </div>
        </div>
        <div className="mt-12 pt-8 border-t border-zinc-800 flex flex-col md:flex-row justify-between items-center gap-4 text-sm text-zinc-500">
          <p>© {new Date().getFullYear()} GhanaRide. All rights reserved. Built for Ghana 🇬🇭</p>
          <p>Pay with MTN MoMo • Vodafone Cash • Card • Wallet</p>
        </div>
      </div>
    </footer>
  )
}
