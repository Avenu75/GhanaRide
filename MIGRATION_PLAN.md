# GhanaRide Frontend Migration to Next.js — Complete Analysis & Roadmap

**Date:** 2026-07-16
**Source:** Avenu75/ghanaride (Spring Boot + Thymeleaf)
**Target:** Spring Boot (REST) + MySQL + Next.js 15 + TypeScript + Tailwind + shadcn/ui

---

## 1. Backend Analysis

### 1.1 Tech Stack (Current)
- Java 17, Spring Boot 3.x, Spring Security, OAuth2 (Google)
- Thymeleaf + Bootstrap 5, MySQL 8
- Paystack payments (GHS currency)
- WebSocket for real-time
- Caffeine caching, Actuator

### 1.2 Entities (Database Schema)
```
users (id, username, full_name, email, password, phone, role [USER/DRIVER/COMPANY/ADMIN], account_type, dob, gender, address, profile_image, license_*, vehicle_*, company_*, email_verified, enabled, account_locked, created_at...)
cars (id, driver_id, company_id, plate_number unique, car_brand, model, year, color, total_seats, vin, status [ACTIVE/MAINTENANCE/INACTIVE/RETIRED/PENDING_DOCS/SUSPENDED], image_path, created_at)
companies (extension? Actually Company entity exists, but User has company fields – dual model. Need to normalize. Entity Company.java likely separate. Car can belong to Company)
trips (id, car_id, driver_id, company_id, from_location, to_location, pickup_station, drop_off_station, departure_time, arrival_time, trip_amount, available_seats, total_seats, description, status [PENDING/APPROVED/REJECTED/CANCELLED/FULL/COMPLETED/FAILED_TO_SHOW/EXPIRED], cancel_reason, cancelled_by, created_at, expired_at)
bookings (id, user_id, trip_id, seat_map_id, booking_reference unique GR-XXXX, seat_number, booking_date, status [PENDING_PAYMENT/ACTIVE/CONFIRMED/PAID/COMPLETED/CANCELLED/EXPIRED/NO_SHOW], total_amount, booking_type [SELF/RELATIVE], passenger_name, passenger_phone, payment_method [WALLET/PAYSTACK/CASH/...], payment_status [PENDING/SUCCESS/FAILED/REFUNDED], transaction_reference, cancel_reason_details, cancelled_by, cancelled_at)
seat_maps (id, trip_id, car_id, seat_number, row_number, column_label, seat_type [REGULAR/WINDOW/AISLE/DRIVER/EXTRA], status [AVAILABLE/HELD/BOOKED/BLOCKED], extra_legroom, held_until, booking_id)
wallets (id, user_id, balance, ...)
wallet_transactions (id, wallet_id, booking_id, amount, type [TOPUP/PAYMENT/REFUND/WITHDRAWAL], provider, status, created_at)
notifications (id, user_id, type [BOOKING_CONFIRMED/BOOKING_CANCELLED/TRIP_CANCELLED/PAYMENT_SUCCESS/etc], title, message, link, is_read, created_at)
reviews (id, passenger_id, driver_id, trip_id, booking_id, rating 1-5, comment)
coupons, verification_tokens, password_reset_tokens, login_attempts
```

