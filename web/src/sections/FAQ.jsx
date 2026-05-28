import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ChevronDown } from 'lucide-react'

const faqs = [
  {
    question: 'How does AnteClick detect phishing links?',
    answer: 'AnteClick uses Android\'s Accessibility Service to read URLs from browser address bars. It then applies 16 heuristic signals including banking keyword detection, suspicious TLD analysis, typo domain matching (Levenshtein distance), URL shortener detection, and homograph attack identification. Suspicious URLs are additionally verified against our cloud backend.',
  },
  {
    question: 'Does AnteClick access my passwords or banking data?',
    answer: 'Absolutely not. AnteClick only reads the URL text displayed in your browser\'s address bar. It has zero access to form fields, passwords, OTPs, or any other data on the page. No personal information is ever collected or transmitted.',
  },
  {
    question: 'Why does it need Accessibility Service permission?',
    answer: 'The Accessibility Service is the only way on Android to read browser URL bars without replacing the browser itself. AnteClick uses this permission exclusively to extract URLs for phishing analysis. A full disclosure screen explains this before you enable the service.',
  },
  {
    question: 'Does it work offline?',
    answer: 'Yes. The core threat scoring engine runs entirely on your device with 16 local heuristic signals. The backend API is only consulted for ambiguous cases (WARNING-level threats). If the backend is unreachable, local scoring continues without interruption.',
  },
  {
    question: 'Which browsers are supported?',
    answer: 'AnteClick supports Chrome, Firefox, Brave, Samsung Internet, and in-app browsers within WhatsApp, Telegram, and Instagram. Any browser that exposes its URL bar via Android\'s accessibility tree is supported.',
  },
  {
    question: 'Will it slow down my phone?',
    answer: 'No. AnteClick is event-driven — it only activates when a URL changes in your browser. Detection takes under 300ms and uses minimal battery. There is no continuous background scanning or traffic monitoring.',
  },
  {
    question: 'Is my browsing history stored?',
    answer: 'No. Only detected threats are logged locally on your device (for your reference on the dashboard). Safe URLs are never stored anywhere. The app uses SharedPreferences for threat history — no cloud sync, no analytics.',
  },
  {
    question: 'What Android versions are supported?',
    answer: 'AnteClick requires Android 12 (API 31) or higher. It targets Android 15 (API 35) and is optimized for modern Android devices including MIUI/HyperOS.',
  },
]

export default function FAQ() {
  const [openIndex, setOpenIndex] = useState(null)

  return (
    <section id="faq" className="relative py-24 bg-navy">
      <div className="mx-auto max-w-3xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-white">
            Frequently Asked <span className="text-cyan">Questions</span>
          </h2>
        </motion.div>

        <div className="space-y-3">
          {faqs.map((faq, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: i * 0.05 }}
              className="glass-card rounded-xl overflow-hidden"
            >
              <button
                onClick={() => setOpenIndex(openIndex === i ? null : i)}
                className="w-full px-6 py-5 flex items-center justify-between text-left"
              >
                <span className="text-white font-medium text-sm md:text-base pr-4">
                  {faq.question}
                </span>
                <ChevronDown
                  className={`w-5 h-5 text-cyan shrink-0 transition-transform ${
                    openIndex === i ? 'rotate-180' : ''
                  }`}
                />
              </button>
              <AnimatePresence>
                {openIndex === i && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.3 }}
                    className="overflow-hidden"
                  >
                    <p className="px-6 pb-5 text-text-muted text-sm leading-relaxed">
                      {faq.answer}
                    </p>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
