import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { MessageSquare, Search, ShieldAlert, CheckCircle } from 'lucide-react'

const steps = [
  {
    icon: MessageSquare,
    step: '01',
    title: 'Suspicious Link Received',
    description: 'You receive a phishing link via WhatsApp, SMS, Telegram, or email disguised as your bank.',
    color: 'from-blue to-blue-light',
  },
  {
    icon: Search,
    step: '02',
    title: 'AI Analyzes Instantly',
    description: 'AnteClick detects the URL in your browser and runs 16 heuristic signals in under 300ms.',
    color: 'from-cyan to-cyan-glow',
  },
  {
    icon: ShieldAlert,
    step: '03',
    title: 'Threat Warning Shown',
    description: 'If the URL is dangerous, an instant overlay warning appears — before you enter any credentials.',
    color: 'from-alert to-warning',
  },
  {
    icon: CheckCircle,
    step: '04',
    title: 'You Stay Protected',
    description: 'Block the site with one tap. Your banking credentials remain safe. Threat is logged for reference.',
    color: 'from-success to-cyan',
  },
]

// ─── Phone Animations ─────────────────────────────────────────────────────────

function PhoneStep1() {
  return (
    <div className="w-full h-full flex flex-col">
      {/* WhatsApp-style chat */}
      <div className="px-3 pt-3 pb-1 bg-green-600 rounded-t-lg">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-full bg-white/20" />
          <span className="text-text text-[10px] font-medium">+91 98XXX XXXXX</span>
        </div>
      </div>
      <div className="flex-1 bg-[#ECE5DD] p-3 space-y-2 overflow-hidden">
        {/* Incoming message */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.3 }}
          className="max-w-[85%] bg-white rounded-lg p-2 shadow-sm"
        >
          <p className="text-[9px] text-gray-800 leading-relaxed">
            ⚠️ Dear Customer, your SBI account has been suspended. Verify immediately:
          </p>
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.8 }}
            className="text-[9px] text-blue-600 underline mt-1"
          >
            https://sbi-secure-login.xyz/verify
          </motion.p>
        </motion.div>

        {/* User tapping the link */}
        <motion.div
          initial={{ opacity: 0, scale: 0 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 1.5, type: 'spring' }}
          className="flex justify-center mt-3"
        >
          <motion.div
            animate={{ scale: [1, 0.9, 1] }}
            transition={{ delay: 2, duration: 0.3 }}
            className="relative"
          >
            {/* Finger tap indicator */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: [0, 1, 0] }}
              transition={{ delay: 2, duration: 0.6 }}
              className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-blue/30 border-2 border-blue/50"
            />
            <div className="px-3 py-1.5 bg-blue/10 border border-blue/30 rounded-lg">
              <span className="text-[8px] text-blue-700 font-medium">👆 User clicks link</span>
            </div>
          </motion.div>
        </motion.div>

        {/* Browser opening animation */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 2.5 }}
          className="bg-white rounded-lg p-2 shadow-sm border border-gray-200"
        >
          <div className="flex items-center gap-1 px-2 py-1 bg-gray-100 rounded">
            <div className="w-2 h-2 rounded-full bg-red-400" />
            <span className="text-[7px] text-gray-600 truncate">sbi-secure-login.xyz/verify</span>
          </div>
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: '100%' }}
            transition={{ delay: 2.8, duration: 0.5 }}
            className="h-0.5 bg-blue mt-1 rounded"
          />
        </motion.div>
      </div>
    </div>
  )
}

const signals = [
  { name: 'Banking keyword', score: 20, color: 'bg-warning' },
  { name: 'Suspicious TLD (.xyz)', score: 25, color: 'bg-alert' },
  { name: 'TLD + keyword combo', score: 30, color: 'bg-alert' },
  { name: 'Typo domain', score: 25, color: 'bg-warning' },
  { name: 'Excessive hyphens', score: 15, color: 'bg-warning' },
]

