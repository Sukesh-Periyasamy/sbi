import { useState, useEffect, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ForceGraph2D } from 'react-force-graph-2d'
import { Search, Shield, AlertTriangle, Link as LinkIcon, Globe, Lock, Key, Activity, Clock } from 'lucide-react'

export default function ThreatExplorer() {
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') || ''
  
  const [searchInput, setSearchInput] = useState(query)
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  
  const graphRef = useRef()

  const handleSearch = async (e) => {
    e?.preventDefault()
    if (!searchInput || searchInput.length < 2) return
    
    setSearchParams({ q: searchInput })
    fetchData(searchInput)
  }

  const fetchData = async (q) => {
    setLoading(true)
    setError('')
    try {
      const response = await fetch(`http://localhost:8000/intel/search?q=${encodeURIComponent(q)}`)
      if (!response.ok) throw new Error('Search failed')
      const json = await response.json()
      setData(json)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (query) {
      setSearchInput(query)
      fetchData(query)
    }
  }, [query])

  // Custom node drawing for ForceGraph
  const paintNode = (node, ctx, globalScale) => {
    const label = node.label
    const fontSize = 12 / globalScale
    ctx.font = `${fontSize}px Sans-Serif`
    const textWidth = ctx.measureText(label).width
    const bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2)

    let color = '#94a3b8' // default registrar/ssl
    if (node.type === 'domain') color = node.risk_score >= 70 ? '#ef4444' : (node.risk_score >= 40 ? '#f59e0b' : '#22c55e')
    if (node.type === 'brand') color = '#3b82f6'
    if (node.type === 'campaign') color = '#a855f7'

    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)'
    ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions)
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillStyle = color
    ctx.fillText(label, node.x, node.y)

    node.__bckgDimensions = bckgDimensions
  }

  const nodePointerAreaPaint = (node, color, ctx) => {
    ctx.fillStyle = color
    const bckgDimensions = node.__bckgDimensions
    bckgDimensions && ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions)
  }

  return (
    <div className="flex flex-col lg:flex-row gap-6 h-full font-sans">
      
      {/* Sidebar Details */}
      <div className="w-full lg:w-96 flex flex-col gap-6 shrink-0 overflow-y-auto">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
            <Search className="h-5 w-5 text-blue-600" />
            Threat Explorer
          </h2>
          
          <form onSubmit={handleSearch} className="flex gap-2">
            <input 
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Domain, Brand, or Registrar..."
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button 
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              Search
            </button>
          </form>
        </div>

        {loading && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 flex justify-center items-center">
            <div className="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
          </div>
        )}

        {error && (
          <div className="bg-red-50 text-red-600 p-4 rounded-xl border border-red-100 flex items-start gap-3">
            <AlertTriangle className="h-5 w-5 shrink-0" />
            <p className="text-sm font-medium">{error}</p>
          </div>
        )}

        {data && data.domains.map((domain, idx) => (
          <div key={idx} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className={`h-2 ${domain.risk_score >= 70 ? 'bg-red-500' : (domain.risk_score >= 40 ? 'bg-amber-500' : 'bg-green-500')}`} />
            <div className="p-6">
              <h3 className="text-lg font-bold text-gray-800 break-all">{domain.domain}</h3>
              
              <div className="mt-4 mb-6">
                <div className="flex justify-between text-sm mb-1">
                  <span className="font-semibold text-gray-700">Threat Score</span>
                  <span className={`font-bold ${domain.risk_score >= 70 ? 'text-red-600' : 'text-amber-600'}`}>
                    {domain.risk_score}
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2.5">
                  <div 
                    className={`h-2.5 rounded-full ${domain.risk_score >= 70 ? 'bg-red-500' : 'bg-amber-500'}`}
                    style={{ width: `${domain.risk_score}%` }}
                  />
                </div>
              </div>

              <div className="space-y-3 text-sm">
                {domain.brand && (
                  <div className="flex items-center gap-3 text-gray-600">
                    <Shield className="h-4 w-4 shrink-0" />
                    <span className="font-medium text-gray-900 w-20">Brand:</span>
                    <span>{domain.brand}</span>
                  </div>
                )}
                {domain.registrar && (
                  <div className="flex items-center gap-3 text-gray-600">
                    <Globe className="h-4 w-4 shrink-0" />
                    <span className="font-medium text-gray-900 w-20">Registrar:</span>
                    <span>{domain.registrar}</span>
                  </div>
                )}
                {domain.ssl_issuer && (
                  <div className="flex items-center gap-3 text-gray-600">
                    <Lock className="h-4 w-4 shrink-0" />
                    <span className="font-medium text-gray-900 w-20">SSL:</span>
                    <span>{domain.ssl_issuer}</span>
                  </div>
                )}
                {domain.campaign && (
                  <div className="flex items-center gap-3 text-gray-600">
                    <Activity className="h-4 w-4 shrink-0" />
                    <span className="font-medium text-gray-900 w-20">Campaign:</span>
                    <span className="bg-purple-100 text-purple-700 px-2 py-0.5 rounded text-xs font-bold">{domain.campaign}</span>
                  </div>
                )}
                {domain.first_seen && (
                  <div className="flex items-center gap-3 text-gray-600">
                    <Clock className="h-4 w-4 shrink-0" />
                    <span className="font-medium text-gray-900 w-20">Seen:</span>
                    <span>{new Date(domain.first_seen).toLocaleDateString()}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Main Graph View */}
      <div className="flex-1 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden min-h-[600px] flex flex-col relative">
        <div className="p-4 border-b border-gray-100 bg-gray-50/50 flex items-center justify-between z-10">
          <h3 className="font-semibold text-gray-800 flex items-center gap-2">
            <LinkIcon className="h-4 w-4 text-blue-600" />
            Relationship Graph
          </h3>
          {data && (
            <div className="text-xs font-medium text-gray-500 bg-white px-3 py-1 rounded-full border border-gray-200 shadow-sm">
              {data.graph.nodes.length} Nodes · {data.graph.links.length} Links
            </div>
          )}
        </div>
        
        <div className="flex-1 relative bg-slate-50">
          {data ? (
            <ForceGraph2D
              ref={graphRef}
              graphData={data.graph}
              nodeCanvasObject={paintNode}
              nodePointerAreaPaint={nodePointerAreaPaint}
              linkColor={() => '#cbd5e1'}
              linkWidth={1.5}
              linkDirectionalArrowLength={3.5}
              linkDirectionalArrowRelPos={1}
              cooldownTicks={100}
              onEngineStop={() => graphRef.current?.zoomToFit(400, 50)}
              backgroundColor="#f8fafc"
            />
          ) : (
            <div className="absolute inset-0 flex flex-col items-center justify-center text-gray-400">
              <Network className="h-16 w-16 mb-4 opacity-20" />
              <p className="font-medium">Enter a search query to visualize relationships</p>
            </div>
          )}
        </div>
      </div>

    </div>
  )
}
