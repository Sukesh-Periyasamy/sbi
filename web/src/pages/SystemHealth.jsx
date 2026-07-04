import { useState, useEffect } from 'react'
import { Cpu, Server, Database as DatabaseIcon, Activity, Clock, Layers } from 'lucide-react'
import { motion } from 'framer-motion'

export default function SystemHealth() {
  const [health, setHealth] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('http://localhost:8000/dashboard/system')
      .then(res => res.json())
      .then(data => {
        setHealth(data)
        setLoading(false)
      })
      .catch(console.error)
  }, [])

  if (loading) return <div className="p-8">Loading system health...</div>

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">System Health</h2>
          <p className="text-gray-500">Live telemetry and pipeline infrastructure</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* Database Latency */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex justify-between items-start mb-4">
            <div className="w-12 h-12 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600">
              <DatabaseIcon className="w-6 h-6" />
            </div>
            <span className={`px-2 py-1 rounded text-xs font-bold ${health.database_latency >= 0 && health.database_latency < 50 ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
              {health.database_latency >= 0 ? 'ONLINE' : 'ERROR'}
            </span>
          </div>
          <div className="text-3xl font-bold text-gray-800 mb-1">{health.database_latency} ms</div>
          <div className="text-sm font-medium text-gray-500">PostgreSQL Latency</div>
        </motion.div>

        {/* Redis Latency */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex justify-between items-start mb-4">
            <div className="w-12 h-12 rounded-lg bg-red-50 flex items-center justify-center text-red-600">
              <Server className="w-6 h-6" />
            </div>
            <span className={`px-2 py-1 rounded text-xs font-bold ${health.redis_latency >= 0 && health.redis_latency < 20 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
              {health.redis_latency >= 0 ? 'ONLINE' : 'ERROR'}
            </span>
          </div>
          <div className="text-3xl font-bold text-gray-800 mb-1">{health.redis_latency} ms</div>
          <div className="text-sm font-medium text-gray-500">Redis Cache Latency</div>
        </motion.div>

        {/* Uptime */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex justify-between items-start mb-4">
            <div className="w-12 h-12 rounded-lg bg-green-50 flex items-center justify-center text-green-600">
              <Clock className="w-6 h-6" />
            </div>
            <span className="px-2 py-1 rounded text-xs font-bold bg-green-100 text-green-700">ACTIVE</span>
          </div>
          <div className="text-3xl font-bold text-gray-800 mb-1">{health.uptime}</div>
          <div className="text-sm font-medium text-gray-500">System Uptime</div>
        </motion.div>

        {/* Background Jobs */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-lg bg-purple-50 flex items-center justify-center text-purple-600">
              <Cpu className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-800">{health.feed_jobs}</div>
              <div className="text-sm font-medium text-gray-500">Scheduler Jobs</div>
            </div>
          </div>
          <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
            <div className={`h-full ${health.scheduler_running ? 'bg-purple-500' : 'bg-red-500'}`} style={{ width: '100%' }}></div>
          </div>
        </motion.div>

        {/* Enrichment Queue */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-lg bg-amber-50 flex items-center justify-center text-amber-600">
              <Layers className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-800">{health.enrichment_queue}</div>
              <div className="text-sm font-medium text-gray-500">Pending Enrichment Queue</div>
            </div>
          </div>
          <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
            <div className="h-full bg-amber-500" style={{ width: `${Math.min(100, (health.enrichment_queue / 500) * 100)}%` }}></div>
          </div>
        </motion.div>

        {/* Cache Hit Rate */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Activity className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-800">{health.cache_hit_rate}%</div>
              <div className="text-sm font-medium text-gray-500">Redis Cache Hit Rate</div>
            </div>
          </div>
          <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
            <div className="h-full bg-emerald-500" style={{ width: `${health.cache_hit_rate}%` }}></div>
          </div>
        </motion.div>

      </div>
    </div>
  )
}
