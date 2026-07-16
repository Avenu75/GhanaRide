export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REGISTER_DRIVER: '/auth/register/driver',
    REGISTER_COMPANY: '/auth/register/company',
    ME: '/auth/me',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
    GOOGLE: '/auth/oauth/google',
    FORGOT_PASSWORD: '/auth/forgot-password',
    RESET_PASSWORD: '/auth/reset-password',
  },
  TRIPS: {
    LIST: '/trips',
    SEARCH: '/trips/search',
    DETAIL: (id: number | string) => `/trips/${id}`,
    CREATE: '/trips',
    MY_TRIPS: '/trips/my',
    APPROVE: (id: number) => `/trips/${id}/approve`,
    REJECT: (id: number) => `/trips/${id}/reject`,
  },
  BOOKINGS: {
    CREATE: (tripId: number) => `/bookings/trip/${tripId}`,
    MY: '/bookings/my',
    DETAIL: (id: number) => `/bookings/${id}`,
    CANCEL: (id: number) => `/bookings/${id}/cancel`,
    RECEIPT: (id: number) => `/bookings/${id}/receipt`,
    BOARDING_PASS: (id: number) => `/bookings/${id}/boarding-pass`,
  },
  SEATS: {
    MAP: (tripId: number) => `/trips/${tripId}/seats`,
    LIVE: (tripId: number) => `/trips/${tripId}/live`,
  },
  USER: {
    PROFILE: '/users/me',
    UPDATE_PROFILE: '/users/me',
    AVATAR: '/users/me/avatar',
    CHANGE_PASSWORD: '/users/me/password',
  },
  WALLET: {
    GET: '/wallet',
    TOPUP: '/wallet/topup',
    HISTORY: '/wallet/history',
    RECENT: '/wallet/recent',
  },
  NOTIFICATIONS: {
    LIST: '/notifications',
    UNREAD_COUNT: '/notifications/unread-count',
    MARK_READ: (id: number) => `/notifications/${id}/read`,
    MARK_ALL_READ: '/notifications/read-all',
  },
  CARS: {
    MY: '/cars/my',
    CREATE: '/cars',
    DELETE: (id: number) => `/cars/${id}`,
  },
  PAYMENTS: {
    PAYSTACK_INIT: '/payments/paystack/initialize',
    PAYSTACK_VERIFY: '/payments/paystack/verify',
  },
  ADMIN: {
    DASHBOARD: '/admin/dashboard',
    USERS: '/admin/users',
    TRIPS: '/admin/trips',
    BOOKINGS: '/admin/bookings',
  },
  DRIVER: {
    DASHBOARD: '/driver/dashboard',
    TRIPS: '/driver/trips',
    PASSENGERS: (tripId: number) => `/driver/trips/${tripId}/passengers`,
  },
  COMPANY: {
    DASHBOARD: '/company/dashboard',
    VEHICLES: '/company/vehicles',
    TRIPS: '/company/trips',
  },
  TRACK: {
    BOOKING: (id: number) => `/track/${id}`,
  },
  REVIEWS: {
    CREATE: '/reviews',
  },
  PUBLIC: {
    CONTACT: '/contact',
    CITIES: '/cities',
  }
} as const
