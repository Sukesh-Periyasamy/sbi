import { motion } from 'framer-motion'
import { ShieldCheck, EyeOff, Server, Lock, Wifi, Database } from 'lucide-react'

const principles = [
  {
    icon: EyeOff,
    title: 'No Data Collection',
    description: 'Only URL text from browser address bars is read. No passwords, forms, or browsing history.',
  },
  {
    icon: Lock,
    title: 'No Credential Storage',
    description: 'AnteClick never sees or stores your banking credentials. Zero access to login forms.',
  },
  {
    icon: Wifi,
    title: 'HTTPS Only',
    description: 'All communication enforced over HTTPS. Network security config blocks cleartext traffic.',
  },
  {
    icon: Server,
    title: 'Local-First Analysis',
    description: 'Threat scoring runs entirely on-device. Backend is only consulted for ambiguous cases.',
  },
  {
    icon: Database,
    title: 'No Cloud Backups',
    description: 'App data is excluded from Android cloud backups. Sensitive threat data stays on your device.',
  },
  {
    icon: ShieldCheck,
    title: 'Open Architecture',
    description: 'Transparent detection logic. No traffic interception, no MITM, no packet inspection.',
  },
]

export default function Security() {
  return (
    <section id="security" className="relative py-24 bg-navy-light">
      <div className="mx-auto max-w-7xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-white">
            Privacy-First <span className="text-cyan">Architecture</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto">
            Built from the ground up with zero-trust principles. 
            Your data never leaves your device unless absolutely necessary.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {principles.map((item, i) => (
            <motion.div
              key={item.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: i * 0.08 }}
              className="glass-card rounded-2xl p-6 hover:border-cyan/20 transition-all"
            >
              <item.icon className="w-8 h-8 text-success mb-4" />
              <h3 className="text-white font-semibold mb-2">{item.title}</h3>
              <p className="text-text-muted text-sm leading-relaxed">{item.description}</p>
            </motion.div>
          ))}
        </div>

        {/* Trust banner */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="mt-12 text-center p-6 rounded-2xl border border-success/20 bg-success/5"
        >
          <p className="text-success font-medium">
            ✓ No MITM &nbsp;·&nbsp; ✓ No TLS Decryption &nbsp;·&nbsp; ✓ No Packet Inspection &nbsp;·&nbsp; ✓ No Credential Access
          </p>
        </motion.div>
      </div>
    </section>
  )
}
