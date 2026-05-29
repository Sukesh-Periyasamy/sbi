import { motion } from 'framer-motion'
import { Shield, Eye, Zap, Globe, Bell, Lock, Cpu, Fingerprint } from 'lucide-react'

const features = [
  {
    icon: Eye,
    title: 'Real-Time URL Monitoring',
    description: 'Monitors browser address bars in real-time using Android Accessibility Service. Detects phishing the moment you navigate.',
  },
  {
    icon: Cpu,
    title: 'AI Threat Scoring',
    description: '16 heuristic signals analyze every URL — banking keywords, suspicious TLDs, typo domains, homograph attacks, and more.',
  },
  {
    icon: Zap,
    title: 'Instant Warnings',
    description: 'Overlay alerts appear in under 300ms. No app switching needed — warnings show directly over your browser.',
  },
  {
    icon: Globe,
    title: 'Backend Verification',
    description: 'Suspicious URLs are verified against our cloud API with Redis-cached reputation data for maximum accuracy.',
  },
  {
    icon: Shield,
    title: 'Banking Domain Whitelist',
    description: 'Trusted bank domains (SBI, HDFC, ICICI, Axis, Kotak, Paytm) are whitelisted to prevent false positives.',
  },
  {
    icon: Bell,
    title: 'Smart Notifications',
    description: 'Context-aware alerts that only trigger for genuine threats. No notification fatigue — only real dangers.',
  },
  {
    icon: Lock,
    title: 'Zero Data Collection',
    description: 'Only URL text is analyzed. No passwords, no browsing history, no personal data ever leaves your device.',
  },
  {
    icon: Fingerprint,
    title: 'Typo Domain Detection',
    description: 'Levenshtein distance analysis catches domains like "sbii.xyz" or "hdfc-secure.top" that mimic real banks.',
  },
]

export default function Features() {
  return (
    <section id="features" className="relative py-16 md:py-24 bg-white">
      <div className="mx-auto max-w-7xl px-4 md:px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <h2 className="text-2xl md:text-3xl lg:text-4xl font-bold text-text">
            Intelligent Protection, <span className="text-cyan">Zero Friction</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto text-sm md:text-base">
            AnteClick runs silently in the background, analyzing every URL you visit 
            and alerting you only when a real threat is detected.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: i * 0.05 }}
              className="glass-card rounded-2xl p-5 md:p-6 hover:border-cyan/20 transition-all group text-center md:text-left"
            >
              <div className="w-12 h-12 rounded-xl bg-blue/10 flex items-center justify-center mb-4 group-hover:bg-blue/20 transition-colors mx-auto md:mx-0">
                <feature.icon className="w-6 h-6 text-cyan" />
              </div>
              <h3 className="text-text font-semibold mb-2">{feature.title}</h3>
              <p className="text-text-muted text-sm leading-relaxed">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