### 1.3 Controllers (Current MVC)
- **HomeController**: `/`, `/landing`, `/dashboard`, `/rides`, search trips
- **AuthController**: `/login`, `/register`, `/register/driver`, `/register/company`, OAuth2 flow
- **BookingController**: `/booking/{tripId}` GET+POST, `/my-bookings`, `/booking/receipt/{id}`, `/booking/{id}/cancel`, `/boarding-pass/{bookingId}`
- **DriverController**: `/driver/dashboard`, `/driver/add-trip`, `/driver/trip/{id}/passengers`, `/driver/trips`, vehicle management
- **CompanyController**: `/company/dashboard`, `/company/add-trip`, `/company/vehicles`, `/company/trip/{id}/passengers`
- **AdminController**: `/admin/dashboard`, `/admin/trips` (approve/reject/fail/delete), `/admin/users` (search/delete), `/admin/bookings`, `/admin/cars/{id}/delete`
- **ProfileController**: `/profile`, update profile, avatar, password, notifications
- **PaymentController**: Paystack init/verify/webhook
- **WalletController**: `/wallet`, `/wallet/topup`
- **NotificationController**: list, mark read, delete
- **TrackController**: `/track/{bookingId}` – live GPS + Google Maps / OSM fallback
- **RealTimeController**: WebSocket chat/location
- **ReviewController**: POST `/reviews`
- **SeatApiController**: `/api/trips/{tripId}/seats`, `/api/trips/{tripId}/live` – already REST!
- **PasswordResetController**, **PageController**: static pages `/about`, `/contact`, `/faq`, `/privacy`, `/terms`, `/refunds`

### 1.4 Security Analysis
- Form login + OAuth2 Google, session cookie `GHANARIDE_SESSION`, HttpOnly, Secure, SameSite=Lax
- RememberMe 30 days
- Role-based: USER, DRIVER, COMPANY, ADMIN
- CustomAuthenticationSuccessHandler redirects by role
- CustomAuthenticationFailureHandler handles lockout after 5 attempts / 15 min
- Interceptors: GhanaOnlyConfig (IP geofence), ProfileCompletionInterceptor (phone required)
- Current weakness: No JWT for API – session doesn't work well for SPA. Must add JWT.

### 1.5 Services – Business Logic to Preserve
- BookingService: seat check, GR-XXX reference, decrement seats, duplicate booking check, cancellable 2h before departure, refund logic
- TripService: search (from/to/date), approve/reject flow, expiration scheduler, automatic cleanup
- PaymentService: cash, wallet, Paystack verify, webhook signature validation
- WalletService: getOrCreate, topup, pay, refund, history
- SeatService: ensureSeatMap generation, hold/confirm/release, expired release
- UserService: registerPassenger/Driver/Company, profile complete, avatar, password change, search
- NotificationService: push all event types
- FileStorageService: car images, profile, driver/company docs
- TripExpirationScheduler: expire old trips, departure reminders

### 1.6 Existing Frontend Pages (Thymeleaf)
Landing, Index, Login, Register (3 variants), Forgot/Reset Password, Dashboard (user), Search Trips (rides.html), Booking Confirm, My Bookings, Receipt, Boarding Pass (QR), Wallet, Track (live map), Profile, Notifications, Payment, About, Contact, FAQ, Privacy, Terms, Refunds, Driver: dashboard, add-trip, trip-passengers, Company: dashboard, add-trip, vehicles, trip-passengers, Admin: dashboard, trips, users, bookings, Error 403/404/500, Fragments.

---

## 2. Migration Strategy

### 2.1 Philosophy
**Backend = Source of Truth**. No business logic in frontend. Frontend only calls REST APIs. Keep all validations server-side but mirror UX validations client-side.

### 2.2 Backend Refactoring (Required)
Create new package `com.ghanaride.api` with REST controllers returning JSON.

**Authentication:**
- Keep existing form login for backward compat
- Add JWT filter: `JwtAuthenticationFilter` + `JwtTokenProvider`
- Endpoints:
  - `POST /api/auth/register` → JSON
  - `POST /api/auth/login` → returns JWT + refresh token
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
  - `POST /api/auth/oauth/google` – exchange id_token for JWT
  - `POST /api/auth/forgot-password`, `/api/auth/reset-password`

**DTOs for API:**
- Create `com.ghanaride.dto.response` – ApiResponse<T>, PagedResponse<T>, JwtResponse, UserDTO, TripDTO, BookingDTO, CarDTO, WalletDTO, NotificationDTO, SeatMapDTO
- Never expose entities directly

