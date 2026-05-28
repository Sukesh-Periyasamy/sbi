import { motion } from 'framer-motion'
import { Check, X } from 'lucide-react'

const features = [
  { name: 'Banking-focused detection', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'Local-first analysis', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'Real-time overlay warnings', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'Sub-300ms detection', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'No VPN required', antivirus: false, safeBrowsing: true, anteclick: true },
  { name: 'Lightweight (<10MB)', antivirus: false, safeBrowsing: true, anteclick: true },
  { name: 'Works in WhatsApp/Telegram', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'Zero data collection', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'Typo domain detection', antivirus: false, safeBrowsing: false, anteclick: true },
  { name: 'Indian bank whitelist', antivirus: false, safeBrowsing: false, anteclick: true },
]

function Cell({ value }) {
  return value ? (
    <div className="w-6 h-6 rounded-full bg-success/20 flex items-center justify-center mx-auto">
      <Check className="w-3.5 h-3.5 text-success" />
    </div>
  ) : (
    <div className="w-6 h-6 rounded-full bg-alert/10 flex items-center justify-center mx-auto">
      <X className="w-3.5 h-3.5 text-alert/60" />
    </div>
  )
}

export default function Comparison() {
  return (
    <section className="relative py-24 bg-navy">
      <div className="mx-auto max-w-4xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-white">
            Why <span className="text-cyan">AnteClick</span> Is Different
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto">
            Purpose-built for mobile banking phishing — not a generic antivirus or browser filter.
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="glass-card rounded-2xl overflow-hidden"
        >
          {/* Header */}
          <div className="grid grid-cols-4 gap-4 px-6 py-4 border-b border-glass-border bg-navy-lighter/50">
            <div className="text-text-muted text-xs font-semibold uppercase">Feature</div>
            <div className="text-center text-text-muted text-xs font-semibold uppercase">Antivirus</div>
            <div className="text-center text-text-muted text-xs font-semibold uppercase">Safe Browsing</div>
            <div className="text-center text-cyan text-xs font-semibold uppercase">AnteClick</div>
          </div>

          {/* Rows */}
          {features.map((feature, i) => (
            <motion.div
              key={feature.name}
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 + i * 0.05 }}
              className="grid grid-cols-4 gap-4 px-6 py-3 border-b border-glass-border/50 last:border-0 hover:bg-navy-lighter/30 transition-colors"
            >
              <div className="text-text text-sm flex items-center">{feature.name}</div>
              <Cell value={feature.antivirus} />
              <Cell value={feature.safeBrowsing} />
              <Cell value={feature.anteclick} />
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