function PhoneStep2() {
  return (
    <div className="w-full h-full flex flex-col bg-navy-light p-3">
      {/* URL being scanned */}
      <div className="px-2 py-1.5 bg-white rounded-lg border border-cyan/20 mb-3">
        <div className="flex items-center gap-1">
          <Search className="w-3 h-3 text-cyan" />
          <span className="text-[8px] text-cyan font-mono truncate">sbi-secure-login.xyz</span>
        </div>
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: '100%' }}
          transition={{ duration: 1.5, ease: 'easeInOut' }}
          className="h-0.5 bg-gradient-to-r from-cyan to-blue mt-1 rounded"
        />
      </div>

      {/* Signals appearing one by one */}
      <div className="flex-1 space-y-1.5 overflow-hidden">
        <div className="text-[8px] text-text-muted font-medium mb-1">HEURISTIC SIGNALS</div>
        {signals.map((signal, i) => (
          <motion.div
            key={signal.name}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.5 + i * 0.4 }}
            className="flex items-center justify-between px-2 py-1.5 bg-white/80 rounded-lg border border-glass-border"
          >
            <div className="flex items-center gap-1.5">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.7 + i * 0.4, type: 'spring' }}
                className={`w-1.5 h-1.5 rounded-full ${signal.color}`}
              />
              <span className="text-[8px] text-text">{signal.name}</span>
            </div>
            <motion.span
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.9 + i * 0.4 }}
              className="text-[8px] font-bold text-alert"
            >
              +{signal.score}
            </motion.span>
          </motion.div>
        ))}
      </div>

      {/* Final score */}
      <motion.div
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 2.8, type: 'spring' }}
        className="mt-2 p-2 rounded-xl bg-alert/10 border border-alert/30 text-center"
      >
        <div className="text-[8px] text-text-muted">THREAT SCORE</div>
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 3 }}
          className="text-xl font-bold text-alert"
        >
          115
        </motion.div>
        <div className="text-[8px] text-alert font-medium">HIGH RISK</div>
      </motion.div>
    </div>
  )
}

function PhoneStep3() {
  return (
    <div className="w-full h-full flex flex-col bg-navy-light relative overflow-hidden">
      {/* Browser in background (dimmed) */}
      <div className="absolute inset-0 opacity-30 p-3">
        <div className="px-2 py-1 bg-gray-800 rounded text-[7px] text-gray-400">
          sbi-secure-login.xyz/verify
        </div>
        <div className="mt-2 space-y-1">
          <div className="h-2 bg-gray-700 rounded w-3/4" />
          <div className="h-2 bg-gray-700 rounded w-1/2" />
          <div className="h-6 bg-gray-700 rounded w-full mt-2" />
        </div>
      </div>

      {/* Overlay warning sliding down */}
      <motion.div
        initial={{ y: -200, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.5, type: 'spring', damping: 20 }}
        className="relative z-10 m-3 mt-6"
      >
        <div className="bg-white rounded-2xl border border-alert/40 shadow-2xl overflow-hidden">
          {/* Warning header */}
          <div className="bg-alert/20 px-3 py-2 flex items-center gap-2">
            <motion.div
              animate={{ rotate: [0, -10, 10, -10, 0] }}
              transition={{ delay: 1, duration: 0.5 }}
            >
              <ShieldAlert className="w-4 h-4 text-alert" />
            </motion.div>
            <span className="text-[10px] font-bold text-alert">⚠ Phishing Detected</span>
          </div>

          {/* Warning body */}
          <div className="p-3 space-y-2">
            <p className="text-[8px] text-text leading-relaxed">
              This website may impersonate your bank and attempt to steal login credentials.
            </p>

            {/* Risk chips */}
            <div className="flex flex-wrap gap-1">
              {['Suspicious TLD', 'Banking keyword', 'Typo domain'].map((chip, i) => (
                <motion.span
                  key={chip}
                  initial={{ opacity: 0, scale: 0 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 1 + i * 0.2 }}
                  className="px-1.5 py-0.5 bg-alert/10 border border-alert/30 rounded text-[7px] text-alert"
                >
                  {chip}
                </motion.span>
              ))}
            </div>

            {/* Confidence bar */}
            <div className="mt-1">
              <div className="flex justify-between text-[7px] text-text-muted mb-0.5">
                <span>Confidence</span>
                <span className="text-alert">96%</span>
              </div>
              <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: '96%' }}
                  transition={{ delay: 1.5, duration: 0.8 }}
                  className="h-full bg-gradient-to-r from-warning to-alert rounded-full"
                />
              </div>
            </div>

            {/* Action buttons */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 2 }}
              className="flex gap-2 mt-2"
            >
              <div className="flex-1 py-1.5 bg-alert rounded-lg text-center">
                <span className="text-[8px] text-text font-bold">Leave Website</span>
              </div>
              <div className="flex-1 py-1.5 border border-text-muted/30 rounded-lg text-center">
                <span className="text-[8px] text-text-muted">Continue</span>
              </div>
            </motion.div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}

