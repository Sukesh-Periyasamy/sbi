import { motion } from 'framer-motion'
import { Shield, ArrowRight } from 'lucide-react'

export default function CTA() {
  return (
    <section id="download" className="relative py-24 bg-navy-light overflow-hidden">
      {/* Background glow */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="w-[600px] h-[600px] bg-blue/10 rounded-full blur-[150px]" />
      </div>

      <div className="relative z-10 mx-auto max-w-4xl px-6 text-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue/20 mb-8">
            <Shield className="w-8 h-8 text-cyan" />
          </div>

          <h2 className="text-3xl md:text-5xl font-bold text-text leading-tight">
            Start Protecting Your
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue to-cyan">
              Banking Today
            </span>
          </h2>

          <p className="mt-6 text-text-muted text-lg max-w-xl mx-auto">
            Download AnteClick and get instant protection against phishing attacks. 
            Free, private, and always watching your back.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <a
              href="#"
              className="group px-8 py-4 bg-blue rounded-xl text-text font-semibold flex items-center gap-3 hover:bg-blue-light transition-all glow-blue"
            >
              <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 01-.61-.92V2.734a1 1 0 01.609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.198l2.807 1.626a1 1 0 010 1.73l-2.808 1.626L15.206 12l2.492-2.491zM5.864 2.658L16.8 8.99l-2.3 2.3-8.636-8.632z"/>
              </svg>
              Get on Google Play
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </a>
            <a
              href="#"
              className="px-8 py-4 border border-glass-border rounded-xl text-text-muted font-semibold hover:border-cyan/30 hover:text-cyan transition-all"
            >
              Download APK
            </a>
          </div>

          <p className="mt-8 text-text-muted text-xs">
            Requires Android 12+. Free to use. No ads. No tracking.
          </p>
        </motion.div>
      </div>
    </section>
  )
}
