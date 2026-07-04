import { useState } from 'react'
import { ComposableMap, Geographies, Geography, Marker } from 'react-simple-maps'
import { motion } from 'framer-motion'
import { Globe, MapPin, Activity } from 'lucide-react'

const geoUrl = "https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json"

const markers = [
  { name: "Maharashtra", coordinates: [75.7139, 19.7515], count: 380, offset: [0, -15] },
  { name: "Delhi", coordinates: [77.1025, 28.7041], count: 245, offset: [0, -15] },
  { name: "Tamil Nadu", coordinates: [78.6569, 11.1271], count: 189, offset: [0, -15] },
  { name: "Karnataka", coordinates: [75.7139, 15.3173], count: 156, offset: [15, 0] },
  { name: "West Bengal", coordinates: [87.8550, 22.9868], count: 134, offset: [0, -15] }
]

export default function ThreatMap() {
  const [view, setView] = useState('world')

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Geographic Threat Map</h2>
          <p className="text-gray-500">Live detection origin and hosting heatmap</p>
        </div>
        <div className="flex bg-white rounded-lg shadow-sm p-1 border border-gray-200">
          <button 
            onClick={() => setView('world')}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${view === 'world' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}
          >
            Global Hosting
          </button>
          <button 
            onClick={() => setView('india')}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${view === 'india' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}
          >
            India Targets
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        
        {/* Map Visualization Area */}
        <div className="lg:col-span-3 bg-white p-6 rounded-xl shadow-sm border border-gray-200 overflow-hidden relative min-h-[500px] flex items-center justify-center bg-slate-50">
          
          <ComposableMap
            projection="geoMercator"
            projectionConfig={{
              scale: view === 'india' ? 1000 : 140,
              center: view === 'india' ? [80, 22] : [0, 20]
            }}
            width={800}
            height={500}
            style={{ width: "100%", height: "auto" }}
          >
            <Geographies geography={geoUrl}>
              {({ geographies }) =>
                geographies.map((geo) => {
                  const isIndia = geo.properties.name === "India"
                  const fill = isIndia ? (view === 'india' ? '#e2e8f0' : '#f87171') : '#cbd5e1'
                  
                  return (
                    <Geography
                      key={geo.rsmKey}
                      geography={geo}
                      fill={fill}
                      stroke="#f8fafc"
                      strokeWidth={0.5}
                      style={{
                        default: { outline: 'none' },
                        hover: { fill: '#94a3b8', outline: 'none' },
                        pressed: { outline: 'none' }
                      }}
                    />
                  )
                })
              }
            </Geographies>
            
            {view === 'india' && markers.map(({ name, coordinates }) => (
              <Marker key={name} coordinates={coordinates}>
                <motion.circle 
                  r={6} 
                  fill="#ef4444" 
                  initial={{ scale: 0 }}
                  animate={{ scale: [1, 1.5, 1] }}
                  transition={{ repeat: Infinity, duration: 2 }}
                />
                <circle r={3} fill="#fff" />
              </Marker>
            ))}
          </ComposableMap>
        </div>

        {/* Top Regions List */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <h3 className="font-semibold text-gray-800 mb-6 flex items-center gap-2">
            <Activity className="h-5 w-5 text-blue-600" />
            Top Regions
          </h3>
          
          <div className="space-y-6">
            {markers.sort((a,b) => b.count - a.count).map((marker, i) => (
              <div key={marker.name} className="flex items-center gap-4">
                <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 font-bold flex items-center justify-center shrink-0">
                  {i + 1}
                </div>
                <div className="flex-1">
                  <div className="flex justify-between mb-1">
                    <span className="text-sm font-medium text-gray-700">{marker.name}</span>
                    <span className="text-sm font-bold text-gray-900">{marker.count}</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div 
                      className="bg-blue-600 h-2 rounded-full" 
                      style={{ width: `${(marker.count / 380) * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
