'use client'
export default function CompanyTripPassengers({ params }: { params: { id: string } }) {
  return <div className="container mx-auto px-4 py-8"><h1 className="text-3xl font-bold">Company Trip {params.id} • Passengers</h1><p className="text-muted-foreground">Same passenger list logic as driver, but with company fleet context.</p></div>
}
