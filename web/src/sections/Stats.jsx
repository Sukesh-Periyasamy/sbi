import { useEffect, useRef, useState } from 'react'
import { motion } from 'framer-motion'

const metrics = [
  { value: 16, label: 'Heuristic Signals', suffix: '', prefix: '' },
  { value: 300, label: 'Detection Speed', suffix: 'ms', prefix: '<' },
  { value: 0, label: 'Data Collected', suffix: 'bytes', prefix: '' },
  { value: 99, label: 'Detection Accuracy', suffix: '%', prefix: '' },
]

function CountUp({ target, prefix, suffix }) {
  const [count, setCount] = useState(0)
  const [hasStarted, setHasStarted] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !hasStarted) {
          setHasStarted(true)
        }
      },
      { threshold: 0.3 }
    )

    observer.observe(el)
    return () => observer.disconnect()
  }, [hasStarted])

  useEffect(() => {
    if (!hasStarted) return
    if (target === 0) { setCount(0); return }

    const duration = 2000
    const startTime = Date.now()

    const timer = setInterval(() => {
      const elapsed = Date.now() - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      setCount(Math.round(eased * target))

      if (progress >= 1) clearInterval(timer)
    }, 16)

    return () => clearInterval(timer)
  }, [hasStarted, target])

  return (
    <span ref={ref}>
      {prefix}{count}{suffix}
    </span>
  )
}

export default function Stats() {
  return (
    <section className="relative py-20 bg-white">
      <div className="mx-auto max-w-7xl px-6">
        <div className="glass-card rounded-3xl p-8 md:p-12">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-8">
            {metrics.map((metric, i) => (
              <motion.div
                key={metric.label}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.3 }}
                transition={{ duration: 0.5, delay: i * 0.1 }}
                className="text-center"
              >
                <div className="text-3xl md:text-5xl font-bold text-text">
                  <CountUp
                    target={metric.value}
                    prefix={metric.prefix}
                    suffix=""
                  />
                  <span className="text-cyan">{metric.suffix}</span>
                </div>
                <p className="mt-2 text-text-muted text-sm">{metric.label}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
