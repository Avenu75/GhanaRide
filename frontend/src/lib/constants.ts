export const GHANA_STATIONS: Record<string, { lat: number; lng: number }> = {
  'Accra - Circle': { lat: 5.5560, lng: -0.1969 },
  'Accra - Kaneshie': { lat: 5.5653, lng: -0.2378 },
  'Accra - Tema Station': { lat: 5.5495, lng: -0.2050 },
  'Kumasi - Kejetia': { lat: 6.6885, lng: -1.6244 },
  'Kumasi - Adum': { lat: 6.6923, lng: -1.6300 },
  'Kumasi - Tech Junction': { lat: 6.6770, lng: -1.5730 },
  'Cape Coast - Kotokoraba': { lat: 5.1053, lng: -1.2466 },
  'Takoradi - Market Circle': { lat: 4.8845, lng: -1.7554 },
  'Tamale - Central': { lat: 9.4008, lng: -0.8393 },
  'Koforidua - Central': { lat: 6.0904, lng: -0.2590 },
  'Ho - Central': { lat: 6.6008, lng: 0.4713 },
  'Sunyani - Central': { lat: 7.3398, lng: -2.3268 },
  'Bolgatanga - Central': { lat: 10.7856, lng: -0.8514 },
  'Wa - Central': { lat: 10.0601, lng: -2.5090 },
  'Nkawkaw - Station': { lat: 6.5560, lng: -0.7670 },
}

export const GHANA_BOUNDS = {
  latMin: 4.5,
  latMax: 11.5,
  lngMin: -3.5,
  lngMax: 1.5,
}

export const BOOKING_STATUS_COLORS: Record<string, string> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  CONFIRMED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200',
  PAID: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  COMPLETED: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  EXPIRED: 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200',
  NO_SHOW: 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200',
}

export const TRIP_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-red-100 text-red-800',
  FULL: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-gray-100 text-gray-800',
  FAILED_TO_SHOW: 'bg-orange-100 text-orange-800',
  EXPIRED: 'bg-zinc-100 text-zinc-800',
}

export const PAYMENT_METHODS = [
  { id: 'WALLET', label: 'GhanaRide Wallet', icon: 'Wallet', description: 'Pay with your wallet balance' },
  { id: 'PAYSTACK', label: 'Card / Mobile Money', icon: 'CreditCard', description: 'MTN MoMo, Vodafone Cash, Card' },
  { id: 'CASH', label: 'Pay on Board', icon: 'Banknote', description: 'Pay driver when boarding' },
] as const

export const CANCEL_REASONS = [
  'Change of plans',
  'Found alternative transport',
  'Emergency',
  'Trip delayed',
  'Driver requested cancellation',
  'Overbooked / Duplicate booking',
  'Other',
]

export const DRIVER_CANCEL_REASONS = [
  'Vehicle breakdown',
  'Driver emergency',
  'Road condition',
  'Low passenger turn-up',
  'Weather condition',
  'Overbooked',
  'Other - Operational',
]
