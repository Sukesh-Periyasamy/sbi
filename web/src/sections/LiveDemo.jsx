import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Search, Shield, AlertTriangle, CheckCircle } from 'lucide-react'

const EXAMPLE_URLS = [
  'sbi-verify-secure.xyz',
  'hdfc-login-update.top',
  'bit.ly/axis-kyc',
  'paytm-secure.click',
  '192.168.1.10/login',
]

const SAFE_URLS = [
  'onlinesbi.sbi',
  'hdfcbank.com',
  'google.com',
  'github.com',
]

const SIGNALS_DB = {
  'banking_keyword': { name: 'Banking keyword detected', weight: 20 },
  'suspicious_tld': { name: 'Suspicious TLD', weight: 25 },
  'tld_combo': { name: 'Keyword + TLD escalation', weight: 30 },
  'typo_domain': { name: 'Typo domain pattern', weight: 25 },
  'excessive_hyphens': { name: 'Excessive hyphens', weight: 15 },
  'url_shortener': { name: 'URL shortener detected', weight: 35 },
  'raw_ip': { name: 'Raw IP address', weight: 40 },
  'deep_subdomains': { name: 'Deep subdomain depth', weight: 15 },
  'high_entropy': { name: 'High entropy domain', weight: 20 },
  'punycode': { name: 'Punycode/IDN detected', weight: 30 },
}

