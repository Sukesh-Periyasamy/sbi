import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Shield, AlertTriangle, ShieldCheck, ExternalLink, Smartphone } from 'lucide-react'

export default function BrandIntel() {
  const [brands, setBrands] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function fetchBrands() {
      try {
        const response = await fetch('http://localhost:8000/dashboard/brand-intelligence')
        if (!response.ok) throw new Error('Failed to fetch brand intelligence')
        const data = await response.json()
        setBrands(data.sort((a, b) => b.total_attacks_detected - a.total_attacks_detected))
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    fetchBrands()
  }, [])

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-600 p-4 rounded-xl border border-red-100 flex items-start gap-3">
        <AlertTriangle className="h-5 w-5 shrink-0" />
        <p className="font-medium">Error loading brand intelligence: {error}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Brand Intelligence</h2>
          <p className="text-gray-500">Live monitoring of targeted financial institutions</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {brands.map((brand, idx) => (
          <motion.div
            key={brand.brand_key}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.1 }}
            className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow"
          >
            <div className="p-6 border-b border-gray-100 bg-slate-50 flex justify-between items-start">
              <div className="flex gap-4 items-center">
                <div className="w-12 h-12 rounded-lg bg-white shadow-sm border border-gray-200 flex items-center justify-center p-2 overflow-hidden">
                  <img src={brand.logo} alt={brand.name} className="max-w-full max-h-full object-contain" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900">{brand.name}</h3>
                  <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{brand.category}</span>
                </div>
              </div>
            </div>
            
            <div className="p-6 space-y-4">
              <div className="flex justify-between items-center p-3 bg-red-50 rounded-lg border border-red-100">
                <span className="text-sm font-semibold text-red-800 flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4" /> Total Attacks
                </span>
                <span className="text-lg font-bold text-red-600">{brand.total_attacks_detected.toLocaleString()}</span>
              </div>
              
              <div className="flex justify-between items-center p-3 bg-purple-50 rounded-lg border border-purple-100">
                <span className="text-sm font-semibold text-purple-800 flex items-center gap-2">
                  <Shield className="h-4 w-4" /> Active Campaigns
                </span>
                <span className="text-lg font-bold text-purple-600">{brand.active_campaigns}</span>
              </div>

              <div className="pt-4 border-t border-gray-100 space-y-2">
                <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">Official Assets</h4>
                
                <div className="space-y-2">
                  {brand.official_domains.map(d => (
                    <div key={d} className="flex items-center gap-2 text-sm text-gray-600">
                      <ExternalLink className="h-4 w-4 text-gray-400 shrink-0" />
                      <span className="truncate">{d}</span>
                      <ShieldCheck className="h-4 w-4 text-green-500 ml-auto shrink-0" />
                    </div>
                  ))}
                  {brand.official_packages.map(p => (
                    <div key={p} className="flex items-center gap-2 text-sm text-gray-600">
                      <Smartphone className="h-4 w-4 text-gray-400 shrink-0" />
                      <span className="truncate" title={p}>{p}</span>
                      <ShieldCheck className="h-4 w-4 text-green-500 ml-auto shrink-0" />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  )
}
