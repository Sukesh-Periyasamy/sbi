import { motion } from 'framer-motion'
import { Shield } from 'lucide-react'

const banks = ['SBI', 'HDFC', 'ICICI', 'Axis', 'Kotak', 'Paytm', 'PhonePe', 'UPI']

const threats = [
  'Fake KYC update links',
  'SBI/HDFC impersonation sites',
  'UPI payment scams',
  'Fake banking APK downloads',
  'OTP phishing via WhatsApp',
  'Loan approval fraud links',
]

export default function IndiaFocus() {
  return (
    <section className="relative py-24 bg-navy">
      <div className="mx-auto max-w-7xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-white">
            Built for India's <span className="text-cyan">Banking Ecosystem</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto">
            Specifically trained on Indian banking phishing patterns — not generic global databases.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Protected banks */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
            className="glass-card rounded-2xl p-6"
          >
            <h3 className="text-white font-semibold text-lg mb-4 flex items-center gap-2">
              <Shield className="w-5 h-5 text-success" />
              Trusted Bank Whitelist
            </h3>
            <div className="flex flex-wrap gap-2">
              {banks.map((bank, i) => (
                <motion.span
                  key={bank}
                  initial={{ opacity: 0, scale: 0.8 }}
                  whileInView={{ opacity: 1, scale: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.05 }}
                  className="px-3 py-1.5 bg-success/10 border border-success/30 rounded-lg text-success text-sm font-medium"
                >
                  {bank}
                </motion.span>
              ))}
            </div>
            <p className="mt-4 text-text-muted text-sm">
              Legitimate bank domains are whitelisted to prevent false positives. 
              Your real banking apps and websites are never blocked.
            </p>
          </motion.div>

          {/* Threats detected */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
            className="glass-card rounded-2xl p-6"
          >
            <h3 className="text-white font-semibold text-lg mb-4 flex items-center gap-2">
              <Shield className="w-5 h-5 text-alert" />
              Threats We Detect
            </h3>
            <div className="space-y-2">
              {threats.map((threat, i) => (
                <motion.div
                  key={threat}
                  initial={{ opacity: 0 }}
                  whileInView={{ opacity: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.08 }}
                  className="flex items-center gap-2"
                >
                  <div className="w-1.5 h-1.5 rounded-full bg-alert shrink-0" />
                  <span className="text-text text-sm">{threat}</span>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
