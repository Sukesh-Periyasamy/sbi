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
              This Privacy Policy explains how AnteClick collects, uses, processes, and protects information when you use the AnteClick mobile application and related services ("Service").
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
                <li>browser and WebView URL analysis,</li>
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
              <p>The following information is processed locally on your device for phishing detection:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>URL text displayed in browser address bars and WebView navigation contexts,</li>
                <li>domain names extracted from those URLs,</li>
                <li>package names of newly installed apps (for banking app authenticity verification),</li>
                <li>installer source of newly installed apps (Play Store vs other sources),</li>
                <li>signing certificate hashes of newly installed apps (to verify official banking apps).</li>
              </ul>
              <p className="mt-2">
                This information is analyzed locally on your device. It is not continuously uploaded to our servers.
              </p>

              <h3 className="text-lg font-medium text-white mt-5 mb-2">B. Information Sent to Backend Services</h3>
              <p>
                Only domain names or package identifiers required for threat verification may be transmitted to our backend services. Full webpage content, passwords, messages, and personal information are never transmitted.
              </p>
              <p className="mt-2">Specifically, the following may be transmitted:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>suspicious domain names (for URL phishing verification),</li>
                <li>suspicious package names (for banking app verification),</li>
                <li>package signing certificate hashes (for official app verification),</li>
                <li>app version and Android OS version (for compatibility).</li>
              </ul>
              <p className="mt-2">
                SAFE URLs and verified official banking apps are processed locally and are not transmitted.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">3. Information We DO NOT Collect</h2>
              <p>AnteClick does NOT collect:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>passwords or authentication credentials,</li>
                <li>OTPs or verification codes,</li>
                <li>banking account information,</li>
                <li>chat messages or personal communications,</li>
                <li>emails or contact information,</li>
                <li>contact lists or address books,</li>
                <li>photos, audio recordings, or media files,</li>
                <li>keyboard input or keystrokes,</li>
                <li>precise or approximate location data,</li>
                <li>browsing history or navigation logs,</li>
                <li>webpage content or form data,</li>
                <li>device identifiers (IMEI, advertising ID),</li>
                <li>personal names, email addresses, or phone numbers.</li>
              </ul>
              <p className="mt-3 font-medium text-white">AnteClick does NOT sell personal information.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">4. Accessibility Service Usage</h2>
              <p>
                AnteClick uses Android Accessibility APIs solely for phishing protection purposes.
              </p>
              <p className="mt-3"><strong className="text-white">The Accessibility Service reads only:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>URL text displayed in browser address bars,</li>
                <li>URL text in WebView navigation contexts within supported messaging apps.</li>
              </ul>
              <p className="mt-3"><strong className="text-white">The Accessibility Service does NOT access:</strong></p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>webpage content or HTML,</li>
                <li>form fields or user-entered text,</li>
                <li>passwords or OTPs,</li>
                <li>chat messages or emails,</li>
                <li>keyboard input or keystrokes,</li>
                <li>any content outside browser address bars and WebView URL contexts,</li>
                <li>any data from non-browser applications.</li>
              </ul>
              <p className="mt-3">
                A prominent in-app disclosure screen explains the service's purpose before the user enables it. Users may disable the Accessibility Service at any time through Android Settings.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">5. Banking App Authenticity Verification</h2>
              <p>
                AnteClick uses the Android PACKAGE_ADDED system broadcast to verify newly installed apps that resemble banking applications. This is event-driven and activates only when a new app is installed.
              </p>
              <p className="mt-3">When a new app is installed, AnteClick may analyze:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>the package name of the newly installed app,</li>
                <li>the installer source (Play Store or other),</li>
                <li>the signing certificate hash (to verify official banking apps).</li>
              </ul>
              <p className="mt-3">
                AnteClick does NOT scan all installed apps. It only analyzes the single package identified in the PACKAGE_ADDED broadcast, and only if that package name contains banking-related keywords.
              </p>
              <p className="mt-3">
                AnteClick never automatically removes or blocks any app. It only warns users and provides an uninstall shortcut.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">6. How We Use Information</h2>
              <p>Information processed by AnteClick is used only to:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>detect phishing websites in real-time,</li>
                <li>verify suspicious domains against threat intelligence feeds,</li>
                <li>verify banking app authenticity,</li>
                <li>display contextual security warnings,</li>
                <li>maintain a local threat detection history for the user's reference.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">7. Threat Intelligence Sources</h2>
              <p>AnteClick's backend uses third-party phishing intelligence feeds to improve detection accuracy:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>OpenPhish (https://openphish.com),</li>
                <li>URLhaus (https://urlhaus.abuse.ch),</li>
                <li>PhishTank (https://phishtank.org).</li>
              </ul>
              <p className="mt-2">
                These feeds provide lists of known phishing domains. Domain names queried against these feeds are not stored or shared with the feed providers.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">8. Data Storage and Security</h2>
              <p>AnteClick uses the following security measures:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>all communication with our backend is encrypted using HTTPS (TLS),</li>
                <li>a network security configuration explicitly blocks cleartext (HTTP) traffic,</li>
                <li>API key authentication for all backend requests,</li>
                <li>app data is excluded from Android cloud backups (android:allowBackup="false"),</li>
                <li>local threat history is stored in SharedPreferences (max 100 entries, device-only).</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">9. Data Retention</h2>
              <ul className="list-disc pl-6 space-y-1">
                <li><strong className="text-white">Local threat history:</strong> Stored on your device until you clear it or uninstall the app. Limited to 100 entries.</li>
                <li><strong className="text-white">Backend cache:</strong> Domain reputation results are cached for 10 minutes, then automatically deleted. No user identifiers are associated with cached results.</li>
                <li><strong className="text-white">Server logs:</strong> Operational logs contain only domain names and timestamps. No IP addresses or user identifiers are logged. Logs are retained for 7 days, then permanently deleted.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">10. Third-Party Services</h2>
              <p>AnteClick uses the following third-party infrastructure providers:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>Render (backend hosting),</li>
                <li>Redis (caching),</li>
                <li>Google Play Services (app distribution).</li>
              </ul>
              <p className="mt-2">
                These providers may process limited technical information necessary to operate the Service. AnteClick is not responsible for the privacy practices of third-party providers.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">11. Children's Privacy</h2>
              <p>
                AnteClick is not intended for children under the age of 13. We do not knowingly collect personal information from children. Since AnteClick does not collect personal information from any user, this concern is inherently addressed by our privacy-first architecture.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">12. Your Choices</h2>
              <p>You may:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>disable the Accessibility Service at any time from Android Settings,</li>
                <li>clear your local threat history from within the app,</li>
                <li>uninstall AnteClick to remove all local data.</li>
              </ul>
              <p className="mt-2">Disabling the Accessibility Service will stop URL phishing detection.</p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-white mb-3">13. Changes to This Privacy Policy</h2>
              <p>
                AnteClick may update this Privacy Policy periodically. Updated versions will be posted within the application or on the official website. Continued use of the Service after updates constitutes acceptance of the revised Privacy Policy.
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
