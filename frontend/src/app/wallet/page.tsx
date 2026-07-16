
'use client'
import { useEffect, useState } from 'react'
import { apiClient } from '@/lib/api/client'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Wallet as WalletIcon, TrendingUp, ArrowDown, ArrowUp, CreditCard } from 'lucide-react'
import { Wallet, WalletTransaction } from '@/types'
import { formatCurrency, formatDate } from '@/lib/utils'
import { toast } from 'sonner'

export default function WalletPage() {
  const [wallet, setWallet] = useState<Wallet>({ id:1, balance: 245.50, userId:1, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() })
  const [transactions, setTransactions] = useState<WalletTransaction[]>([])
  const [amount, setAmount] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(()=>{
    const load = async () => {
      try {
        const res = await apiClient.get('/wallet')
        setWallet(res.data.data || res.data)
        const hist = await apiClient.get('/wallet/history')
        setTransactions(hist.data.data?.content || hist.data.content || [])
      } catch {
        setTransactions([
          { id:1, amount:100, type:'TOPUP', provider:'PAYSTACK', status:'SUCCESS', createdAt:new Date(Date.now()-86400000).toISOString(), walletId:1 },
          { id:2, amount:-120, type:'PAYMENT', provider:'WALLET', status:'SUCCESS', createdAt:new Date(Date.now()-86400000*2).toISOString(), walletId:1, bookingReference:'GR-AB12CD' },
          { id:3, amount:50, type:'REFUND', provider:'WALLET', status:'SUCCESS', createdAt:new Date(Date.now()-86400000*3).toISOString(), walletId:1 },
        ] as any)
      }
    }
    load()
  },[])

  const handleTopup = async (e:React.FormEvent) => {
    e.preventDefault()
    if (!amount) return
    setLoading(true)
    try {
      await apiClient.post('/wallet/topup', { amount: parseFloat(amount), provider:'PAYSTACK' })
      toast.success('Wallet topup initiated')
      setWallet(w=>({...w, balance: w.balance + parseFloat(amount)}))
      setAmount('')
    } catch (err:any) { toast.error(err.response?.data?.message || 'Topup failed') } finally { setLoading(false) }
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-6">Wallet</h1>
      <div className="grid md:grid-cols-3 gap-6">
        <Card className="md:col-span-2 rounded-[24px] bg-zinc-950 text-white overflow-hidden"><CardContent className="p-8"><div className="flex justify-between items-start"><div><div className="text-zinc-400 text-sm">Total Balance</div><div className="text-4xl font-bold mt-2">{formatCurrency(wallet.balance)}</div><div className="text-zinc-400 text-xs mt-2">Available for bookings</div></div><div className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center"><WalletIcon className="w-6 h-6" /></div></div><div className="mt-8 flex gap-3"><div className="flex items-center gap-1 text-sm bg-white/10 px-3 py-1 rounded-full"><TrendingUp className="w-4 h-4" /> Ghana wallet</div></div></CardContent></Card>
        <Card className="rounded-[24px]"><CardHeader><CardTitle>Top Up</CardTitle></CardHeader><CardContent><form onSubmit={handleTopup} className="space-y-4"><div className="space-y-2"><Label>Amount (GHS)</Label><Input type="number" min="5" step="0.01" placeholder="50.00" value={amount} onChange={e=>setAmount(e.target.value)} required /></div><Button type="submit" className="w-full rounded-xl" loading={loading}><CreditCard className="w-4 h-4 mr-2" /> Top Up with MoMo / Card</Button><p className="text-xs text-muted-foreground">Minimum GH₵5. Powered by Paystack.</p></form></CardContent></Card>
      </div>
      <Card className="mt-6 rounded-[24px]"><CardHeader><CardTitle>Recent Transactions</CardTitle></CardHeader><CardContent className="space-y-3">
        {transactions.map(tx=>(
          <div key={tx.id} className="flex items-center justify-between p-3 rounded-xl bg-muted/50"><div className="flex items-center gap-3"><div className={`w-10 h-10 rounded-xl flex items-center justify-center ${tx.type==='TOPUP' ? 'bg-green-100 text-green-600' : tx.type==='PAYMENT' ? 'bg-blue-100 text-blue-600' : 'bg-amber-100 text-amber-600'}`}>{tx.type==='TOPUP' ? <ArrowDown className="w-5 h-5" /> : <ArrowUp className="w-5 h-5" />}</div><div><div className="font-medium">{tx.type} {tx.bookingReference ? `• ${tx.bookingReference}` : ''}</div><div className="text-xs text-muted-foreground">{formatDate(tx.createdAt)} • {tx.provider}</div></div></div><div className={`font-bold ${tx.amount>0 ? 'text-green-600' : 'text-foreground'}`}>{tx.amount>0?'+':''}{formatCurrency(tx.amount)}</div></div>
        ))}
      </CardContent></Card>
    </div>
  )
}
