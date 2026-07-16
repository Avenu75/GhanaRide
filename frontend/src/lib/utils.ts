import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number | string, currency = "GHS") {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  return new Intl.NumberFormat('en-GH', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(num).replace('GHS', 'GH₵')
}

export function formatDate(date: string | Date, format: 'short' | 'long' | 'relative' = 'short') {
  const d = typeof date === 'string' ? new Date(date) : date
  if (format === 'short') {
    return new Intl.DateTimeFormat('en-GH', { dateStyle: 'medium', timeStyle: 'short' }).format(d)
  }
  if (format === 'long') {
    return new Intl.DateTimeFormat('en-GH', { dateStyle: 'full', timeStyle: 'long' }).format(d)
  }
  // relative
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours}h ago`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) return `${diffDays}d ago`
  return new Intl.DateTimeFormat('en-GH', { dateStyle: 'medium' }).format(d)
}

export function generateBookingRef() {
  return `GR-${Math.random().toString(36).substring(2,8).toUpperCase()}`
}

export function truncate(str: string, len: number) {
  return str.length > len ? str.slice(0, len) + '...' : str
}

export function getInitials(name: string) {
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0,2)
}

export const GHANA_CITIES = [
  'Accra', 'Kumasi', 'Takoradi', 'Tamale', 'Cape Coast', 'Koforidua',
  'Ho', 'Sunyani', 'Bolgatanga', 'Wa', 'Techiman', 'Tema', 'Obuasi',
  'Kasoa', 'Madina', 'Achimota', 'East Legon', 'Legon', 'Kumasi Central',
  'Kejetia', 'Adum', 'Circle', 'Kaneshie', 'Nkawkaw', 'Konongo', 'Ejisu'
]

export function isValidGhanaPhone(phone: string) {
  // Ghana phone: +233 or 0 + 9 digits
  return /^(?:\+233|0)(?:20|24|27|28|54|55|59|50)[0-9]{7}$/.test(phone.replace(/\s/g,''))
}
