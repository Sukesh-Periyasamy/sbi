import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Menu, X } from 'lucide-react'
import { Link, useNavigate, useLocation } from 'react-router-dom'

const navLinks = [
  { label: 'Features', section: 'features' },
  { label: 'How It Works', section: 'how-it-works' },
  { label: 'Dashboard', section: null, route: '/dashboard' },
  { label: 'SDK', section: null, route: '/sdk' },
  { label: 'Security', section: 'security' },
]

export default function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const scrollToSection = (section) => {
    if (location.pathname !== '/') {
      // Navigate to home first, then scroll after render
      navigate('/')
      setTimeout(() => {
        const el = document.getElementById(section)
        if (el) el.scrollIntoView({ behavior: 'smooth' })
      }, 100)
    } else {
      const el = document.getElementById(section)
      if (el) el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  const handleNavClick = (link) => {
    if (link.route) {
      navigate(link.route)
    } else {
      scrollToSection(link.section)
    }
  }

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-glass-border bg-navy/80 backdrop-blur-xl">
      <div className="mx-auto max-w-7xl px-6 py-4 flex items-center justify-between">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2">
          <img src="/logo.png" alt="AnteClick" className="w-8 h-8" />
          <span className="text-xl font-bold text-white">AnteClick</span>
        </Link>

        {/* Desktop links */}
        <div className="hidden md:flex items-center gap-8">
          {navLinks.map(link => (
            <button
              key={link.label}
              onClick={() => handleNavClick(link)}
              className="text-text-muted hover:text-cyan transition-colors text-sm font-medium cursor-pointer"
            >
              {link.label}
            </button>
          ))}
          <button
            onClick={() => scrollToSection('download')}
            className="px-5 py-2.5 bg-blue rounded-lg text-white text-sm font-semibold hover:bg-blue-light transition-colors cursor-pointer"
          >
            Get Protected
          </button>
        </div>

        {/* Mobile toggle */}
        <button
          className="md:hidden text-text-muted"
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label="Toggle menu"
        >
          {mobileOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* Mobile menu */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="md:hidden border-t border-glass-border bg-navy-light"
          >
            <div className="px-6 py-4 flex flex-col gap-4">
              {navLinks.map(link => (
                <button
                  key={link.label}
                  onClick={() => { handleNavClick(link); setMobileOpen(false) }}
                  className="text-text-muted hover:text-cyan transition-colors text-sm font-medium text-left cursor-pointer"
                >
                  {link.label}
                </button>
              ))}
              <button
                onClick={() => { scrollToSection('download'); setMobileOpen(false) }}
                className="px-5 py-2.5 bg-blue rounded-lg text-white text-sm font-semibold text-center cursor-pointer"
              >
                Get Protected
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  )
}
