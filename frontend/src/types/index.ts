export type Role = 'USER' | 'DRIVER' | 'COMPANY' | 'ADMIN'

export type BookingStatus = 'PENDING_PAYMENT' | 'ACTIVE' | 'CONFIRMED' | 'PAID' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED' | 'NO_SHOW'
export type TripStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'FULL' | 'COMPLETED' | 'FAILED_TO_SHOW' | 'EXPIRED'
export type PaymentMethod = 'WALLET' | 'PAYSTACK' | 'CASH' | 'MOMO' | 'VODAFONE_CASH'
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'
export type CarStatus = 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE' | 'RETIRED' | 'PENDING_DOCS' | 'SUSPENDED'

export interface User {
  id: number
  username: string
  fullName: string
  email: string
  phoneNumber?: string
  role: Role
  accountType?: string
  dateOfBirth?: string
  gender?: string
  address?: string
  profileImagePath?: string
  licenseNumber?: string
  vehiclePlate?: string
  vehicleModel?: string
  companyName?: string
  emailVerified: boolean
  enabled: boolean
  createdAt: string
  lastLoginAt?: string
  isProfileComplete?: boolean
}

export interface Car {
  id: number
  plateNumber: string
  carBrand: string
  model: string
  year?: number
  color?: string
  totalSeats: number
  vin?: string
  status: CarStatus
  imagePath?: string
  description?: string
  driver?: User
  company?: Company
  createdAt: string
}

export interface Company {
  id: number
  name: string
  email: string
  phone: string
  location: string
  description: string
  registrationNo?: string
  status: string
}

export interface Trip {
  id: number
  fromLocation: string
  toLocation: string
  pickupStation?: string
  dropOffStation?: string
  departureTime: string
  arrivalTime?: string
  tripAmount: number
  availableSeats: number
  totalSeats: number
  description?: string
  status: TripStatus
  car: Car
  driver?: User
  company?: Company
  createdAt: string
  bookingsCount?: number
}

export interface Seat {
  seatNumber: string
  row: number
  col: string
  type: 'REGULAR' | 'WINDOW' | 'AISLE' | 'DRIVER'
  status: 'AVAILABLE' | 'HELD' | 'BOOKED' | 'BLOCKED'
  extraLegroom: boolean
}

export interface Booking {
  id: number
  bookingReference: string
  seatNumber?: number
  displaySeat?: string
  bookingDate: string
  status: BookingStatus
  totalAmount: number
  bookingType: 'SELF' | 'RELATIVE'
  passengerName?: string
  passengerPhone?: string
  paymentMethod?: PaymentMethod
  paymentStatus: PaymentStatus
  transactionReference?: string
  cancelReason?: string
  cancelReasonDetails?: string
  trip: Trip
  user: User
  seatMap?: Seat
  isCancellable?: boolean
}

export interface Wallet {
  id: number
  balance: number
  userId: number
  createdAt: string
  updatedAt: string
}

export interface WalletTransaction {
  id: number
  amount: number
  type: 'TOPUP' | 'PAYMENT' | 'REFUND' | 'WITHDRAWAL'
  provider?: string
  status: string
  bookingReference?: string
  createdAt: string
  walletId: number
}

export interface Notification {
  id: number
  type: string
  title: string
  message: string
  link?: string
  read: boolean
  createdAt: string
}

export interface Review {
  id: number
  rating: number
  comment?: string
  passenger: User
  driver: User
  trip: Trip
  createdAt: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

export interface JwtResponse {
  token: string
  refreshToken: string
  user: User
}

export interface TripSearchParams {
  from?: string
  to?: string
  date?: string
  passengers?: number
  minPrice?: number
  maxPrice?: number
  sortBy?: 'departure' | 'price' | 'seats'
}
