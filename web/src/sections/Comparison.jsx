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
    <div className="w-5 h-5 md:w-6 md:h-6 rounded-full bg-success/20 flex items-center justify-center mx-auto">
      <Check className="w-3 h-3 md:w-3.5 md:h-3.5 text-success" />
    </div>
  ) : (
    <div className="w-5 h-5 md:w-6 md:h-6 rounded-full bg-alert/10 flex items-center justify-center mx-auto">
      <X className="w-3 h-3 md:w-3.5 md:h-3.5 text-alert/60" />
    </div>
  )
}

export default function Comparison() {
  return (
    <section className="relative py-16 md:py-24 bg-white">
      <div className="mx-auto max-w-4xl px-4 md:px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h2 className="text-2xl md:text-3xl lg:text-4xl font-bold text-text">
            Why <span className="text-cyan">AnteClick</span> Is Different
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto text-sm md:text-base">
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
          <div className="grid grid-cols-[1fr_auto_auto_auto] md:grid-cols-4 gap-2 md:gap-4 px-3 md:px-6 py-3 md:py-4 border-b border-glass-border bg-gray-100">
            <div className="text-text-muted text-[10px] md:text-xs font-semibold uppercase">Feature</div>
            <div className="text-center text-text-muted text-[10px] md:text-xs font-semibold uppercase"><span className="hidden min-[400px]:inline">Antivirus</span><span className="min-[400px]:hidden">AV</span></div>
            <div className="text-center text-text-muted text-[10px] md:text-xs font-semibold uppercase"><span className="hidden min-[400px]:inline">Safe Browsing</span><span className="min-[400px]:hidden">SB</span></div>
            <div className="text-center text-cyan text-[10px] md:text-xs font-semibold uppercase"><span className="hidden min-[400px]:inline">AnteClick</span><span className="min-[400px]:hidden">AC</span></div>
          </div>

          {/* Rows */}
          {features.map((feature, i) => (
            <motion.div
              key={feature.name}
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 + i * 0.05 }}
              className="grid grid-cols-[1fr_auto_auto_auto] md:grid-cols-4 gap-2 md:gap-4 px-3 md:px-6 py-2.5 md:py-3 border-b border-glass-border/50 last:border-0 hover:bg-gray-200/30 transition-colors"
            >
              <div className="text-text text-xs md:text-sm flex items-center">{feature.name}</div>
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