function PhoneStep4() {
  return (
    <div className="w-full h-full flex flex-col bg-navy-light p-3">
      {/* Success state */}
      <motion.div
        initial={{ opacity: 0, scale: 0.5 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.3, type: 'spring' }}
        className="flex flex-col items-center justify-center py-4"
      >
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.5, type: 'spring' }}
          className="w-12 h-12 rounded-full bg-success/20 flex items-center justify-center mb-2"
        >
          <CheckCircle className="w-6 h-6 text-success" />
        </motion.div>
        <span className="text-[10px] font-bold text-success">Threat Blocked</span>
        <span className="text-[7px] text-text-muted mt-0.5">Your credentials are safe</span>
      </motion.div>

      {/* Threat logged */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1 }}
        className="bg-white rounded-xl border border-glass-border p-2.5 mb-2"
      >
        <div className="text-[8px] text-text-muted font-medium mb-1.5">THREAT LOGGED</div>
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-lg bg-alert/10 flex items-center justify-center">
            <ShieldAlert className="w-3 h-3 text-alert" />
          </div>
          <div className="flex-1">
            <div className="text-[8px] text-text font-medium">sbi-secure-login.xyz</div>
            <div className="text-[7px] text-text-muted">Phishing · Score 115 · Just now</div>
          </div>
        </div>
      </motion.div>

      {/* Dashboard preview */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1.5 }}
        className="bg-white rounded-xl border border-glass-border p-2.5 flex-1"
      >
        <div className="text-[8px] text-text-muted font-medium mb-2">PROTECTION STATUS</div>
        <div className="flex items-center gap-2 mb-2">
          <motion.div
            animate={{ opacity: [0.5, 1, 0.5] }}
            transition={{ repeat: Infinity, duration: 2 }}
            className="w-2 h-2 rounded-full bg-success"
          />
          <span className="text-[9px] text-success font-medium">Active Protection</span>
        </div>
        <div className="grid grid-cols-2 gap-1.5">
          <div className="bg-gray-100 rounded-lg p-1.5 text-center">
            <div className="text-[10px] font-bold text-text">3</div>
            <div className="text-[6px] text-text-muted">Threats Blocked</div>
          </div>
          <div className="bg-gray-100 rounded-lg p-1.5 text-center">
            <div className="text-[10px] font-bold text-cyan">247</div>
            <div className="text-[6px] text-text-muted">URLs Scanned</div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}

const phoneAnimations = [PhoneStep1, PhoneStep2, PhoneStep3, PhoneStep4]

// ─── Main Component ───────────────────────────────────────────────────────────

