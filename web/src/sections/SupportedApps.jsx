import { motion } from 'framer-motion'

const apps = [
  { name: 'Chrome', icon: '🌐' },
  { name: 'Firefox', icon: '🦊' },
  { name: 'Brave', icon: '🦁' },
  { name: 'Samsung Internet', icon: '🌍' },
  { name: 'WhatsApp', icon: '💬' },
  { name: 'Telegram', icon: '✈️' },
  { name: 'Instagram', icon: '📷' },
  { name: 'Edge', icon: '🔷' },
]

export default function SupportedApps() {
  return (
    <section className="relative py-20 bg-navy-light">
      <div className="mx-auto max-w-5xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-text">
            Works Across <span className="text-cyan">All Your Apps</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-xl mx-auto">
            Protects you in browsers and in-app links — wherever phishing links appear.
          </p>
        </motion.div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {apps.map((app, i) => (
            <motion.div
              key={app.name}
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: i * 0.05 }}
              className="glass-card rounded-xl p-4 text-center hover:border-cyan/20 transition-all"
            >
              <div className="text-3xl mb-2">{app.icon}</div>
              <div className="text-text text-sm font-medium">{app.name}</div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
