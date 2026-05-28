import Hero from '../sections/Hero'
import Problem from '../sections/Problem'
import Features from '../sections/Features'
import HowItWorks from '../sections/HowItWorks'
import LiveDemo from '../sections/LiveDemo'
import Stats from '../sections/Stats'
import Architecture from '../sections/Architecture'
import Comparison from '../sections/Comparison'
import SupportedApps from '../sections/SupportedApps'
import IndiaFocus from '../sections/IndiaFocus'
import Security from '../sections/Security'
import Roadmap from '../sections/Roadmap'
import FAQ from '../sections/FAQ'
import CTA from '../sections/CTA'

export default function Home() {
  return (
    <main>
      <Hero />
      <Problem />
      <Features />
      <HowItWorks />
      <LiveDemo />
      <Stats />
      <Architecture />
      <Comparison />
      <SupportedApps />
      <IndiaFocus />
      <Security />
      <Roadmap />
      <FAQ />
      <CTA />
    </main>
  )
}
