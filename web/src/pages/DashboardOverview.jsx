import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Shield, AlertTriangle, Activity, Zap } from 'lucide-react'
import { Bar, Doughnut } from 'react-chartjs-2'
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend } from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend)

function StatCard({ icon: Icon, value, label, color, delay }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center gap-4"
    >
      <div className={`w-14 h-14 rounded-xl ${color} flex items-center justify-center shrink-0`}>
        <Icon className="w-7 h-7 text-white" />
      </div>
      <div>
        <div className="text-3xl font-bold text-gray-800">
          {value.toLocaleString()}
        </div>
        <div className="text-sm font-medium text-gray-500 mt-1">{label}</div>
      </div>
    </motion.div>
  )
}

export default function DashboardOverview() {
  const [overview, setOverview] = useState(null)
  const [feed, setFeed] = useState([])
  const [geo, setGeo] = useState([])
  
  useEffect(() => {
    fetch('http://localhost:8000/dashboard/overview')
      .then(res => res.json())
      .then(setOverview)
      .catch(console.error)

    fetch('http://localhost:8000/dashboard/live-feed')
      .then(res => res.json())
      .then(setFeed)
      .catch(console.error)
      
    fetch('http://localhost:8000/dashboard/geolocation')
      .then(res => res.json())
      .then(setGeo)
      .catch(console.error)
  }, [])

  if (!overview) return <div className="p-8">Loading overview...</div>

  const donutData = {
    labels: overview.source_apps.map(a => a.app),
    datasets: [{
      data: overview.source_apps.map(a => a.count),
      backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
      borderWidth: 0,
    }]
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard delay={0.1} icon={Shield} value={overview.total_detections} label="Total Threats Blocked" color="bg-blue-600" />
        <StatCard delay={0.2} icon={AlertTriangle} value={overview.high_risk_threats} label="High Risk Detections" color="bg-red-500" />
        <StatCard delay={0.3} icon={Activity} value={overview.safe_domains} label="Safe Whitelisted" color="bg-green-500" />
        <StatCard delay={0.4} icon={Zap} value={Math.round(overview.avg_latency_ms)} label="Avg Detection Time (ms)" color="bg-purple-500" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Live Feed */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-bold text-gray-800 mb-6 flex items-center gap-2">
            <Activity className="h-5 w-5 text-blue-600" /> Live Threat Feed
          </h3>
          <div className="space-y-3">
            {feed.map((item, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                className="flex items-center gap-4 p-3 rounded-lg border border-gray-100 bg-slate-50 hover:bg-slate-100 transition-colors"
              >
                <div className={`w-2 h-2 rounded-full ${item.verdict === 'HIGH_RISK' ? 'bg-red-500' : 'bg-amber-500'}`} />
                <span className="text-xs text-gray-500 font-mono w-16">{new Date(item.timestamp).toLocaleTimeString()}</span>
                <span className={`text-xs px-2 py-1 rounded font-bold ${item.verdict === 'HIGH_RISK' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                  {item.verdict === 'HIGH_RISK' ? 'HIGH' : 'WARN'}
                </span>
                <span className="text-sm font-medium text-gray-800 truncate flex-1">{item.domain}</span>
                <span className="text-xs text-gray-500 bg-white px-2 py-1 border border-gray-200 rounded">{item.source_app}</span>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Source Apps Doughnut */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-bold text-gray-800 mb-6">Attack Vectors</h3>
          <div className="h-64 flex justify-center">
            {overview.source_apps.length > 0 ? (
              <Doughnut data={donutData} options={{ maintainAspectRatio: false, cutout: '70%' }} />
            ) : (
              <div className="text-gray-400 mt-10">No vector data</div>
            )}
          </div>
        </div>

      </div>
    </div>
  )
}
