import { useState, useEffect, useCallback, useRef } from 'react'
import { motion, AnimatePresence, useInView } from 'framer-motion'
import { Shield, AlertTriangle, Activity, Zap, TrendingUp, Globe, Smartphone, Search, Play, RefreshCw } from 'lucide-react'
import { Bar, Doughnut, Line } from 'react-chartjs-2'
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, ArcElement, PointElement, LineElement, Title, Tooltip, Legend, Filler } from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, PointElement, LineElement, Title, Tooltip, Legend, Filler)

// ─── Animated Counter ─────────────────────────────────────────────────────────

function AnimatedCounter({ value, duration = 1500 }) {
  const [count, setCount] = useState(0)
  const ref = useRef(null)
  const inView = useInView(ref, { once: false, amount: 0.3 })
  const prevValue = useRef(0)

  useEffect(() => {
    if (!inView) return
    const start = prevValue.current
    const end = value
    prevValue.current = value
    const startTime = Date.now()

    const timer = setInterval(() => {
      const elapsed = Date.now() - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      setCount(Math.round(start + (end - start) * eased))
      if (progress >= 1) clearInterval(timer)
    }, 16)

    return () => clearInterval(timer)
  }, [value, inView, duration])

  return <span ref={ref}>{count.toLocaleString()}</span>
}

// ─── Mock Data ────────────────────────────────────────────────────────────────

function generateOverview() {
  return {
    threats_blocked_today: Math.floor(800 + Math.random() * 700),
    high_risk_detections: Math.floor(200 + Math.random() * 200),
    fake_banking_apps: Math.floor(50 + Math.random() * 70),
    avg_detection_ms: Math.floor(180 + Math.random() * 140),
  }
}

const feedItems = [
  { target: 'sbi-secure-login.xyz', risk: 'HIGH_RISK', source: 'Chrome', method: 'Heuristic' },
  { target: 'com.paytm.verify.kyc', risk: 'HIGH_RISK', source: 'Sideload', method: 'Package Scorer' },
  { target: 'hdfc-verify-account.top', risk: 'HIGH_RISK', source: 'WhatsApp', method: 'Blacklist' },
  { target: 'com.phonepe.reward.claim', risk: 'WARNING', source: 'Telegram', method: 'Package Scorer' },
  { target: 'icici-kyc-update.shop', risk: 'HIGH_RISK', source: 'SMS', method: 'Heuristic' },
  { target: 'axis-bonus-claim.live', risk: 'WARNING', source: 'Chrome', method: 'Backend' },
  { target: 'com.sbi.verify.secure', risk: 'HIGH_RISK', source: 'Sideload', method: 'Package Scorer' },
  { target: 'upi-verify-secure.click', risk: 'HIGH_RISK', source: 'WhatsApp', method: 'Heuristic' },
  { target: 'bank-otp-verify.xyz', risk: 'HIGH_RISK', source: 'Telegram', method: 'Blacklist' },
  { target: 'com.hdfc.secure.login', risk: 'WARNING', source: 'Chrome', method: 'Package Scorer' },
  { target: 'kotak-reward-bonus.xyz', risk: 'HIGH_RISK', source: 'SMS', method: 'Heuristic' },
  { target: 'com.axis.gift.reward', risk: 'WARNING', source: 'Sideload', method: 'Package Scorer' },
]

const topBanks = [
  { bank: 'SBI', percentage: 45 },
  { bank: 'HDFC', percentage: 22 },
  { bank: 'ICICI', percentage: 18 },
  { bank: 'Axis', percentage: 9 },
  { bank: 'Paytm', percentage: 6 },
]

const sourceApps = [
  { app: 'Chrome', percentage: 48 },
  { app: 'WhatsApp', percentage: 27 },
  { app: 'Telegram', percentage: 13 },
  { app: 'SMS/Sideload', percentage: 8 },
  { app: 'Instagram', percentage: 4 },
]

