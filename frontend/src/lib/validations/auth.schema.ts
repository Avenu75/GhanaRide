
import { z } from 'zod'
export const loginSchema = z.object({ email: z.string().min(3), password: z.string().min(6) })
export const registerSchema = z.object({ fullName: z.string().min(3), email: z.string().email(), username: z.string().min(3), phoneNumber: z.string().min(10), password: z.string().min(6), confirmPassword: z.string().min(6) }).refine(d=>d.password===d.confirmPassword, { message:'Passwords mismatch', path:['confirmPassword'] })
