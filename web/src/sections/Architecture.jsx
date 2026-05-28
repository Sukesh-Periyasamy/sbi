import { motion } from 'framer-motion'
import { Smartphone, Eye, Cpu, Cloud, Database, Globe } from 'lucide-react'

const layers = [
  { icon: Smartphone, label: 'Android App', detail: 'Kotlin + Jetpack Compose', color: 'bg-blue' },
  { icon: Eye, label: 'Accessibility Detection', detail: 'Browser URL monitoring', color: 'bg-cyan' },
  { icon: Cpu, label: 'Local Heuristics', detail: '16 signals, <300ms', color: 'bg-blue-light' },
  { icon: Cloud, label: 'FastAPI Backend', detail: 'Cloud verification', color: 'bg-cyan' },
  { icon: Database, label: 'Redis Cache', detail: '10-min TTL, <5ms lookup', color: 'bg-blue' },
  { icon: Globe, label: 'Threat Intelligence', detail: 'OpenPhish, PhishTank, URLhaus', color: 'bg-cyan' },
]

const specs = [
  { label: 'Detection', value: 'Local-first, sub-300ms' },
  { label: 'Backend', value: '~50ms cached response' },
  { label: 'Cache', value: 'Redis, ~5ms lookup' },
  { label: 'Signals', value: '16 heuristic checks' },
  { label: 'Network', value: 'No VPN, no packet inspection' },
  { label: 'Privacy', value: '0 personal data collected' },
]

export default function Architecture() {
  return (
    <section id="architecture" className="relative py-24 bg-navy-light">
      <div className="mx-auto max-w-7xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-text">
            Technology <span className="text-cyan">Architecture</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto">
            A layered detection pipeline designed for speed, accuracy, and privacy.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Architecture flow */}
          <div className="space-y-3">
            {layers.map((layer, i) => (
              <motion.div
                key={layer.label}
                initial={{ opacity: 0, x: -30 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.4, delay: i * 0.1 }}
                className="flex items-center gap-4"
              >
                <div className={`w-12 h-12 rounded-xl ${layer.color}/20 flex items-center justify-center shrink-0`}>
                  <layer.icon className="w-6 h-6 text-cyan" />
                </div>
                <div className="flex-1 glass-card rounded-xl px-4 py-3 flex items-center justify-between">
                  <div>
                    <div className="text-text font-medium text-sm">{layer.label}</div>
                    <div className="text-text-muted text-xs">{layer.detail}</div>
                  </div>
                  {i < layers.length - 1 && (
                    <div className="text-cyan text-lg">↓</div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>

          {/* Performance specs */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="glass-card rounded-2xl p-6 md:p-8"
          >
            <h3 className="text-text font-semibold text-lg mb-6">Performance Metrics</h3>
            <div className="space-y-4">
              {specs.map((spec, i) => (
                <motion.div
                  key={spec.label}
                  initial={{ opacity: 0 }}
                  whileInView={{ opacity: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.4 + i * 0.08 }}
                  className="flex items-center justify-between py-2 border-b border-glass-border last:border-0"
                >
                  <span className="text-text-muted text-sm">{spec.label}</span>
                  <span className="text-cyan font-mono text-sm font-medium">{spec.value}</span>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