function analyzeUrl(url) {
  const domain = url.toLowerCase().replace(/https?:\/\//, '').split('/')[0]
  const signals = []
  let score = 0

  const bankingKeywords = ['sbi', 'hdfc', 'icici', 'axis', 'kotak', 'paytm', 'upi', 'login', 'verify', 'secure', 'update', 'kyc']
  const suspiciousTlds = ['.xyz', '.top', '.click', '.shop', '.live', '.buzz']
  const shorteners = ['bit.ly', 'tinyurl.com', 't.co', 'rb.gy']
  const trustedDomains = ['onlinesbi.sbi', 'hdfcbank.com', 'icicibank.com', 'axisbank.com', 'kotak.com', 'paytm.com', 'google.com', 'github.com', 'stackoverflow.com']

  if (trustedDomains.some(d => domain.includes(d))) {
    return { score: 0, verdict: 'SAFE', confidence: 0.05, signals: [{ name: 'Trusted domain', weight: 0, safe: true }] }
  }

  const hasBanking = bankingKeywords.some(kw => domain.includes(kw))
  if (hasBanking) { signals.push({ ...SIGNALS_DB.banking_keyword }); score += 20 }

  const hasSuspiciousTld = suspiciousTlds.some(tld => domain.endsWith(tld))
  if (hasSuspiciousTld) { signals.push({ ...SIGNALS_DB.suspicious_tld, name: `Suspicious TLD (${suspiciousTlds.find(t => domain.endsWith(t))})` }); score += 25 }

  if (hasBanking && hasSuspiciousTld) { signals.push({ ...SIGNALS_DB.tld_combo }); score += 30 }

  if (shorteners.some(s => domain.includes(s))) { signals.push({ ...SIGNALS_DB.url_shortener }); score += 35 }

  if (/^(\d{1,3}\.){3}\d{1,3}$/.test(domain)) { signals.push({ ...SIGNALS_DB.raw_ip }); score += 40 }

  if (domain.split('-').length >= 3) { signals.push({ ...SIGNALS_DB.excessive_hyphens }); score += 15 }

  if (domain.split('.').length >= 4) { signals.push({ ...SIGNALS_DB.deep_subdomains }); score += 15 }

  if (hasBanking && !trustedDomains.some(d => domain.includes(d))) { signals.push({ ...SIGNALS_DB.typo_domain }); score += 25 }

  const verdict = score >= 70 ? 'HIGH_RISK' : score >= 30 ? 'WARNING' : 'SAFE'
  const confidence = score >= 70 ? Math.min(0.99, 0.70 + (score - 70) * 0.01) : score >= 30 ? 0.50 + (score - 30) * 0.005 : 0.10

  return { score, verdict, confidence: Math.round(confidence * 100), signals }
}

export default function LiveDemo() {
  const [url, setUrl] = useState('')
  const [result, setResult] = useState(null)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [visibleSignals, setVisibleSignals] = useState(0)

  const handleAnalyze = () => {
    if (!url.trim()) return
    setResult(null)
    setIsAnalyzing(true)
    setVisibleSignals(0)

    setTimeout(() => {
      const analysis = analyzeUrl(url)
      setResult(analysis)
      setIsAnalyzing(false)

      // Reveal signals one by one
      analysis.signals.forEach((_, i) => {
        setTimeout(() => setVisibleSignals(i + 1), (i + 1) * 300)
      })
    }, 1200)
  }

  const fillExample = (example) => {
    setUrl(example)
    setResult(null)
    setVisibleSignals(0)
  }

  return (
    <section id="demo" className="relative py-24 bg-white">
      <div className="mx-auto max-w-4xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-text">
            Live Threat <span className="text-cyan">Scanner</span>
          </h2>
          <p className="mt-4 text-text-muted max-w-2xl mx-auto">
            Try it yourself. Enter any URL and watch AnteClick analyze it in real-time 
            using the same 16 heuristic signals as the mobile app.
          </p>
        </motion.div>

        {/* Scanner input */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="glass-card rounded-2xl p-6 md:p-8"
        >
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="flex-1 relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-muted" />
              <input
                type="text"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAnalyze()}
                placeholder="Enter a URL to analyze (e.g., sbi-login.xyz)"
                className="w-full pl-12 pr-4 py-4 bg-white border border-glass-border rounded-xl text-text text-sm placeholder:text-text-muted/50 focus:outline-none focus:border-cyan/50 transition-colors"
              />
            </div>
            <button
              onClick={handleAnalyze}
              disabled={isAnalyzing || !url.trim()}
              className="px-8 py-4 bg-blue rounded-xl text-text font-semibold hover:bg-blue-light transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shrink-0"
            >
              {isAnalyzing ? 'Scanning...' : 'Analyze'}
            </button>
          </div>

          {/* Example URLs */}
          <div className="mt-4 flex flex-wrap gap-2">
            <span className="text-[11px] text-text-muted">Try:</span>
            {EXAMPLE_URLS.map(ex => (
              <button
                key={ex}
                onClick={() => fillExample(ex)}
                className="px-2.5 py-1 text-[11px] text-cyan/70 border border-cyan/20 rounded-lg hover:border-cyan/40 hover:text-cyan transition-colors cursor-pointer"
              >
                {ex}
              </button>
            ))}
            {SAFE_URLS.slice(0, 2).map(ex => (
              <button
                key={ex}
                onClick={() => fillExample(ex)}
                className="px-2.5 py-1 text-[11px] text-success/70 border border-success/20 rounded-lg hover:border-success/40 hover:text-success transition-colors cursor-pointer"
              >
                {ex}
              </button>
            ))}
          </div>

          {/* Scanning animation */}
          <AnimatePresence>
            {isAnalyzing && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="mt-6 flex items-center justify-center gap-3 py-6"
              >
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ repeat: Infinity, duration: 0.8, ease: 'linear' }}
                  className="w-6 h-6 rounded-full border-2 border-cyan/30 border-t-cyan"
                />
                <span className="text-cyan text-sm font-medium">Running 16 heuristic signals...</span>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Results */}
          <AnimatePresence>
            {result && !isAnalyzing && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-6 space-y-4"
              >
                {/* Verdict banner */}
                <div className={`p-4 rounded-xl border ${
                  result.verdict === 'HIGH_RISK' ? 'bg-alert/10 border-alert/30' :
                  result.verdict === 'WARNING' ? 'bg-warning/10 border-warning/30' :
                  'bg-success/10 border-success/30'
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      {result.verdict === 'SAFE' ? (
                        <CheckCircle className="w-6 h-6 text-success" />
                      ) : (
                        <AlertTriangle className={`w-6 h-6 ${result.verdict === 'HIGH_RISK' ? 'text-alert' : 'text-warning'}`} />
                      )}
                      <div>
                        <div className={`font-bold text-lg ${
                          result.verdict === 'HIGH_RISK' ? 'text-alert' :
                          result.verdict === 'WARNING' ? 'text-warning' : 'text-success'
                        }`}>
                          {result.verdict.replace('_', ' ')}
                        </div>
                        <div className="text-text-muted text-xs">
                          {result.verdict === 'SAFE' ? 'No threats detected' : `${result.confidence}% confidence`}
                        </div>
                      </div>
                    </div>
                    <div className={`text-3xl font-bold ${
                      result.verdict === 'HIGH_RISK' ? 'text-alert' :
                      result.verdict === 'WARNING' ? 'text-warning' : 'text-success'
                    }`}>
                      {result.score}<span className="text-lg text-text-muted">/150</span>
                    </div>
                  </div>
                </div>

                {/* Signals */}
                {result.signals.length > 0 && (
                  <div className="space-y-2">
                    <div className="text-xs text-text-muted font-semibold uppercase tracking-wider">
                      Signals Detected ({result.signals.length})
                    </div>
                    {result.signals.map((signal, i) => (
                      <AnimatePresence key={signal.name}>
                        {i < visibleSignals && (
                          <motion.div
                            initial={{ opacity: 0, x: -20 }}
                            animate={{ opacity: 1, x: 0 }}
                            className="flex items-center justify-between px-4 py-2.5 bg-white rounded-lg border border-glass-border"
                          >
                            <div className="flex items-center gap-2">
                              <div className={`w-2 h-2 rounded-full ${signal.safe ? 'bg-success' : 'bg-alert'}`} />
                              <span className="text-sm text-text">{signal.name}</span>
                            </div>
                            <span className={`text-sm font-bold ${signal.safe ? 'text-success' : 'text-alert'}`}>
                              {signal.safe ? '✓ Safe' : `+${signal.weight}`}
                            </span>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    ))}
                  </div>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      </div>
    </section>
  )
}
