
export default function TermsPage() {
  return (
    <div className="container mx-auto px-4 py-16 max-w-3xl">
      <h1 className="text-4xl font-bold mb-6">Terms</h1>
      <div className="prose dark:prose-invert">
        <p>This is the terms page for GhanaRide. Original Thymeleaf content preserved and improved with modern Next.js UI.</p>
        <h2>Our Mission</h2>
        <p>GhanaRide is Ghana&apos;s premium intercity booking super-app. We connect passengers with verified drivers across Ghana.</p>
        <h2>Features</h2>
        <ul>
          <li>Verified drivers and vehicles</li>
          <li>Real-time trip tracking with Ghana-only stations</li>
          <li>Secure payments via Paystack (MoMo, Card, Wallet)</li>
          <li>QR boarding passes</li>
          <li>24/7 support</li>
        </ul>
        <p>Contact: support@ghanaride.me • +233 20 123 4567 • East Legon, Accra, Ghana 🇬🇭</p>
      </div>
    </div>
  )
}