**New REST Controllers:**
- `/api/trips` – search, get, list, driver create
- `/api/bookings` – create, my bookings, cancel, receipt data
- `/api/users` – profile, avatar upload
- `/api/driver/trips`, `/api/company/...`, `/api/admin/...`
- `/api/wallet`, `/api/notifications`, `/api/payments/paystack/*`
- `/api/seats` keep existing
- Keep MVC controllers but mark @Deprecated or under feature flag `useThymeleaf=false`

**CORS:**
- Allow `http://localhost:3000`, production frontend URL
- Allow credentials

**File Upload:**
- Serve via `/api/files/**` or keep static

### 2.3 Frontend Architecture (Next.js)

**Stack:**
- Next.js 15 App Router, TypeScript strict, Tailwind CSS 3.4, shadcn/ui (Radix + Tailwind), Lucide React, Framer Motion for animations, React Query / tanstack-query for data fetching, Zustand for auth state, next-themes for dark mode, React Hook Form + Zod validation, Sonner for toasts, date-fns.

**Folder Structure:**
```
src/
  app/
    (public)/               # Public routes: landing, about, contact, faq, etc.
      page.tsx (landing)
      about/page.tsx
      contact/page.tsx
      privacy/page.tsx
      terms/page.tsx
      refunds/page.tsx
      faq/page.tsx
    (auth)/
      login/page.tsx
      register/page.tsx
      register/driver/page.tsx
      register/company/page.tsx
      forgot-password/page.tsx
      reset-password/page.tsx
    (dashboard)/
      dashboard/page.tsx           # USER dashboard /rides search
      rides/page.tsx               # Search results
      booking/[tripId]/page.tsx
      booking/receipt/[id]/page.tsx
      boarding-pass/[bookingId]/page.tsx
      my-bookings/page.tsx
      track/[bookingId]/page.tsx
      wallet/page.tsx
      notifications/page.tsx
      profile/page.tsx
    driver/
      dashboard/page.tsx
      add-trip/page.tsx
      trips/[id]/passengers/page.tsx
    company/
      dashboard/page.tsx
      add-trip/page.tsx
      vehicles/page.tsx
      trips/[id]/passengers/page.tsx
    admin/
      dashboard/page.tsx
      trips/page.tsx
      users/page.tsx
      bookings/page.tsx
    layout.tsx
    globals.css
  components/
    ui/ (button, card, input, label, badge, dialog, dropdown, skeleton, table, tabs, etc.)
    layout/ (navbar, footer, sidebar, mobile-nav)
    booking/ (seat-map, trip-card, booking-form, receipt)
    trips/ (search-filters, trip-list)
    wallet/ (balance-card, transaction-list)
    maps/ (live-track-map with OSM fallback + Google Maps)
    auth/ (login-form, register-forms)
    common/ (page-header, empty-state, loading, error-boundary)
  lib/
    api/ (client.ts axios/fetch wrapper, endpoints.ts typed)
    auth.ts
    utils.ts (cn, formatCurrency GHS, formatDate, Ghana stations)
    constants.ts (Ghana cities, stations coords)
    validations/ (auth.schema, booking.schema, trip.schema)
  hooks/
    use-auth.ts
    use-trips.ts
    use-bookings.ts
    use-wallet.ts
    use-notifications.ts
    use-debounce.ts
  contexts/
    auth-context.tsx
    theme-context.tsx
  types/
    index.ts (all DTO types mirroring backend)
  services/
    trip.service.ts
    booking.service.ts
    etc.
  store/
    auth.store.ts (zustand)
```

**API Layer Design:**
- `lib/api/client.ts` – fetch wrapper with baseUrl from env NEXT_PUBLIC_API_URL, attaches JWT from localStorage/cookie, handles 401 refresh, returns typed.
- `lib/api/endpoints.ts` – all endpoint definitions.
- Preserve every feature via service functions.