const detectionTypes = [
  { type: 'Typosquatting', percentage: 32 },
  { type: 'Suspicious TLD', percentage: 28 },
  { type: 'Fake Banking App', percentage: 21 },
  { type: 'Homoglyph', percentage: 10 },
  { type: 'Accessibility Abuse', percentage: 9 },
]

const topKeywords = [
  { keyword: 'verify', count: 342 },
  { keyword: 'KYC', count: 289 },
  { keyword: 'reward', count: 234 },
  { keyword: 'secure', count: 198 },
  { keyword: 'update', count: 176 },
  { keyword: 'UPI', count: 156 },
  { keyword: 'OTP', count: 134 },
  { keyword: 'login', count: 112 },
]

const heatmapData = [
  { state: 'Maharashtra', count: 380 },
  { state: 'Delhi', count: 290 },
  { state: 'Uttar Pradesh', count: 260 },
  { state: 'Tamil Nadu', count: 220 },
  { state: 'Karnataka', count: 195 },
  { state: 'Telangana', count: 175 },
  { state: 'Rajasthan', count: 150 },
  { state: 'Gujarat', count: 130 },
  { state: 'West Bengal', count: 120 },
  { state: 'Kerala', count: 95 },
]

// ─── Animation Variants ───────────────────────────────────────────────────────

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
}

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
}

const slideIn = {
  hidden: { opacity: 0, x: -30 },
  show: { opacity: 1, x: 0, transition: { duration: 0.5 } },
}

const scaleIn = {
  hidden: { opacity: 0, scale: 0.8 },
  show: { opacity: 1, scale: 1, transition: { duration: 0.5, type: 'spring' } },
}

// ─── Chart Options ────────────────────────────────────────────────────────────

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 1500, easing: 'easeOutQuart' },
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { color: 'rgba(56,189,248,0.05)' }, ticks: { color: '#94A3B8', font: { size: 10 } } },
    y: { grid: { color: 'rgba(56,189,248,0.05)' }, ticks: { color: '#94A3B8', font: { size: 10 } } },
  },
}

// ─── Components ───────────────────────────────────────────────────────────────

function StatCard({ icon: Icon, label, value, color, delay }) {
  return (
    <motion.div
      variants={scaleIn}
      className="glass-card rounded-xl p-5 flex items-center gap-4 hover:border-cyan/20 transition-all group"
    >
      <motion.div
        className={`w-12 h-12 rounded-xl ${color} flex items-center justify-center shrink-0`}
        whileHover={{ scale: 1.1, rotate: 5 }}
        transition={{ type: 'spring', stiffness: 300 }}
      >
        <Icon className="w-6 h-6 text-text" />
      </motion.div>
      <div>
        <div className="text-2xl md:text-3xl font-bold text-text">
          <AnimatedCounter value={value} />
        </div>
        <div className="text-xs text-text-muted mt-0.5">{label}</div>
      </div>
    </motion.div>
  )
}

function LiveFeedItem({ item: feedItem, index }) {
  const riskColor = feedItem.risk === 'HIGH_RISK' ? 'text-alert' : 'text-warning'
  const riskBg = feedItem.risk === 'HIGH_RISK' ? 'bg-alert/10 border-alert/20' : 'bg-warning/10 border-warning/20'
  const dotColor = feedItem.risk === 'HIGH_RISK' ? 'bg-alert' : 'bg-warning'

  return (
    <motion.div
      initial={{ opacity: 0, x: -20, height: 0 }}
      animate={{ opacity: 1, x: 0, height: 'auto' }}
      exit={{ opacity: 0, x: 20, height: 0 }}
      transition={{ duration: 0.3, delay: index * 0.05 }}
      className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-gray-200/30 transition-all border border-transparent hover:border-glass-border"
    >
      <motion.div
        animate={{ scale: [1, 1.3, 1] }}
        transition={{ repeat: Infinity, duration: 2, delay: index * 0.3 }}
        className={`w-2 h-2 rounded-full ${dotColor} shrink-0`}
      />
      <span className="text-[10px] text-text-muted w-12 shrink-0 font-mono">{feedItem.timestamp}</span>
      <span className={`text-[10px] px-2 py-0.5 rounded border ${riskBg} ${riskColor} font-bold shrink-0`}>
        {feedItem.risk === 'HIGH_RISK' ? 'HIGH' : 'WARN'}
      </span>
      <span className="text-xs text-text truncate flex-1 font-mono">{feedItem.target}</span>
      <span className="text-[10px] text-text-muted shrink-0 hidden sm:block">{feedItem.source}</span>
    </motion.div>
  )
}

