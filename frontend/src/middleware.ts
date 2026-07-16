
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

// Simple auth middleware – checks token presence. Real JWT verification would happen server-side.
// For production, use jose to verify JWT in middleware.
const protectedRoutes = ['/dashboard', '/my-bookings', '/wallet', '/profile', '/booking', '/track', '/notifications', '/driver', '/company', '/admin', '/boarding-pass']

export function middleware(request: NextRequest) {
  const token = request.cookies.get('ghanaride_token')?.value || request.headers.get('authorization')
  const path = request.nextUrl.pathname
  const isProtected = protectedRoutes.some(r => path.startsWith(r))

  // If frontend stores token in localStorage, middleware cannot read it – hence we allow and let client-side guard handle.
  // Here we only check if route is protected and token missing from cookie (if using cookie auth)
  // For now, pass through and handle client side.
  return NextResponse.next()
}

export const config = { matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'] }