**Auth Flow (JWT):**
- On login, backend returns { token, refreshToken, user }
- Store token in httpOnly? For SPA we store in localStorage + memory, refresh token in secure cookie. Next.js middleware checks JWT for protected routes.
- Role-based middleware: reads JWT payload, redirects if unauthorized.
- OAuth: Google Identity Services on frontend, send id_token to backend `/api/auth/oauth/google`, backend verifies and returns JWT.

**Key UX Improvements:**
- Skeleton loading for all lists
- Toast notifications (sonner)
- Optimistic UI for booking
- Responsive tables with mobile cards
- Better form validation (zod)
- Search filters: from, to, date, price range, seats, sorting
- Dark mode (next-themes)
- Mobile-first: bottom nav, swipeable trip cards, drawer for filters
- Accessibility: proper aria, keyboard nav
- Animations: framer-motion page transitions, card hover lift

### 2.4 Feature Preservation Checklist
- [ ] Trip search (from, to, date, Ghana-only validation)
- [ ] Booking (seat selection, self/relative, payment method wallet/paystack/cash)
- [ ] Booking restrictions (own trip cannot book, duplicate booking, phone required, 2h cancellable)
- [ ] Driver registration with license/id upload
- [ ] Company registration with cert upload
- [ ] Vehicle management (add, list, status, image)
- [ ] Profile management (edit, avatar, password, notification prefs)
- [ ] Driver withdrawals (wallet)
- [ ] Paystack integration (init, verify, webhook)
- [ ] Booking receipts (PDF generation)
- [ ] Booking cancellation with reason
- [ ] Driver cancellation reasons
- [ ] Notifications (real-time via websocket + polling fallback)
- [ ] Trip history
- [ ] Automatic trip cleanup (backend scheduler stays)
- [ ] Admin management (approve/reject trips, users, bookings, cars)
- [ ] Boarding pass QR code
- [ ] Live tracking with OSM/Google Maps fallback
- [ ] Reviews (passenger → driver)
- [ ] Wallet topup, history, recent
- [ ] Ghana-only IP check
- [ ] Seat map visual
- [ ] Reviews, contact form

### 2.5 Migration Order (One feature at a time)
1. Setup Next.js project + design system + API client + auth context
2. Auth pages (login, register user/driver/company) + protected route middleware
3. Landing + public pages (about, contact, etc.)
4. Trip search & listing (rides)
5. Booking flow (seat selection, confirm, payment)
6. User dashboard & My Bookings & Receipt & Boarding Pass
7. Profile & Wallet & Notifications & Track
8. Driver dashboard + add trip + passengers
9. Company dashboard + vehicles + add trip
10. Admin dashboards (trips, users, bookings)
11. Payment + webhook + PDF
12. Final polish: dark mode, skeleton, animations, mobile, a11y, SEO

### 2.6 Environment Variables
Backend:
- SPRING_DATASOURCE_URL, USERNAME, PASSWORD
- MAIL_*, APP_BASE_URL, GOOGLE_*, PAYSTACK_*, JWT_SECRET, etc.
Frontend:
- NEXT_PUBLIC_API_URL=http://localhost:8088/api
- NEXT_PUBLIC_APP_URL=http://localhost:3000
- NEXT_PUBLIC_GOOGLE_CLIENT_ID
- NEXT_PUBLIC_GOOGLE_MAPS_API_KEY
- NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY

---

## 3. Risks & Mitigations
- Session vs JWT mismatch → Build dual auth support, CORS enabled, cookie sameSite=Lax
- File upload → Use multipart/form-data via API, keep uploads folder served
- GhanaOnly interceptor blocks Next.js dev → Whitelist localhost, or make frontend pass X-Forwarded-For header mock
- Paystack webhook needs public URL → Use ngrok for testing
- Real-time WebSocket → Keep STOMP over SockJS, connect from Next.js

---

## 4. Success Criteria
- All Thymeleaf pages recreated in Next.js with improved UI
- No business logic duplicated
- All roles functional
- Mobile responsive, Lighthouse >90
- Production build passes, no TS errors
- Backend tests still pass

---

END OF PLAN
