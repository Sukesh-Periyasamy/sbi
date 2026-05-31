import { motion } from 'framer-motion'
import { Shield, Zap, Eye, EyeOff, Lock, Cpu, Cloud, Smartphone, Building2, ArrowRight, CheckCircle, XCircle, Activity } from 'lucide-react'

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
}
const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4 } },
}

const architectureSteps = [
  'User Opens Banking App',
  'SDK Remains Idle',
  'Suspicious Link / Fake App Detected',
  'AnteClick Threat Engine Activates',
  'Local Heuristic Analysis',
  'Optional Backend Verification',
  'Risk Verdict Generated',
  'Warning Overlay Displayed',
  'SDK Returns to Idle State',
]

const detectionLayers = [
  'Phishing URL detection',
  'Banking keyword heuristics',
  'Suspicious TLD analysis',
  'Typosquatting detection',
  'Fake banking app verification',
  'Accessibility abuse detection',
  'Backend threat intelligence enrichment',
]

const neverDoes = [
  'Collect passwords',
  'Read OTPs',
  'Inspect personal messages',
  'Capture keystrokes',
  'Store browsing history',
  'Perform packet interception',
]

const bankBenefits = [
  'Phishing prevention',
  'Fake app detection',
  'Fraud intelligence',
  'Threat analytics',
  'Attack visibility',
  'Customer-side protection',
]

const integrationTargets = [
  'Mobile banking apps',
  'UPI applications',
  'Fintech applications',
  'Payment wallets',
  'Enterprise financial platforms',
]

