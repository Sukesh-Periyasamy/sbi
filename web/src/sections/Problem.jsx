import { motion } from 'framer-motion'
import { AlertTriangle, TrendingUp, Smartphone, Link2 } from 'lucide-react'

const stats = [
  { icon: TrendingUp, value: '₹1,750 Cr+', label: 'Lost to phishing in India (2023)', color: 'text-alert' },
  { icon: Smartphone, value: '5 Lakh+', label: 'Fake banking apps detected yearly', color: 'text-warning' },
  { icon: Link2, value: '10,000+', label: 'New phishing URLs created daily', color: 'text-cyan' },
  { icon: AlertTriangle, value: '82%', label: 'Attacks target mobile users', color: 'text-alert' },
]

export default function Problem() {
  return (
    <section className="relative py-16 md:py-24 bg-navy-light">
      <div className="mx-auto max-w-7xl px-4 md:px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <h2 className="text-2xl md:text-3xl lg:text-4xl font-bold text-text">
            The Threat Is <span className="text-alert">Real</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto text-sm md:text-base">
            Every day, millions of Indians receive phishing links via WhatsApp, SMS, 
            and email — designed to steal banking credentials in seconds.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 min-[400px]:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
          {stats.map((stat, i) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: i * 0.1 }}
              className="glass-card rounded-2xl p-4 md:p-6 text-center hover:border-cyan/20 transition-colors"
            >
              <stat.icon className={`w-7 h-7 md:w-8 md:h-8 ${stat.color} mx-auto mb-3 md:mb-4`} />
              <div className={`text-xl md:text-2xl lg:text-3xl font-bold ${stat.color}`}>{stat.value}</div>
              <p className="mt-2 text-text-muted text-xs md:text-sm">{stat.label}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
