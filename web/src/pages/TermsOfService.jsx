import { motion } from 'framer-motion'
import { Shield } from 'lucide-react'

export default function TermsOfService() {
  return (
    <main className="pt-24 pb-20 bg-white min-h-screen">
      <div className="mx-auto max-w-3xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
        >
          <div className="flex items-center gap-3 mb-8">
            <Shield className="w-8 h-8 text-cyan" />
            <h1 className="text-3xl md:text-4xl font-bold text-text">Terms & Conditions</h1>
          </div>

          <div className="text-text-muted text-sm mb-8">
            <p>Last Updated: January 2026</p>
          </div>

          <div className="prose prose-invert max-w-none space-y-8 text-text-muted leading-relaxed text-[15px]">

            <p>
              Welcome to AnteClick.
            </p>
            <p>
              These Terms & Conditions ("Terms") govern your use of the AnteClick mobile application, website, services, and related cybersecurity protection features ("Service"). By installing or using AnteClick, you agree to these Terms.
            </p>
            <p>
              If you do not agree with these Terms, please do not use the Service.
            </p>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">1. About AnteClick</h2>
              <p>
                AnteClick is a lightweight Android financial phishing protection application designed to help users identify suspicious banking and phishing websites before interacting with them.
              </p>
              <p className="mt-3">
                AnteClick is intended to provide security warnings and risk indicators based on phishing intelligence feeds, heuristic analysis, browser/WebView URL detection, and banking app authenticity verification.
              </p>
              <p className="mt-3"><strong className="text-text">AnteClick is NOT:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>a banking institution,</li>
                <li>an antivirus product,</li>
                <li>a malware removal tool,</li>
                <li>a VPN service,</li>
                <li>a guaranteed fraud prevention solution.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">2. Eligibility</h2>
              <p>
                You must be at least 18 years old or have permission from a parent or legal guardian to use AnteClick.
              </p>
              <p className="mt-3">
                You are responsible for ensuring your use of the Service complies with applicable local laws and regulations.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">3. Acceptable Use</h2>
              <p>You agree NOT to:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>misuse the Service,</li>
                <li>reverse engineer the application,</li>
                <li>attempt to interfere with security mechanisms,</li>
                <li>use AnteClick for unlawful activities,</li>
                <li>abuse backend APIs or automated systems,</li>
                <li>attempt to bypass warnings or protections for malicious purposes.</li>
              </ul>
              <p className="mt-3">
                You also agree not to use AnteClick in ways that may harm other users, networks, or systems.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">4. Accessibility Service Usage</h2>
              <p>AnteClick uses Android Accessibility APIs solely for phishing protection purposes.</p>
              <p className="mt-3">AnteClick also uses the PACKAGE_ADDED system broadcast to verify newly installed banking-related apps for authenticity. This is event-driven and does not scan all installed apps.</p>
              <p className="mt-3"><strong className="text-text">The Accessibility Service is used only to:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>detect browser and WebView URL navigation events,</li>
                <li>identify potentially dangerous phishing websites,</li>
                <li>display contextual security warnings.</li>
              </ul>
              <p className="mt-3"><strong className="text-text">AnteClick does NOT:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>capture passwords,</li>
                <li>read OTPs,</li>
                <li>monitor personal messages,</li>
                <li>record keystrokes,</li>
                <li>collect banking credentials,</li>
                <li>store browsing history.</li>
              </ul>
              <p className="mt-3">
                Users may disable the Accessibility Service at any time through Android Settings.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">4A. Banking App Authenticity Verification</h2>
              <p>
                AnteClick uses the Android PACKAGE_ADDED system broadcast to verify newly installed apps that resemble banking applications.
              </p>
              <p className="mt-3"><strong className="text-text">This feature:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>only activates when a new app is installed (event-driven),</li>
                <li>only analyzes apps containing banking-related keywords,</li>
                <li>verifies package signatures against known official banking apps,</li>
                <li>warns users about potentially fake banking apps,</li>
                <li>never automatically removes or blocks any app installation.</li>
              </ul>
              <p className="mt-3"><strong className="text-text">This feature does NOT:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>scan all installed apps,</li>
                <li>run continuous background monitoring,</li>
                <li>use the QUERY_ALL_PACKAGES permission,</li>
                <li>function as antivirus or malware scanning software.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">5. Security Warnings</h2>
              <p>
                AnteClick provides heuristic and intelligence-based phishing risk assessments. Security warnings are informational and preventive in nature.
              </p>
              <p className="mt-3"><strong className="text-text">AnteClick does not guarantee:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>detection of all phishing websites,</li>
                <li>prevention of all fraud,</li>
                <li>uninterrupted availability,</li>
                <li>complete accuracy of all threat intelligence feeds.</li>
              </ul>
              <p className="mt-3"><strong className="text-text">Users remain responsible for:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>verifying websites,</li>
                <li>protecting their credentials,</li>
                <li>reviewing banking transactions,</li>
                <li>practicing safe online behavior.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">6. Data Collection</h2>
              <p>
                AnteClick is designed with privacy-focused principles.
              </p>
              <p className="mt-3">
                The Service may process limited technical metadata necessary for phishing detection and service improvement, including:
              </p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>domain names,</li>
                <li>phishing risk scores,</li>
                <li>browser source,</li>
                <li>Android version,</li>
                <li>anonymized device information,</li>
                <li>approximate region or country.</li>
              </ul>
              <p className="mt-3"><strong className="text-text">AnteClick does NOT intentionally collect:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>passwords,</li>
                <li>OTPs,</li>
                <li>financial account information,</li>
                <li>chat messages,</li>
                <li>personal files,</li>
                <li>contact lists,</li>
                <li>precise GPS location.</li>
              </ul>
              <p className="mt-3">
                Additional details are available in the <a href="/privacy-policy" className="text-cyan hover:underline">Privacy Policy</a>.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">7. Backend Services</h2>
              <p>AnteClick may use cloud-hosted backend services to:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>verify suspicious domains,</li>
                <li>retrieve phishing intelligence,</li>
                <li>improve detection accuracy,</li>
                <li>maintain updated threat intelligence feeds.</li>
              </ul>
              <p className="mt-3">Backend services may occasionally be unavailable due to:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>maintenance,</li>
                <li>internet connectivity,</li>
                <li>hosting provider limitations,</li>
                <li>third-party infrastructure outages.</li>
              </ul>
              <p className="mt-3">
                AnteClick may continue operating with limited local protection during such periods.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">8. Third-Party Services</h2>
              <p>
                AnteClick may integrate or rely on third-party infrastructure and threat intelligence providers, including:
              </p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>OpenPhish,</li>
                <li>PhishTank,</li>
                <li>Render,</li>
                <li>Upstash Redis,</li>
                <li>Supabase,</li>
                <li>Google Play Services.</li>
              </ul>
              <p className="mt-3">
                AnteClick is not responsible for the availability, accuracy, or reliability of third-party services.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">9. Intellectual Property</h2>
              <p>
                All AnteClick branding, software, source code, logos, designs, and related materials are protected by applicable intellectual property laws.
              </p>
              <p className="mt-3">You may not:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>copy,</li>
                <li>redistribute,</li>
                <li>modify,</li>
                <li>resell,</li>
                <li>commercially exploit</li>
              </ul>
              <p className="mt-1">any part of AnteClick without written permission.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">10. Limitation of Liability</h2>
              <p>
                AnteClick is provided on an "AS IS" and "AS AVAILABLE" basis.
              </p>
              <p className="mt-3">
                To the maximum extent permitted by law, AnteClick and its developers shall not be liable for:
              </p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>financial losses,</li>
                <li>banking fraud,</li>
                <li>phishing damages,</li>
                <li>indirect damages,</li>
                <li>lost profits,</li>
                <li>data loss,</li>
                <li>service interruptions,</li>
                <li>security incidents beyond reasonable control.</li>
              </ul>
              <p className="mt-3">
                Users acknowledge that cybersecurity protection tools reduce risk but cannot eliminate all threats.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">11. No Financial or Legal Advice</h2>
              <p>AnteClick does not provide:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>financial advice,</li>
                <li>banking advice,</li>
                <li>legal advice,</li>
                <li>cybersecurity consulting services.</li>
              </ul>
              <p className="mt-3">Warnings and scores are automated risk indicators only.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">12. Service Modifications</h2>
              <p>AnteClick may:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>update features,</li>
                <li>modify detection logic,</li>
                <li>change backend infrastructure,</li>
                <li>discontinue parts of the Service,</li>
                <li>update these Terms</li>
              </ul>
              <p className="mt-1">at any time without prior notice.</p>
              <p className="mt-3">
                Continued use of the Service after updates constitutes acceptance of revised Terms.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">13. Termination</h2>
              <p>AnteClick reserves the right to suspend or terminate access to the Service if:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>misuse is detected,</li>
                <li>abuse of APIs occurs,</li>
                <li>malicious activity is identified,</li>
                <li>these Terms are violated.</li>
              </ul>
              <p className="mt-3">
                Users may stop using the Service at any time by uninstalling the application.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">14. Privacy</h2>
              <p>
                Your use of AnteClick is also governed by the <a href="/privacy-policy" className="text-cyan hover:underline">AnteClick Privacy Policy</a>.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">15. Governing Law</h2>
              <p>
                These Terms shall be governed by and interpreted in accordance with the laws of India.
              </p>
              <p className="mt-3">
                Any disputes arising from these Terms shall be subject to the jurisdiction of courts located in India.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">16. Contact</h2>
              <p>For questions, feedback, or support regarding AnteClick:</p>
              <div className="mt-3 p-4 glass-card rounded-xl">
                <p><strong className="text-text">Email:</strong> support@anteclick.app</p>
                <p className="mt-1"><strong className="text-text">Website:</strong> https://anteclick.app</p>
              </div>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-text mb-3">17. Acknowledgement</h2>
              <p>By installing or using AnteClick, you acknowledge that:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>you have read these Terms,</li>
                <li>you understand these Terms,</li>
                <li>you agree to be bound by these Terms.</li>
              </ul>
            </section>

          </div>
        </motion.div>
      </div>
    </main>
  )
}
