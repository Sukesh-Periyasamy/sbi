import { motion } from 'framer-motion'
import { CheckCircle, Circle } from 'lucide-react'

const phases = [
  { phase: 'Phase 1', title: 'Android Phishing Protection', status: 'done', items: ['16 heuristic signals', 'Real-time overlay warnings', 'Backend verification', 'Play Store ready'] },
  { phase: 'Phase 2', title: 'Threat Intelligence Dashboard', status: 'next', items: ['Phishing heatmap', 'Most attacked banks', 'Daily threat spikes', 'Regional analytics'] },
  { phase: 'Phase 3', title: 'Browser Extension', status: 'planned', items: ['Chrome extension', 'Firefox add-on', 'Desktop protection', 'Cross-platform sync'] },
  { phase: 'Phase 4', title: 'Enterprise Analytics', status: 'planned', items: ['Organization dashboard', 'Employee protection', 'Threat reporting API', 'Compliance tools'] },
  { phase: 'Phase 5', title: 'ML Campaign Detection', status: 'planned', items: ['Pattern recognition', 'Campaign clustering', 'Predictive alerts', 'Zero-day detection'] },
]

export default function Roadmap() {
  return (
    <section className="relative py-24 bg-navy-light">
      <div className="mx-auto max-w-4xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-white">
            Product <span className="text-cyan">Roadmap</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-xl mx-auto">
            From mobile protection to a full cyber intelligence platform.
          </p>
        </motion.div>

        <div className="space-y-4">
          {phases.map((phase, i) => (
            <motion.div
              key={phase.phase}
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: i * 0.1 }}
              className={`glass-card rounded-xl p-5 flex gap-4 ${
                phase.status === 'done' ? 'border-success/30' :
                phase.status === 'next' ? 'border-cyan/30' : ''
              }`}
            >
              <div className="shrink-0 pt-0.5">
                {phase.status === 'done' ? (
                  <CheckCircle className="w-5 h-5 text-success" />
                ) : phase.status === 'next' ? (
                  <motion.div animate={{ opacity: [0.5, 1, 0.5] }} transition={{ repeat: Infinity, duration: 2 }}>
                    <Circle className="w-5 h-5 text-cyan" />
                  </motion.div>
                ) : (
                  <Circle className="w-5 h-5 text-text-muted/40" />
                )}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-1">
                  <span className="text-xs text-cyan font-mono">{phase.phase}</span>
                  {phase.status === 'done' && <span className="text-[10px] px-2 py-0.5 bg-success/20 text-success rounded-full font-medium">Complete</span>}
                  {phase.status === 'next' && <span className="text-[10px] px-2 py-0.5 bg-cyan/20 text-cyan rounded-full font-medium">In Progress</span>}
                </div>
                <h3 className="text-white font-semibold">{phase.title}</h3>
                <div className="mt-2 flex flex-wrap gap-2">
                  {phase.items.map(item => (
                    <span key={item} className="text-[11px] text-text-muted px-2 py-0.5 bg-navy-lighter/50 rounded">
                      {item}
                    </span>
                  ))}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
