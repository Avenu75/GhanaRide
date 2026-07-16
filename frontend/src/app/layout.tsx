import type { Metadata } from 'next'
import './globals.css'
import { Navbar } from '@/components/layout/navbar'
import { Footer } from '@/components/layout/footer'
import { Toaster } from 'sonner'
import { ThemeProvider } from 'next-themes'

export const metadata: Metadata = {
  title: {
    default: 'GhanaRide - Premium Intercity Booking',
    template: '%s | GhanaRide',
  },
  description: "Ghana's premium intercity booking super-app. Safe, reliable, built for Ghana.",
  keywords: ['Ghana', 'Transport', 'Booking', 'Bus', 'Intercity', 'Ride'],
  authors: [{ name: 'GhanaRide' }],
  openGraph: {
    type: 'website',
    locale: 'en_GH',
    siteName: 'GhanaRide',
  },
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700&family=Inter:wght@400;500;600&display=swap" rel="stylesheet" />
      </head>
      <body className="min-h-screen flex flex-col">
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Footer />
          <Toaster richColors position="top-right" />
        </ThemeProvider>
      </body>
    </html>
  )
}
