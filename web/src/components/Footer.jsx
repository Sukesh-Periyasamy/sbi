import { Link, useNavigate, useLocation } from 'react-router-dom'

function SectionLink({ section, children }) {
  const navigate = useNavigate()
  const location = useLocation()

  const handleClick = (e) => {
    e.preventDefault()
    if (location.pathname !== '/') {
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

  return (
    <a
      href={`/#${section}`}
      onClick={handleClick}
      className="text-text-muted text-sm hover:text-cyan transition-colors"
    >
      {children}
    </a>
  )
}

export default function Footer() {
  return (
    <footer className="border-t border-glass-border bg-navy-light">      <div className="mx-auto max-w-7xl px-6 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div className="md:col-span-2">
            <Link to="/" className="flex items-center gap-2 mb-4">
              <img src="/logo.png" alt="AnteClick" className="w-6 h-6" />
              <span className="text-lg font-bold text-blue">AnteClick</span>
            </Link>
            <p className="text-text-muted text-sm max-w-md leading-relaxed">
              AI-powered phishing protection for mobile banking users. 
              Detect fraudulent links before you click. No data collected, 
              no credentials stored.
            </p>
          </div>

          {/* Links */}
          <div>
            <h4 className="text-text font-semibold text-sm mb-4">Product</h4>
            <ul className="space-y-2">
              <li><SectionLink section="features">Features</SectionLink></li>
              <li><SectionLink section="how-it-works">How It Works</SectionLink></li>
              <li><SectionLink section="security">Security</SectionLink></li>
              <li><SectionLink section="faq">FAQ</SectionLink></li>
              <li><a href="https://test.anteclick.app" target="_blank" rel="noopener noreferrer" className="text-text-muted text-sm hover:text-cyan transition-colors">Test Links ↗</a></li>
            </ul>
          </div>

          <div>
            <h4 className="text-text font-semibold text-sm mb-4">Legal</h4>
            <ul className="space-y-2">
              <li><Link to="/privacy-policy" className="text-text-muted text-sm hover:text-cyan transition-colors">Privacy Policy</Link></li>
              <li><Link to="/terms-and-conditions" className="text-text-muted text-sm hover:text-cyan transition-colors">Terms & Conditions</Link></li>
              <li><a href="mailto:hello@anteclick.app" className="text-text-muted text-sm hover:text-cyan transition-colors">Contact</a></li>
            </ul>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-glass-border flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-text-muted text-xs">
            © {new Date().getFullYear()} AnteClick. All rights reserved.
          </p>
          <p className="text-text-muted text-xs">
            Built with privacy-first architecture. No banking credentials stored.
          </p>
        </div>
      </div>
    </footer>
  )
}
