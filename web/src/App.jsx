import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Analytics } from '@vercel/analytics/react'
import { SpeedInsights } from '@vercel/speed-insights/react'
import ScrollToTop from './components/ScrollToTop'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home from './pages/Home'
import SDK from './pages/SDK'
import PrivacyPolicy from './pages/PrivacyPolicy'
import TermsOfService from './pages/TermsOfService'

// New Advanced Features
import AdminLayout from './components/AdminLayout'
import DashboardOverview from './pages/DashboardOverview'
import ThreatMap from './pages/ThreatMap'
import ThreatFeeds from './pages/ThreatFeeds'
import BrandIntel from './pages/BrandIntel'
import SystemHealth from './pages/SystemHealth'
import ThreatExplorer from './pages/ThreatExplorer'
import Campaigns from './pages/Campaigns'

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <div className="min-h-screen bg-white text-text font-sans">
        <Routes>
          {/* Public Routes with Navbar/Footer */}
          <Route path="/" element={<><Navbar /><Home /><Footer /></>} />
          <Route path="/sdk" element={<><Navbar /><SDK /><Footer /></>} />
          <Route path="/privacy-policy" element={<><Navbar /><PrivacyPolicy /><Footer /></>} />
          <Route path="/terms-and-conditions" element={<><Navbar /><TermsOfService /><Footer /></>} />
          
          {/* Public Intel Portal */}
          <Route path="/intel" element={
            <div className="h-screen flex flex-col">
              <Navbar />
              <div className="flex-1 bg-slate-50 p-6 overflow-hidden">
                <ThreatExplorer />
              </div>
            </div>
          } />

          {/* Secured Admin Dashboard */}
          <Route path="/dashboard" element={<AdminLayout />}>
            <Route index element={<DashboardOverview />} />
            <Route path="map" element={<ThreatMap />} />
            <Route path="feed" element={<DashboardOverview />} />
            <Route path="campaigns" element={<Campaigns />} />
            <Route path="brands" element={<BrandIntel />} />
            <Route path="feeds" element={<ThreatFeeds />} />
            <Route path="health" element={<SystemHealth />} />
            <Route path="scheduler" element={<div className="p-8">Scheduler jobs module...</div>} />
            <Route path="pipeline" element={<div className="p-8">Pipeline module...</div>} />
          </Route>
          
          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </div>
      <Analytics />
      <SpeedInsights />
    </BrowserRouter>
  )
}