export default function SDK() {
  return (
    <main className="pt-24 pb-20 bg-white min-h-screen">
      <div className="mx-auto max-w-5xl px-6">

        {/* Hero */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-cyan/20 bg-cyan/5 mb-6">
            <Building2 className="w-4 h-4 text-cyan" />
            <span className="text-cyan text-sm font-medium">For Banking & Fintech</span>
          </div>
          <h1 className="text-3xl md:text-5xl font-bold text-text leading-tight">
            AnteClick Shield SDK
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue to-cyan">
              for Banking Applications
            </span>
          </h1>
          <p className="mt-6 text-lg text-text-muted max-w-2xl mx-auto leading-relaxed">
            Lightweight embedded fraud protection for mobile banking. 
            Event-driven security that activates only when threats are detected — 
            zero impact on normal banking experience.
          </p>

          {/* Key specs */}
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            {[
              { label: '< 500 KB', desc: 'SDK Size' },
              { label: 'Event-Driven', desc: 'Architecture' },
              { label: 'No UI Default', desc: 'Invisible' },
              { label: 'Offline', desc: 'Works Without Internet' },
              { label: '0 ms Impact', desc: 'On App Performance' },
            ].map(spec => (
              <div key={spec.label} className="px-4 py-2 glass-card rounded-xl text-center">
                <div className="text-blue font-bold text-sm">{spec.label}</div>
                <div className="text-text-muted text-[10px]">{spec.desc}</div>
              </div>
            ))}
          </div>
        </motion.div>

        {/* How It Works */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-3">How the AnteClick SDK Works</h2>
          <p className="text-text-muted mb-6 leading-relaxed">
            Unlike traditional mobile security products, AnteClick does not continuously monitor the device 
            or run heavy background scanning. Instead, the SDK uses an <span className="text-cyan font-medium">event-driven security architecture</span>. 
            The protection layer activates only when suspicious activity is detected.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {['Suspicious banking URLs', 'Phishing navigation attempts', 'Fake banking applications', 'Typosquatting domains', 'Accessibility abuse indicators', 'Fraudulent package installations'].map((trigger, i) => (
              <motion.div
                key={trigger}
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.05 }}
                className="px-4 py-3 glass-card rounded-xl text-sm text-text flex items-center gap-2"
              >
                <div className="w-1.5 h-1.5 rounded-full bg-alert shrink-0" />
                {trigger}
              </motion.div>
            ))}
          </div>

          <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-3">
            {['Minimal battery usage', 'Near-zero performance impact', 'Lightweight memory footprint', 'Seamless banking experience'].map((benefit, i) => (
              <motion.div
                key={benefit}
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
                transition={{ delay: 0.3 + i * 0.08 }}
                className="px-3 py-2 bg-success/10 border border-success/20 rounded-lg text-xs text-success text-center"
              >
                ✓ {benefit}
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Architecture Flow */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">Event-Driven Architecture</h2>
          <div className="glass-card rounded-2xl p-6 md:p-8">
            <div className="space-y-0">
              {architectureSteps.map((step, i) => {
                const isIdle = step.includes('Idle')
                const isDetection = step.includes('Detected') || step.includes('Activates')
                const isResult = step.includes('Verdict') || step.includes('Warning')
                return (
                  <motion.div
                    key={step}
                    initial={{ opacity: 0, x: -20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ delay: i * 0.08 }}
                  >
                    <div className="flex items-center gap-3 py-2.5">
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 text-xs font-bold ${
                        isIdle ? 'bg-success/20 text-success' :
                        isDetection ? 'bg-alert/20 text-alert' :
                        isResult ? 'bg-cyan/20 text-cyan' :
                        'bg-gray-200 text-text-muted'
                      }`}>
                        {i + 1}
                      </div>
                      <span className={`text-sm font-medium ${
                        isIdle ? 'text-success' :
                        isDetection ? 'text-alert' :
                        isResult ? 'text-cyan' :
                        'text-text'
                      }`}>
                        {step}
                      </span>
                    </div>
                    {i < architectureSteps.length - 1 && (
                      <div className="ml-4 h-3 border-l border-dashed border-glass-border" />
                    )}
                  </motion.div>
                )
              })}
            </div>
          </div>
        </motion.section>

        {/* Key Characteristics */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">Key SDK Characteristics</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Lightweight Runtime */}
            <div className="glass-card rounded-2xl p-6">
              <div className="flex items-center gap-2 mb-4">
                <Zap className="w-5 h-5 text-cyan" />
                <h3 className="text-text font-semibold">Lightweight Runtime</h3>
              </div>
              <div className="space-y-2">
                {['No continuous device scanning', 'No packet inspection', 'No VPN tunneling', 'No TLS interception', 'No persistent foreground service', 'No aggressive background processing'].map(item => (
                  <div key={item} className="flex items-center gap-2">
                    <XCircle className="w-3.5 h-3.5 text-text-muted/50 shrink-0" />
                    <span className="text-sm text-text-muted">{item}</span>
                  </div>
                ))}
              </div>
              <p className="mt-4 text-xs text-cyan/70 italic">
                The SDK is optimized to remain dormant until required.
              </p>
            </div>

            {/* Invisible During Normal Use */}
            <div className="glass-card rounded-2xl p-6">
              <div className="flex items-center gap-2 mb-4">
                <EyeOff className="w-5 h-5 text-cyan" />
                <h3 className="text-text font-semibold">Invisible During Normal Use</h3>
              </div>
              <p className="text-sm text-text-muted mb-3">During safe banking usage:</p>
              <div className="space-y-2">
                {['No popups', 'No interruptions', 'No additional screens', 'No impact on banking flow'].map(item => (
                  <div key={item} className="flex items-center gap-2">
                    <CheckCircle className="w-3.5 h-3.5 text-success shrink-0" />
                    <span className="text-sm text-text">{item}</span>
                  </div>
                ))}
              </div>
              <p className="mt-4 text-xs text-text-muted italic">
                Only when high-risk activity is detected does the protection layer display a contextual warning.
              </p>
            </div>
          </div>
        </motion.section>

        {/* Real-Time Detection */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">Real-Time Threat Detection</h2>
          <div className="glass-card rounded-2xl p-6">
            <p className="text-text-muted text-sm mb-4">The SDK uses multiple lightweight security layers:</p>
            <motion.div variants={container} initial="hidden" whileInView="show" viewport={{ once: true }} className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {detectionLayers.map((layer, i) => (
                <motion.div key={layer} variants={item} className="flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-lg">
                  <Activity className="w-3.5 h-3.5 text-cyan shrink-0" />
                  <span className="text-sm text-text">{layer}</span>
                </motion.div>
              ))}
            </motion.div>
            <p className="mt-4 text-xs text-cyan/70">
              Detection occurs in near real-time with sub-second response targets.
            </p>
          </div>
        </motion.section>

        {/* Privacy */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">Privacy-First Architecture</h2>
          <div className="glass-card rounded-2xl p-6">
            <p className="text-text-muted text-sm mb-4">The SDK is designed with strict privacy principles. AnteClick:</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {neverDoes.map(item => (
                <div key={item} className="flex items-center gap-2 px-3 py-2">
                  <XCircle className="w-4 h-4 text-alert/60 shrink-0" />
                  <span className="text-sm text-text">Does not {item.toLowerCase()}</span>
                </div>
              ))}
            </div>
            <div className="mt-4 p-3 bg-success/5 border border-success/20 rounded-xl">
              <p className="text-success text-sm font-medium">
                ✓ Only suspicious security events are analyzed. Normal banking activity is never inspected.
              </p>
            </div>
          </div>
        </motion.section>

        {/* Why Banks */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">Why Banks Can Use AnteClick</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="glass-card rounded-2xl p-6">
              <h3 className="text-text font-semibold mb-4 flex items-center gap-2">
                <Shield className="w-5 h-5 text-cyan" />
                What Banks Get
              </h3>
              <div className="space-y-2">
                {bankBenefits.map(benefit => (
                  <div key={benefit} className="flex items-center gap-2">
                    <CheckCircle className="w-3.5 h-3.5 text-success shrink-0" />
                    <span className="text-sm text-text">{benefit}</span>
                  </div>
                ))}
              </div>
              <p className="mt-4 text-xs text-text-muted">
                Without requiring intrusive device monitoring.
              </p>
            </div>

            <div className="glass-card rounded-2xl p-6">
              <h3 className="text-text font-semibold mb-4 flex items-center gap-2">
                <Smartphone className="w-5 h-5 text-cyan" />
                Integration Targets
              </h3>
              <div className="space-y-2">
                {integrationTargets.map(target => (
                  <div key={target} className="flex items-center gap-2">
                    <ArrowRight className="w-3.5 h-3.5 text-cyan shrink-0" />
                    <span className="text-sm text-text">{target}</span>
                  </div>
                ))}
              </div>
              <p className="mt-4 text-xs text-text-muted">
                Lightweight and modular integration.
              </p>
            </div>
          </div>
        </motion.section>

        {/* Scalability + Final */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">Enterprise Security Without Enterprise Complexity</h2>
          <div className="glass-card rounded-2xl p-6">
            <p className="text-text-muted text-sm leading-relaxed mb-4">
              AnteClick is designed to provide practical mobile fraud protection without introducing 
              heavy antivirus behavior, invasive monitoring, excessive permissions, battery drain, 
              or complex onboarding flows.
            </p>
            <p className="text-text-muted text-sm leading-relaxed mb-4">
              The architecture separates realtime detection, analytics, dashboard intelligence, and 
              backend enrichment — enabling scalable deployment for large banking ecosystems while 
              maintaining low latency and low infrastructure cost.
            </p>
            <div className="p-4 bg-cyan/5 border border-cyan/20 rounded-xl text-center">
              <p className="text-cyan font-semibold text-lg">
                "Protect users only when protection is needed."
              </p>
            </div>
          </div>
        </motion.section>

        {/* Platform Positioning */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold text-text mb-6">The AnteClick Platform</h2>
          <div className="glass-card rounded-2xl p-6">
            <p className="text-text-muted text-sm leading-relaxed mb-6">
              AnteClick is more than a consumer app. It's a complete banking fraud prevention platform.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[
                { title: 'AnteClick Consumer App', desc: 'Real-time phishing protection for end users', status: 'Live' },
                { title: 'AnteClick Shield SDK', desc: 'Lightweight integration for banking apps', status: 'Available' },
                { title: 'Threat Intelligence Dashboard', desc: 'Real-time analytics and attack visibility', status: 'Live' },
                { title: 'Banking Threat Analytics', desc: 'Enterprise fraud intelligence platform', status: 'Roadmap' },
              ].map((item, i) => (
                <motion.div
                  key={item.title}
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="p-4 bg-navy-light rounded-xl border border-glass-border"
                >
                  <div className="flex items-center justify-between mb-1">
                    <h4 className="text-text font-semibold text-sm">{item.title}</h4>
                    <span className={`text-[9px] px-2 py-0.5 rounded-full font-medium ${
                      item.status === 'Live' ? 'bg-success/10 text-success border border-success/20' :
                      item.status === 'Available' ? 'bg-cyan/10 text-cyan border border-cyan/20' :
                      'bg-gray-100 text-text-muted border border-gray-200'
                    }`}>{item.status}</span>
                  </div>
                  <p className="text-text-muted text-xs">{item.desc}</p>
                </motion.div>
              ))}
            </div>
          </div>
        </motion.section>

        {/* CTA */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center"
        >
          <a
            href="mailto:sdk@anteclick.app"
            className="inline-flex items-center gap-2 px-8 py-4 bg-blue rounded-xl text-text font-semibold hover:bg-blue-light transition-all glow-blue"
          >
            <Building2 className="w-5 h-5" />
            Contact for SDK Integration
            <ArrowRight className="w-4 h-4" />
          </a>
          <p className="mt-4 text-text-muted text-xs">sdk@anteclick.app</p>
        </motion.div>
      </div>
    </main>
  )
}
