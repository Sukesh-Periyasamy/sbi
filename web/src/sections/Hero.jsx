import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Shield, ArrowRight, CheckCircle, AlertTriangle, X, Lock } from 'lucide-react'

// ─── Full Demo Component ──────────────────────────────────────────────────────

function FullPhoneDemo({ scenario, demoKey }) {
  const [phase, setPhase] = useState(0)
  // 0: WhatsApp/SMS message visible
  // 1: User taps link (finger animation)
  // 2: Browser opens with URL loading
  // 3: AnteClick scanning bar appears
  // 4: For SAFE → green check, browsing continues. For FRAUD → signals + warning

  useEffect(() => {
    setPhase(0)
    const timers = []
    timers.push(setTimeout(() => setPhase(1), 800))   // show message
    timers.push(setTimeout(() => setPhase(2), 2200))  // tap link
    timers.push(setTimeout(() => setPhase(3), 3200))  // browser opens
    timers.push(setTimeout(() => setPhase(4), 4200))  // scanning
    timers.push(setTimeout(() => setPhase(5), 5500))  // result
    if (scenario.verdict === 'HIGH_RISK') {
      timers.push(setTimeout(() => setPhase(6), 7000)) // warning popup
    }
    return () => timers.forEach(clearTimeout)
  }, [demoKey, scenario])

  const isSafe = scenario.verdict === 'SAFE'

  return (
    <div className="w-full h-full flex flex-col bg-[#0D1B2A] overflow-hidden">
      {/* Status bar */}
      <div className="flex items-center justify-between px-4 pt-2 pb-1 shrink-0">
        <span className="text-[10px] text-text-muted font-medium">9:41</span>
        <div className="flex items-center gap-1.5">
          <div className="flex gap-0.5">
            <div className="w-1 h-2 bg-text-muted/60 rounded-sm" />
            <div className="w-1 h-2.5 bg-text-muted/60 rounded-sm" />
            <div className="w-1 h-3 bg-text-muted/60 rounded-sm" />
            <div className="w-1 h-3.5 bg-white/60 rounded-sm" />
          </div>
          <div className="w-4 h-2 rounded-sm border border-text-muted/50">
            <div className="w-3 h-full bg-success/70 rounded-sm" />
          </div>
        </div>
      </div>

      {/* Main screen content */}
      <div className="flex-1 relative overflow-hidden">

        {/* ─── Phase 0-2: WhatsApp/SMS Chat ─── */}
        <AnimatePresence>
          {phase <= 3 && (
            <motion.div
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.3 }}
              className="absolute inset-0 flex flex-col"
            >
              {/* App header */}
              <div className={`px-3 py-2 flex items-center gap-2 shrink-0 ${
                scenario.source === 'WhatsApp' ? 'bg-[#075E54]' : 'bg-navy-lighter'
              }`}>
                <div className="w-7 h-7 rounded-full bg-white/20 flex items-center justify-center">
                  <span className="text-[10px]">{scenario.source === 'WhatsApp' ? '💬' : '✉️'}</span>
                </div>
                <div>
                  <span className="text-white text-[10px] font-medium">{scenario.sender}</span>
                  <div className="text-[8px] text-white/60">online</div>
                </div>
              </div>

              {/* Chat area */}
              <div className={`flex-1 p-3 space-y-2 ${
                scenario.source === 'WhatsApp' ? 'bg-[#0B141A]' : 'bg-navy'
              }`}>
                {/* Incoming message */}
                <AnimatePresence>
                  {phase >= 0 && (
                    <motion.div
                      initial={{ opacity: 0, y: 10, scale: 0.95 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      transition={{ delay: 0.3 }}
                      className="max-w-[88%]"
                    >
                      <div className="bg-[#1F2C34] rounded-xl rounded-tl-sm p-2.5 shadow-sm">
                        <p className="text-[9px] text-text leading-relaxed">
                          {scenario.message}
                        </p>
                        <motion.div
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          transition={{ delay: 0.8 }}
                        >
                          <p className="text-[9px] text-blue-400 underline mt-1.5 break-all">
                            {scenario.url}
                          </p>
                        </motion.div>
                        <div className="text-right mt-1">
                          <span className="text-[7px] text-text-muted">10:32 AM</span>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* Tap indicator */}
                <AnimatePresence>
                  {phase === 2 && (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      className="flex justify-center pt-2"
                    >
                      <motion.div
                        animate={{ scale: [1, 0.85, 1] }}
                        transition={{ duration: 0.3, repeat: 1 }}
                        className="relative"
                      >
                        <motion.div
                          animate={{ scale: [0.8, 1.5], opacity: [0.8, 0] }}
                          transition={{ duration: 0.6 }}
                          className="absolute inset-0 rounded-full bg-blue/30"
                        />
                        <div className="px-3 py-1.5 bg-blue/10 border border-blue/30 rounded-full">
                          <span className="text-[8px] text-blue-400 font-medium">👆 Tapping link...</span>
                        </div>
                      </motion.div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ─── Phase 3+: Browser Opens ─── */}
        <AnimatePresence>
          {phase >= 3 && (
            <motion.div
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="absolute inset-0 flex flex-col bg-[#0D1B2A]"
            >
              {/* Browser tab bar */}
              <div className="px-3 pt-2 pb-1 bg-navy-lighter shrink-0">
                <div className="flex items-center gap-2 px-2.5 py-1.5 bg-navy rounded-lg border border-glass-border">
                  {phase >= 5 && isSafe ? (
                    <Lock className="w-3 h-3 text-success shrink-0" />
                  ) : phase >= 5 && !isSafe ? (
                    <AlertTriangle className="w-3 h-3 text-alert shrink-0" />
                  ) : (
                    <div className="w-3 h-3 rounded-full border border-text-muted/40 shrink-0" />
                  )}
                  <span className={`text-[8px] font-mono truncate ${
                    phase >= 5 ? (isSafe ? 'text-success' : 'text-alert') : 'text-text-muted'
                  }`}>
                    {scenario.displayUrl}
                  </span>
                </div>
                {/* Loading bar */}
                {phase === 3 && (
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: '100%' }}
                    transition={{ duration: 0.8 }}
                    className="h-0.5 bg-cyan mt-1 rounded-full"
                  />
                )}
              </div>

              {/* AnteClick scanning notification */}
              <AnimatePresence>
                {phase === 4 && (
                  <motion.div
                    initial={{ y: -30, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: -30, opacity: 0 }}
                    className="mx-3 mt-2 px-3 py-2 bg-cyan/10 border border-cyan/20 rounded-xl flex items-center gap-2"
                  >
                    <motion.div
                      animate={{ rotate: 360 }}
                      transition={{ repeat: Infinity, duration: 0.8, ease: 'linear' }}
                      className="w-4 h-4 rounded-full border-2 border-cyan/30 border-t-cyan shrink-0"
                    />
                    <div>
                      <span className="text-[9px] text-cyan font-semibold">AnteClick Scanning</span>
                      <div className="text-[7px] text-text-muted">Analyzing 16 heuristic signals...</div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Page content (fake website) */}
              <div className="flex-1 mx-3 mt-2 mb-3 relative">
                {/* Fake page skeleton */}
                {phase >= 3 && phase < 6 && (
                  <div className="space-y-2 opacity-40">
                    <div className="h-8 bg-navy-lighter rounded-lg w-3/4" />
                    <div className="h-3 bg-navy-lighter rounded w-full" />
                    <div className="h-3 bg-navy-lighter rounded w-5/6" />
                    <div className="h-20 bg-navy-lighter rounded-lg w-full mt-3" />
                    <div className="h-8 bg-navy-lighter rounded-lg w-1/2 mt-2" />
                  </div>
                )}

                {/* ─── SAFE Result ─── */}
                <AnimatePresence>
                  {phase >= 5 && isSafe && (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="absolute top-0 left-0 right-0"
                    >
                      {/* Small green toast */}
                      <motion.div
                        initial={{ y: -20, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        transition={{ type: 'spring' }}
                        className="px-3 py-2 bg-success/10 border border-success/30 rounded-xl flex items-center gap-2"
                      >
                        <CheckCircle className="w-4 h-4 text-success shrink-0" />
                        <div>
                          <span className="text-[9px] text-success font-semibold">Verified Safe</span>
                          <div className="text-[7px] text-text-muted">Trusted bank domain — no threats</div>
                        </div>
                      </motion.div>

                      {/* Page loads normally */}
                      <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.8 }}
                        className="mt-3 space-y-2"
                      >
                        <div className="h-6 bg-blue/20 rounded-lg w-2/3 flex items-center px-2">
                          <span className="text-[8px] text-blue-400">Welcome to Online SBI</span>
                        </div>
                        <div className="h-3 bg-navy-lighter rounded w-full" />
                        <div className="h-3 bg-navy-lighter rounded w-4/5" />
                        <div className="p-3 bg-navy-lighter rounded-xl mt-2">
                          <div className="text-[8px] text-text-muted mb-1">Account Login</div>
                          <div className="h-6 bg-navy rounded border border-glass-border mb-1.5" />
                          <div className="h-6 bg-navy rounded border border-glass-border mb-2" />
                          <div className="h-7 bg-blue/30 rounded-lg flex items-center justify-center">
                            <span className="text-[8px] text-blue-400 font-medium">Login</span>
                          </div>
                        </div>
                      </motion.div>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* ─── FRAUD: Warning Overlay ─── */}
                <AnimatePresence>
                  {phase >= 6 && !isSafe && (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="absolute inset-0 flex flex-col"
                    >
                      {/* Dim background */}
                      <div className="absolute inset-0 bg-black/60 rounded-xl" />

                      {/* Warning card */}
                      <motion.div
                        initial={{ y: -40, opacity: 0, scale: 0.95 }}
                        animate={{ y: 0, opacity: 1, scale: 1 }}
                        transition={{ type: 'spring', damping: 18 }}
                        className="relative z-10 bg-navy rounded-2xl border border-alert/40 shadow-2xl overflow-hidden m-1"
                      >
                        {/* Header */}
                        <div className="bg-alert/20 px-3 py-2 flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <motion.div
                              animate={{ rotate: [0, -10, 10, -10, 0] }}
                              transition={{ delay: 0.3, duration: 0.5 }}
                            >
                              <AlertTriangle className="w-4 h-4 text-alert" />
                            </motion.div>
                            <span className="text-[10px] font-bold text-alert">⚠ Phishing Detected</span>
                          </div>
                          <X className="w-3 h-3 text-text-muted/50" />
                        </div>

                        <div className="p-3 space-y-2">
                          <p className="text-[8px] text-text leading-relaxed">
                            This website may impersonate your bank and attempt to steal your login credentials.
                          </p>

                          {/* URL */}
                          <div className="px-2 py-1.5 bg-alert/5 border border-alert/20 rounded-lg">
                            <span className="text-[7px] text-text-muted">Blocked URL</span>
                            <p className="text-[8px] text-alert font-mono mt-0.5 truncate">{scenario.displayUrl}</p>
                          </div>

                          {/* Signals */}
                          <div className="space-y-1">
                            {scenario.signals.map((s, i) => (
                              <motion.div
                                key={s.name}
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ delay: 0.5 + i * 0.12 }}
                                className="flex items-center justify-between px-2 py-1 bg-navy-lighter/50 rounded"
                              >
                                <div className="flex items-center gap-1.5">
                                  <div className="w-1.5 h-1.5 rounded-full bg-alert" />
                                  <span className="text-[7px] text-text">{s.name}</span>
                                </div>
                                <span className="text-[7px] font-bold text-alert">+{s.score}</span>
                              </motion.div>
                            ))}
                          </div>

                          {/* Score + Confidence */}
                          <div className="flex items-center gap-2">
                            <div className="flex-1">
                              <div className="flex justify-between text-[7px] mb-0.5">
                                <span className="text-text-muted">Confidence</span>
                                <span className="text-alert font-bold">96%</span>
                              </div>
                              <div className="h-1.5 bg-navy-lighter rounded-full overflow-hidden">
                                <motion.div
                                  initial={{ width: 0 }}
                                  animate={{ width: '96%' }}
                                  transition={{ delay: 1, duration: 0.6 }}
                                  className="h-full bg-gradient-to-r from-warning to-alert rounded-full"
                                />
                              </div>
                            </div>
                            <div className="px-2 py-1 bg-alert/10 border border-alert/30 rounded-lg text-center">
                              <div className="text-[7px] text-text-muted">Score</div>
                              <div className="text-sm font-bold text-alert">{scenario.finalScore}</div>
                            </div>
                          </div>

                          {/* Buttons */}
                          <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            transition={{ delay: 1.5 }}
                            className="flex gap-2 pt-1"
                          >
                            <div className="flex-1 py-2 bg-alert rounded-lg text-center">
                              <span className="text-[9px] text-white font-bold">Leave Website</span>
                            </div>
                            <div className="flex-1 py-2 border border-text-muted/30 rounded-lg text-center">
                              <span className="text-[9px] text-text-muted">Continue</span>
                            </div>
                          </motion.div>
                        </div>
                      </motion.div>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* FRAUD: Scanning signals before warning */}
                <AnimatePresence>
                  {phase === 5 && !isSafe && (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      className="absolute top-0 left-0 right-0"
                    >
                      <div className="px-3 py-2 bg-alert/10 border border-alert/30 rounded-xl">
                        <div className="flex items-center gap-2 mb-2">
                          <AlertTriangle className="w-3.5 h-3.5 text-alert" />
                          <span className="text-[9px] text-alert font-semibold">Threat Detected!</span>
                        </div>
                        <div className="space-y-1">
                          {scenario.signals.slice(0, 3).map((s, i) => (
                            <motion.div
                              key={s.name}
                              initial={{ opacity: 0 }}
                              animate={{ opacity: 1 }}
                              transition={{ delay: i * 0.25 }}
                              className="flex items-center gap-1.5"
                            >
                              <div className="w-1 h-1 rounded-full bg-alert" />
                              <span className="text-[7px] text-text-muted">{s.name}</span>
                            </motion.div>
                          ))}
                        </div>
                        <motion.div
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          transition={{ delay: 0.8 }}
                          className="mt-2 text-[8px] text-alert font-bold"
                        >
                          Score: {scenario.finalScore}/150 — HIGH RISK
                        </motion.div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

// ─── Scenarios ────────────────────────────────────────────────────────────────

const SAFE_SCENARIO = {
  label: 'Safe Link',
  source: 'WhatsApp',
  sender: 'Mom',
  message: 'Check this for your FD renewal 👇',
  url: 'https://www.onlinesbi.sbi/personal/fd',
  displayUrl: 'onlinesbi.sbi/personal/fd',
  signals: [],
  finalScore: 0,
  verdict: 'SAFE',
}

const FRAUD_SCENARIO = {
  label: 'Phishing Link',
  source: 'WhatsApp',
  sender: '+91 87XXX XXXXX',
  message: '⚠️ Dear Customer, your SBI account will be blocked! Update KYC immediately to avoid suspension:',
  url: 'https://sbi-secure-login.xyz/verify-kyc',
  displayUrl: 'sbi-secure-login.xyz/verify-kyc',
  signals: [
    { name: 'Banking keyword (sbi)', score: 20 },
    { name: 'Suspicious TLD (.xyz)', score: 25 },
    { name: 'Keyword + TLD combo', score: 30 },
    { name: 'Excessive hyphens', score: 15 },
    { name: 'Typo domain pattern', score: 25 },
  ],
  finalScore: 115,
  verdict: 'HIGH_RISK',
}

// ─── Main Hero ────────────────────────────────────────────────────────────────

export default function Hero() {
  const [activeScenario, setActiveScenario] = useState('fraud')
  const [demoKey, setDemoKey] = useState(0)

  const scenario = activeScenario === 'safe' ? SAFE_SCENARIO : FRAUD_SCENARIO

  const switchScenario = (type) => {
    setActiveScenario(type)
    setDemoKey(k => k + 1)
  }

  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden grid-bg pt-20">
      {/* Gradient orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue/20 rounded-full blur-[128px]" />
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-cyan/10 rounded-full blur-[128px]" />

      <div className="relative z-10 mx-auto max-w-7xl px-6 py-20">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          {/* Left: Text */}
          <div className="text-center lg:text-left">
            <motion.h1
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight"
            >
              Protect Your Banking
              <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue to-cyan">
                Before You Click
              </span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.1 }}
              className="mt-6 text-lg text-text-muted max-w-xl leading-relaxed"
            >
              Real-time phishing detection that monitors browser URLs and warns you 
              instantly when a fraudulent banking link is detected. No data collected. 
              No credentials stored.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="mt-8 flex flex-col sm:flex-row items-center gap-4 lg:justify-start justify-center"
            >
              <a
                href="#download"
                className="group px-8 py-4 bg-blue rounded-xl text-white font-semibold flex items-center gap-2 hover:bg-blue-light transition-all glow-blue"
              >
                <Shield className="w-5 h-5" />
                Get Protected
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </a>
              <a
                href="#how-it-works"
                className="px-8 py-4 border border-glass-border rounded-xl text-text-muted font-semibold hover:border-cyan/30 hover:text-cyan transition-all"
              >
                See How It Works
              </a>
            </motion.div>
          </div>

          {/* Right: Interactive Phone Demo */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.3 }}
            className="flex flex-col items-center"
          >
            {/* Scenario toggle */}
            <div className="flex items-center gap-2 mb-5 p-1 bg-navy-light rounded-xl border border-glass-border">
              <button
                onClick={() => switchScenario('safe')}
                className={`px-4 py-2 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                  activeScenario === 'safe'
                    ? 'bg-success/20 text-success border border-success/30'
                    : 'text-text-muted hover:text-text'
                }`}
              >
                ✓ Safe Link
              </button>
              <button
                onClick={() => switchScenario('fraud')}
                className={`px-4 py-2 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                  activeScenario === 'fraud'
                    ? 'bg-alert/20 text-alert border border-alert/30'
                    : 'text-text-muted hover:text-text'
                }`}
              >
                ⚠ Phishing Link
              </button>
            </div>

            {/* Phone frame */}
            <div className="relative w-[280px] h-[560px] rounded-[2.8rem] border-[3px] border-[#1a2744] bg-black shadow-2xl overflow-hidden glow-cyan">
              {/* Notch */}
              <div className="absolute top-0 left-1/2 -translate-x-1/2 w-28 h-6 bg-black rounded-b-2xl z-20 flex items-center justify-center">
                <div className="w-3 h-3 rounded-full bg-[#1a2744] border border-navy-lighter/50" />
              </div>
              {/* Screen */}
              <div className="absolute inset-[3px] rounded-[2.5rem] overflow-hidden">
                <FullPhoneDemo key={demoKey} scenario={scenario} demoKey={demoKey} />
              </div>
              {/* Home indicator */}
              <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-24 h-1 bg-white/20 rounded-full" />
            </div>

            {/* Replay */}
            <button
              onClick={() => setDemoKey(k => k + 1)}
              className="mt-5 px-4 py-2 text-xs text-text-muted hover:text-cyan border border-glass-border rounded-lg hover:border-cyan/30 transition-all cursor-pointer"
            >
              ↻ Replay Demo
            </button>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