function HeatmapBar({ state, count, maxCount, index }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: 0.1 + index * 0.06 }}
      className="flex items-center gap-2"
    >
      <span className="text-[11px] text-text-muted w-24 shrink-0">{state}</span>
      <div className="flex-1 h-3.5 bg-gray-200 rounded-full overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${(count / maxCount) * 100}%` }}
          transition={{ duration: 1, delay: 0.3 + index * 0.08, ease: 'easeOut' }}
          className="h-full bg-gradient-to-r from-blue to-cyan rounded-full"
        />
      </div>
      <span className="text-[11px] text-cyan font-mono w-8 text-right">{count}</span>
    </motion.div>
  )
}

// ─── Main Dashboard ───────────────────────────────────────────────────────────

export default function Dashboard() {
  const [overview, setOverview] = useState(generateOverview())
  const [feed, setFeed] = useState([])
  const [simulating, setSimulating] = useState(false)
  const [feedKey, setFeedKey] = useState(0)

  // Initialize feed with timestamps
  useEffect(() => {
    const now = Date.now()
    setFeed(feedItems.map((f, i) => ({
      ...f,
      id: `evt-${i}`,
      timestamp: new Date(now - i * 45000).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }),
    })))
  }, [])

  // Auto-refresh overview every 8s
  useEffect(() => {
    const interval = setInterval(() => {
      setOverview(generateOverview())
    }, 8000)
    return () => clearInterval(interval)
  }, [])

  // Auto-add new feed items every 6s
  useEffect(() => {
    const interval = setInterval(() => {
      const randomItem = feedItems[Math.floor(Math.random() * feedItems.length)]
      const newEvent = {
        ...randomItem,
        id: `auto-${Date.now()}`,
        timestamp: new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }),
      }
      setFeed(prev => [newEvent, ...prev.slice(0, 11)])
    }, 6000)
    return () => clearInterval(interval)
  }, [])

  const simulateAttack = useCallback(() => {
    setSimulating(true)
    const attacks = [
      { target: 'com.sbi.verify.secure.kyc', risk: 'HIGH_RISK', source: 'Telegram', method: 'Package Scorer' },
      { target: 'hdfc-netbanking-login.xyz', risk: 'HIGH_RISK', source: 'WhatsApp', method: 'Heuristic' },
      { target: 'com.icici.otp.verify', risk: 'HIGH_RISK', source: 'Sideload', method: 'Package Scorer' },
    ]
    // Add 3 events rapidly
    attacks.forEach((attack, i) => {
      setTimeout(() => {
        const newEvent = {
          ...attack,
          id: `sim-${Date.now()}-${i}`,
          timestamp: new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }),
        }
        setFeed(prev => [newEvent, ...prev.slice(0, 11)])
        setOverview(prev => ({
          ...prev,
          threats_blocked_today: prev.threats_blocked_today + 1,
          high_risk_detections: prev.high_risk_detections + 1,
          fake_banking_apps: prev.fake_banking_apps + (attack.method === 'Package Scorer' ? 1 : 0),
        }))
      }, i * 800)
    })
    setTimeout(() => setSimulating(false), 3000)
  }, [])

  // Chart data
  const timelineData = {
    labels: Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`),
    datasets: [{
      label: 'Detections',
      data: Array.from({ length: 24 }, () => Math.floor(20 + Math.random() * 100)),
      borderColor: '#38BDF8',
      backgroundColor: 'rgba(56,189,248,0.08)',
      fill: true,
      tension: 0.4,
      pointRadius: 0,
      pointHoverRadius: 4,
      pointHoverBackgroundColor: '#38BDF8',
    }],
  }

  const banksData = {
    labels: topBanks.map(b => b.bank),
    datasets: [{
      data: topBanks.map(b => b.percentage),
      backgroundColor: ['#2563EB', '#3B82F6', '#38BDF8', '#22D3EE', '#06B6D4'],
      borderRadius: 6,
      borderSkipped: false,
    }],
  }

  const typesData = {
    labels: detectionTypes.map(d => d.type),
    datasets: [{
      data: detectionTypes.map(d => d.percentage),
      backgroundColor: ['#EF4444', '#F59E0B', '#2563EB', '#38BDF8', '#10B981'],
      borderWidth: 0,
      hoverOffset: 8,
    }],
  }

  const maxHeatmap = Math.max(...heatmapData.map(h => h.count))

  return (
    <main className="pt-20 pb-12 bg-white min-h-screen grid-bg">
      <div className="mx-auto max-w-7xl px-6">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-8 pt-4 gap-4"
        >
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-text flex items-center gap-3">
              <motion.div
                animate={{ rotate: [0, 5, -5, 0] }}
                transition={{ repeat: Infinity, duration: 4, ease: 'easeInOut' }}
              >
                <Shield className="w-7 h-7 text-cyan" />
              </motion.div>
              Threat Intelligence
            </h1>
            <p className="text-text-muted text-sm mt-1">Real-time phishing & fraud analytics</p>
          </div>
          <motion.button
            onClick={simulateAttack}
            disabled={simulating}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
              simulating
                ? 'bg-alert/20 text-alert border border-alert/30 glow-blue'
                : 'bg-blue/20 text-cyan border border-cyan/30 hover:bg-blue/30'
            }`}
          >
            {simulating ? (
              <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}>
                <RefreshCw className="w-4 h-4" />
              </motion.div>
            ) : (
              <Play className="w-4 h-4" />
            )}
            {simulating ? 'Simulating...' : 'Simulate Attack'}
          </motion.button>
        </motion.div>

        {/* Overview Cards */}
        <motion.div
          variants={container}
          initial="hidden"
          animate="show"
          className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8"
        >
          <StatCard icon={Shield} label="Threats Blocked Today" value={overview.threats_blocked_today} color="bg-alert" />
          <StatCard icon={AlertTriangle} label="High Risk Detections" value={overview.high_risk_detections} color="bg-warning" />
          <StatCard icon={Smartphone} label="Fake Banking Apps" value={overview.fake_banking_apps} color="bg-blue" />
          <StatCard icon={Zap} label="Avg Detection (ms)" value={overview.avg_detection_ms} color="bg-cyan/80" />
        </motion.div>

        {/* Main Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Live Feed */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="lg:col-span-2 glass-card rounded-xl p-5"
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-text font-semibold text-sm flex items-center gap-2">
                <Activity className="w-4 h-4 text-cyan" />
                Live Threat Feed
              </h3>
              <div className="flex items-center gap-1.5">
                <motion.div
                  animate={{ opacity: [0.4, 1, 0.4] }}
                  transition={{ repeat: Infinity, duration: 1.5 }}
                  className="w-2 h-2 rounded-full bg-success"
                />
                <span className="text-[10px] text-success font-medium">Live</span>
              </div>
            </div>
            <div className="space-y-1 max-h-[360px] overflow-y-auto">
              <AnimatePresence mode="popLayout">
                {feed.map((f, i) => <LiveFeedItem key={f.id} item={f} index={i} />)}
              </AnimatePresence>
            </div>
          </motion.div>

          {/* Heatmap */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 }}
            className="glass-card rounded-xl p-5"
          >
            <h3 className="text-text font-semibold text-sm flex items-center gap-2 mb-4">
              <Globe className="w-4 h-4 text-cyan" />
              India Attack Density
            </h3>
            <div className="space-y-2.5">
              {heatmapData.map((h, i) => (
                <HeatmapBar key={h.state} state={h.state} count={h.count} maxCount={maxHeatmap} index={i} />
              ))}
            </div>
          </motion.div>

          {/* Timeline */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="lg:col-span-2 glass-card rounded-xl p-5"
          >
            <h3 className="text-text font-semibold text-sm flex items-center gap-2 mb-4">
              <TrendingUp className="w-4 h-4 text-cyan" />
              24-Hour Detection Timeline
            </h3>
            <div className="h-52">
              <Line data={timelineData} options={{ ...chartOptions, animation: { duration: 2000, easing: 'easeOutQuart' } }} />
            </div>
          </motion.div>

          {/* Detection Types */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.6, type: 'spring' }}
            className="glass-card rounded-xl p-5"
          >
            <h3 className="text-text font-semibold text-sm mb-4">Detection Types</h3>
            <div className="h-44 flex items-center justify-center">
              <Doughnut data={typesData} options={{ responsive: true, maintainAspectRatio: false, animation: { animateRotate: true, duration: 1500 }, plugins: { legend: { position: 'bottom', labels: { color: '#94A3B8', font: { size: 9 }, padding: 8, usePointStyle: true } } }, cutout: '65%' }} />
            </div>
          </motion.div>

          {/* Top Banks */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.7 }}
            className="glass-card rounded-xl p-5"
          >
            <h3 className="text-text font-semibold text-sm mb-4">Most Targeted Banks</h3>
            <div className="h-44">
              <Bar data={banksData} options={{ ...chartOptions, animation: { duration: 1800, easing: 'easeOutBounce' } }} />
            </div>
          </motion.div>

          {/* Source Apps */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.8 }}
            className="glass-card rounded-xl p-5"
          >
            <h3 className="text-text font-semibold text-sm mb-4">Threat Source Apps</h3>
            <div className="space-y-3">
              {sourceApps.map((app, i) => (
                <motion.div
                  key={app.app}
                  initial={{ opacity: 0, x: -15 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.9 + i * 0.1 }}
                  className="flex items-center gap-2"
                >
                  <span className="text-xs text-text w-24 shrink-0">{app.app}</span>
                  <div className="flex-1 h-3 bg-gray-200 rounded-full overflow-hidden">
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${app.percentage}%` }}
                      transition={{ duration: 1.2, delay: 1 + i * 0.1, ease: 'easeOut' }}
                      className="h-full bg-gradient-to-r from-blue to-blue-light rounded-full"
                    />
                  </div>
                  <span className="text-xs text-cyan font-mono w-8 text-right">{app.percentage}%</span>
                </motion.div>
              ))}
            </div>
          </motion.div>

          {/* Top Keywords */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.9 }}
            className="glass-card rounded-xl p-5"
          >
            <h3 className="text-text font-semibold text-sm flex items-center gap-2 mb-4">
              <Search className="w-4 h-4 text-cyan" />
              Top Scam Keywords
            </h3>
            <div className="flex flex-wrap gap-2">
              {topKeywords.map((kw, i) => (
                <motion.span
                  key={kw.keyword}
                  initial={{ opacity: 0, scale: 0 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 1 + i * 0.08, type: 'spring', stiffness: 200 }}
                  whileHover={{ scale: 1.1 }}
                  className="px-3 py-1.5 bg-alert/10 border border-alert/20 rounded-lg text-xs text-alert cursor-default"
                >
                  {kw.keyword} <span className="text-text-muted/70">({kw.count})</span>
                </motion.span>
              ))}
            </div>
          </motion.div>
        </div>
      </div>
    </main>
  )
}
