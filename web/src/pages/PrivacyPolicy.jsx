import { motion } from 'framer-motion'
import { Shield } from 'lucide-react'

export default function PrivacyPolicy() {
  return (
    <main className="pt-24 pb-20 bg-navy min-h-screen">
      <div className="mx-auto max-w-3xl px-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
        >
          <div className="flex items-center gap-3 mb-8">
            <Shield className="w-8 h-8 text-cyan" />
            <h1 className="text-3xl md:text-4xl font-bold text-white">Privacy Policy</h1>
          </div>

          <div className="text-text-muted text-sm mb-8">
            <p>Last Updated: January 2026</p>
          </div>

          <div className="prose prose-invert max-w-none space-y-8 text-text-muted leading-relaxed text-[15px]">

            <p>
              AnteClick ("we", "our", or "us") values your privacy and is committed to protecting your information.
            </p>
            <p>
              This Privacy Policy explains how AnteClick collects, uses, processes, and protects information when you use the AnteClick mobile application, website, and related services ("Service").
            </p>
            <p>
              By using AnteClick, you agree to the practices described in this Privacy Policy.
            </p>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">1. About AnteClick</h2>
              <p>
                AnteClick is a lightweight Android financial phishing protection application designed to help users detect potentially dangerous banking phishing websites before interacting with them.
              </p>
              <p className="mt-3">AnteClick uses:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>browser/WebView URL analysis,</li>
                <li>phishing intelligence feeds,</li>
                <li>heuristic risk scoring,</li>
                <li>optional cloud verification</li>
              </ul>
              <p className="mt-2">to provide phishing warnings and fraud prevention assistance.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">2. Information We Collect</h2>
              <p>
                AnteClick is designed with privacy-first principles and minimal data collection.
              </p>

              <h3 className="text-lg font-medium text-white mt-5 mb-2">A. Information Processed Locally on Device</h3>
              <p>The following information may be processed locally on your device for phishing detection:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>browser URLs,</li>
                <li>domain names,</li>
                <li>banking-related keywords,</li>
                <li>WebView navigation events,</li>
                <li>browser application source,</li>
                <li>newly installed package names (via PACKAGE_ADDED broadcast only),</li>
                <li>package installer source (Play Store vs sideloaded),</li>
                <li>package signing certificate hashes (for official app verification).</li>
              </ul>
              <p className="mt-2">This information is primarily analyzed locally and is not continuously uploaded to our servers.</p>

              <h3 className="text-lg font-medium text-white mt-5 mb-2">B. Information Sent to Backend Services</h3>
              <p>Only suspicious or high-risk URLs/packages may be sent to AnteClick backend services for additional verification.</p>
              <p className="mt-2">Examples of data that may be transmitted:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>suspicious domain names,</li>
                <li>suspicious package names,</li>
                <li>URL hashes,</li>
                <li>package signing certificate hashes,</li>
                <li>phishing risk scores,</li>
                <li>browser source,</li>
                <li>app version,</li>
                <li>Android version,</li>
                <li>anonymized technical metadata.</li>
              </ul>
              <p className="mt-2">SAFE URLs and verified official banking apps are processed locally and are not transmitted.</p>

              <h3 className="text-lg font-medium text-white mt-5 mb-2">C. Device Information</h3>
              <p>AnteClick may collect limited technical device information, including:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>device model,</li>
                <li>Android OS version,</li>
                <li>application version,</li>
                <li>crash diagnostics,</li>
                <li>performance metrics.</li>
              </ul>
              <p className="mt-2">This information helps improve app stability and compatibility.</p>

              <h3 className="text-lg font-medium text-white mt-5 mb-2">D. Approximate Location Information</h3>
              <p>AnteClick may process approximate regional information such as:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>country,</li>
                <li>state,</li>
                <li>city (coarse location only).</li>
              </ul>
              <p className="mt-2">We do NOT collect precise GPS coordinates.</p>
              <p className="mt-2">This information may be used for:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>regional phishing analytics,</li>
                <li>attack trend analysis,</li>
                <li>cybersecurity research.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">3. Information We DO NOT Collect</h2>
              <p>AnteClick does NOT intentionally collect:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>passwords,</li>
                <li>OTPs,</li>
                <li>banking credentials,</li>
                <li>chat messages,</li>
                <li>emails,</li>
                <li>contact lists,</li>
                <li>photos,</li>
                <li>audio recordings,</li>
                <li>keyboard input,</li>
                <li>precise GPS location,</li>
                <li>browsing history logs,</li>
                <li>financial account details.</li>
              </ul>
              <p className="mt-3 font-medium text-white">AnteClick does NOT sell personal information.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">4. Accessibility Service Usage</h2>
              <p>
                AnteClick uses Android Accessibility APIs solely for phishing protection purposes.
              </p>
              <p className="mt-3"><strong className="text-white">The Accessibility Service is used only to:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>detect browser and WebView URL navigation events,</li>
                <li>identify suspicious phishing websites,</li>
                <li>display contextual phishing warnings.</li>
              </ul>
              <p className="mt-3"><strong className="text-white">AnteClick does NOT use Accessibility APIs to:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>monitor personal conversations,</li>
                <li>capture passwords,</li>
                <li>read OTPs,</li>
                <li>record keystrokes,</li>
                <li>inspect unrelated applications,</li>
                <li>collect personal content.</li>
              </ul>
              <p className="mt-3">
                Users may disable Accessibility access at any time through Android Settings.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">5. How We Use Information</h2>
              <p>Information processed by AnteClick may be used to:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>detect phishing websites,</li>
                <li>improve fraud detection accuracy,</li>
                <li>maintain phishing intelligence databases,</li>
                <li>analyze phishing trends,</li>
                <li>improve application stability,</li>
                <li>enhance security research,</li>
                <li>prevent abuse of the Service.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">6. Threat Intelligence Sources</h2>
              <p>AnteClick may use third-party phishing intelligence providers, including:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>OpenPhish,</li>
                <li>PhishTank,</li>
                <li>Phishing.Database,</li>
                <li>URLhaus,</li>
                <li>other cybersecurity intelligence feeds.</li>
              </ul>
              <p className="mt-2">These services may contribute phishing reputation data used during detection.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">7. Data Storage and Security</h2>
              <p>
                AnteClick uses reasonable technical and organizational measures to protect information.
              </p>
              <p className="mt-3">Security measures may include:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>encrypted HTTPS communication,</li>
                <li>secure cloud infrastructure,</li>
                <li>access controls,</li>
                <li>caching security,</li>
                <li>limited data retention.</li>
              </ul>
              <p className="mt-3">However, no internet-based system can be guaranteed 100% secure.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">8. Data Retention</h2>
              <p>AnteClick retains limited technical and phishing-related data only as long as necessary for:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>security analysis,</li>
                <li>system improvement,</li>
                <li>fraud prevention,</li>
                <li>operational reliability.</li>
              </ul>
              <p className="mt-2">Temporary cache entries may automatically expire after short periods.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">9. Third-Party Services</h2>
              <p>AnteClick may use third-party infrastructure providers, including:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>Render,</li>
                <li>Upstash Redis,</li>
                <li>Supabase,</li>
                <li>Google Play Services.</li>
              </ul>
              <p className="mt-2">
                These providers may process limited technical information necessary to operate the Service.
              </p>
              <p className="mt-2">
                AnteClick is not responsible for the privacy practices of third-party providers.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">10. Children's Privacy</h2>
              <p>
                AnteClick is not intended for children under the age of 13.
              </p>
              <p className="mt-2">
                We do not knowingly collect personal information from children.
              </p>
              <p className="mt-2">
                If you believe a child has provided personal information, please contact us so we can remove it.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">11. Your Choices</h2>
              <p>You may:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>disable Accessibility permissions,</li>
                <li>disable notifications,</li>
                <li>stop using the Service,</li>
                <li>uninstall AnteClick at any time.</li>
              </ul>
              <p className="mt-2">Disabling certain permissions may reduce phishing protection functionality.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">12. International Use</h2>
              <p>
                AnteClick services may operate on cloud infrastructure located in different regions.
              </p>
              <p className="mt-2">
                By using the Service, you consent to processing of limited technical data in applicable hosting regions.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">13. Changes to This Privacy Policy</h2>
              <p>
                AnteClick may update this Privacy Policy periodically.
              </p>
              <p className="mt-2">
                Updated versions will be posted within the application or on the official website.
              </p>
              <p className="mt-2">
                Continued use of the Service after updates constitutes acceptance of the revised Privacy Policy.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">14. Contact Us</h2>
              <p>If you have questions regarding this Privacy Policy, please contact:</p>
              <div className="mt-3 p-4 glass-card rounded-xl">
                <p><strong className="text-white">Email:</strong> support@anteclick.app</p>
                <p className="mt-1"><strong className="text-white">Website:</strong> https://anteclick.app</p>
              </div>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">15. Consent</h2>
              <p>By installing or using AnteClick, you acknowledge that:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>you have read this Privacy Policy,</li>
                <li>you understand this Privacy Policy,</li>
                <li>you consent to the practices described herein.</li>
              </ul>
            </section>

          </div>
        </motion.div>
      </div>
    </main>
  )
}