export default function HowItWorks() {
  const [activeStep, setActiveStep] = useState(null)

  const handleStepInteraction = (i) => {
    // Toggle on tap for mobile, acts as hover replacement
    setActiveStep(activeStep === i ? null : i)
  }

  return (
    <section id="how-it-works" className="relative py-16 md:py-24 bg-navy-light">
      <div className="mx-auto max-w-7xl px-4 md:px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12 md:mb-16"
        >
          <h2 className="text-2xl md:text-3xl lg:text-4xl font-bold text-text">
            How <span className="text-cyan">AnteClick</span> Works
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto text-sm md:text-base">
            Protection happens automatically. Tap or hover over each step to see it in action.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 min-[400px]:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-8">
          {steps.map((step, i) => {
            const PhoneAnimation = phoneAnimations[i]
            const isActive = activeStep === i
            return (
              <motion.div
                key={step.step}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: i * 0.15 }}
                className="relative"
                onMouseEnter={() => setActiveStep(i)}
                onMouseLeave={() => setActiveStep(null)}
                onClick={() => handleStepInteraction(i)}
              >
                {/* Connector line */}
                {i < steps.length - 1 && (
                  <div className="hidden lg:block absolute top-12 left-full w-full h-px bg-gradient-to-r from-cyan/30 to-transparent z-0" />
                )}

                <div className={`glass-card rounded-2xl p-4 md:p-6 relative z-10 cursor-pointer transition-all ${isActive ? 'border-cyan/30' : 'hover:border-cyan/30'}`}>
                  {/* Step number */}
                  <div className={`inline-flex items-center justify-center w-10 h-10 md:w-12 md:h-12 rounded-xl bg-gradient-to-br ${step.color} mb-3 md:mb-4`}>
                    <step.icon className="w-5 h-5 md:w-6 md:h-6 text-text" />
                  </div>
                  
                  <div className="text-xs text-cyan font-mono mb-1 md:mb-2">STEP {step.step}</div>
                  <h3 className="text-text font-semibold text-base md:text-lg mb-1 md:mb-2">{step.title}</h3>
                  <p className="text-text-muted text-xs md:text-sm leading-relaxed">{step.description}</p>
                </div>

                {/* Phone popup on hover/tap - desktop only */}
                <AnimatePresence>
                  {isActive && (
                    <motion.div
                      initial={{ opacity: 0, y: 10, scale: 0.95 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 10, scale: 0.95 }}
                      transition={{ duration: 0.2 }}
                      className="absolute z-50 left-1/2 -translate-x-1/2 bottom-full mb-4 hidden md:block"
                    >
                      {/* Phone frame */}
                      <div className="w-48 h-80 rounded-[1.5rem] border-2 border-navy-lighter bg-white shadow-2xl overflow-hidden glow-cyan">
                        {/* Notch */}
                        <div className="flex justify-center pt-1.5 pb-1 bg-white">
                          <div className="w-12 h-1.5 rounded-full bg-gray-200" />
                        </div>
                        {/* Screen */}
                        <div className="h-[calc(100%-24px)] overflow-hidden">
                          <PhoneAnimation />
                        </div>
                      </div>
                      {/* Arrow */}
                      <div className="flex justify-center">
                        <div className="w-3 h-3 bg-white border-b-2 border-r-2 border-navy-lighter rotate-45 -mt-1.5" />
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* Mobile inline phone animation - shows below card on tap */}
                <AnimatePresence>
                  {isActive && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      exit={{ opacity: 0, height: 0 }}
                      transition={{ duration: 0.3 }}
                      className="md:hidden overflow-hidden mt-3"
                    >
                      <div className="w-40 h-72 mx-auto rounded-[1.5rem] border-2 border-navy-lighter bg-white shadow-xl overflow-hidden">
                        <div className="flex justify-center pt-1.5 pb-1 bg-white">
                          <div className="w-10 h-1 rounded-full bg-gray-200" />
                        </div>
                        <div className="h-[calc(100%-20px)] overflow-hidden">
                          <PhoneAnimation />
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
