import { useState, useEffect } from 'react'
import { Target, Calendar, Globe, Database, Network } from 'lucide-react'
import ForceGraph2D from 'react-force-graph-2d'

export default function Campaigns() {
  const [campaigns, setCampaigns] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('http://localhost:8000/dashboard/campaigns')
      .then(res => res.json())
      .then(data => {
        setCampaigns(data)
        setLoading(false)
      })
      .catch(console.error)
  }, [])

  if (loading) return <div className="p-8">Loading campaigns...</div>

  // Mock a graph specifically for a campaign for the visual showcase
  const renderGraph = (campaignName) => {
    const data = {
      nodes: [
        { id: campaignName, label: campaignName, type: 'campaign' },
        { id: 'Target Brand', label: 'Target: Financials', type: 'brand' },
        { id: 'Domain1', label: 'secure-login.xyz', type: 'domain' },
        { id: 'Domain2', label: 'verify-kyc.top', type: 'domain' }
      ],
      links: [
        { source: 'Domain1', target: campaignName },
        { source: 'Domain2', target: campaignName },
        { source: campaignName, target: 'Target Brand' }
      ]
    }

    return (
      <div className="h-48 w-full bg-slate-50 rounded-lg overflow-hidden border border-gray-100 relative mt-4">
        <ForceGraph2D
          graphData={data}
          nodeLabel="label"
          linkColor={() => '#cbd5e1'}
          nodeColor={n => n.type === 'campaign' ? '#a855f7' : (n.type === 'brand' ? '#3b82f6' : '#ef4444')}
          nodeRelSize={4}
          width={400}
          height={200}
        />
        <div className="absolute top-2 left-2 flex items-center gap-1 text-[10px] text-gray-500 font-medium bg-white/80 px-2 py-1 rounded shadow-sm">
          <Network className="w-3 h-3" /> Infrastructure Map
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Active Threat Campaigns</h2>
          <p className="text-gray-500">Track coordinated phishing efforts across the internet</p>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        {campaigns.map((camp, idx) => (
          <div key={idx} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow flex flex-col">
            <div className="p-6 border-b border-gray-100 bg-slate-50 flex items-start justify-between">
              <div>
                <h3 className="text-lg font-bold text-gray-800 flex items-center gap-2">
                  <Target className="h-5 w-5 text-purple-600" />
                  {camp.campaign_id}
                </h3>
                <div className="text-sm font-medium text-gray-500 mt-1 flex items-center gap-4">
                  <span className="flex items-center gap-1"><Globe className="w-4 h-4" /> {camp.domains_count} Domains</span>
                  <span className="flex items-center gap-1"><Calendar className="w-4 h-4" /> Seen {new Date(camp.first_seen).toLocaleDateString()}</span>
                </div>
              </div>
              <div className="bg-purple-100 text-purple-800 text-xs font-bold px-3 py-1 rounded-full border border-purple-200">
                ACTIVE
              </div>
            </div>

            <div className="p-6 flex-1 flex flex-col justify-between">
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
                  <div className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Top Registrar</div>
                  <div className="font-semibold text-gray-800 break-words">{camp.top_registrars[0] || 'Unknown'}</div>
                </div>
                <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
                  <div className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Target Brand</div>
                  <div className="font-semibold text-blue-600 break-words">{camp.top_brands[0] || 'Generic'}</div>
                </div>
              </div>

              <div className="flex items-center gap-2 mb-2 text-sm text-gray-600">
                <Database className="w-4 h-4 text-gray-400" />
                <span className="font-medium">Detected via Feeds:</span>
                <div className="flex gap-1 ml-auto">
                  {camp.top_feeds.slice(0, 3).map(feed => (
                    <span key={feed} className="bg-blue-50 text-blue-700 text-[10px] font-bold px-2 py-0.5 rounded border border-blue-200">{feed}</span>
                  ))}
                </div>
              </div>

              {renderGraph(camp.campaign_id)}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
