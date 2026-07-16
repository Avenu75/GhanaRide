export default function AboutPage() {
  return (
    <div className="container mx-auto px-4 py-16 max-w-4xl">
      <div className="text-center mb-12">
        <h1 className="text-5xl font-bold mb-4">About GhanaRide</h1>
        <p className="text-xl text-muted-foreground">Connecting Ghana through safe, reliable, and premium transport.</p>
      </div>
      <div className="grid md:grid-cols-2 gap-12 items-center">
        <div>
          <h2 className="text-2xl font-bold mb-4 text-primary">Our Mission</h2>
          <p className="text-muted-foreground leading-relaxed mb-4">At GhanaRide, we simplify intercity and campus transportation across Ghana. Traveling from Accra to Kumasi should be seamless, safe, and affordable.</p>
          <p className="text-muted-foreground leading-relaxed">By connecting passengers with verified trusted drivers, we build a community based on trust, reliability, and convenience. Built for Ghana, with Ghanaian payments (MTN MoMo, Vodafone Cash).</p>
        </div>
        <div className="rounded-[24px] overflow-hidden shadow-xl"><img src="https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&q=80&w=800" alt="Ghana transport" className="w-full h-full object-cover" /></div>
      </div>
    </div>
  )
}
