import { useState, useEffect } from 'react'
import { Database, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react'

export default function ThreatFeeds() {
  const [feeds, setFeeds] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('http://localhost:8000/dashboard/feeds')
      .then(res => res.json())
      .then(data => {
        setFeeds(data)
        setLoading(false)
      })
      .catch(console.error)
  }, [])

  if (loading) return <div className="p-8">Loading feeds...</div>

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Threat Feeds</h2>
          <p className="text-gray-500">Live background synchronization statuses</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {feeds.map((feed) => (
          <div key={feed.feed_name} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between bg-slate-50">
              <h3 className="font-bold text-gray-800 flex items-center gap-2">
                <Database className="h-5 w-5 text-blue-600" />
                {feed.feed_name}
              </h3>
              {feed.status === 'success' ? (
                <span className="flex items-center text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded-full border border-green-200">
                  <CheckCircle className="w-3 h-3 mr-1" /> HEALTHY
                </span>
              ) : feed.status === 'pending' ? (
                <span className="flex items-center text-xs font-bold text-amber-600 bg-amber-50 px-2 py-1 rounded-full border border-amber-200">
                  <Clock className="w-3 h-3 mr-1" /> PENDING
                </span>
              ) : (
                <span className="flex items-center text-xs font-bold text-red-600 bg-red-50 px-2 py-1 rounded-full border border-red-200">
                  <XCircle className="w-3 h-3 mr-1" /> FAILED
                </span>
              )}
            </div>
            
            <div className="p-6 space-y-4">
              <div className="flex justify-between items-end">
                <div className="text-sm text-gray-500">Records Imported</div>
                <div className="text-2xl font-bold text-gray-800">{feed.records_imported.toLocaleString()}</div>
              </div>
              
              <div className="space-y-2 text-sm">
                <div className="flex justify-between border-b border-gray-100 pb-2">
                  <span className="text-gray-500">Last Update</span>
                  <span className="font-medium text-gray-900">
                    {feed.last_update === 'never' ? 'Never' : new Date(feed.last_update).toLocaleTimeString()}
                  </span>
                </div>
                <div className="flex justify-between border-b border-gray-100 pb-2">
                  <span className="text-gray-500">Duration</span>
                  <span className="font-medium text-gray-900">{feed.duration_seconds.toFixed(2)}s</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Failures</span>
                  <span className={`font-medium ${feed.failures_count > 0 ? 'text-red-600' : 'text-gray-900'}`}>
                    {feed.failures_count}
                  </span>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
