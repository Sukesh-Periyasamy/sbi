# AnteClick Website

Marketing and legal website for the AnteClick phishing protection app.

**Live URL:** Deploy to Vercel (see Deployment section below)

---

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| React | 19 | UI framework |
| Vite | 8 | Build tool & dev server |
| Tailwind CSS | 4 | Utility-first styling |
| Framer Motion | 12 | Animations & transitions |
| React Router | 7 | Client-side routing |
| Lucide React | 1.16 | Icon library |

---

## Pages

| Route | Page | Description |
|-------|------|-------------|
| `/` | Home | Landing page with all marketing sections |
| `/privacy-policy` | Privacy Policy | Full privacy policy (Play Store compliant) |
| `/terms-and-conditions` | Terms & Conditions | Full T&C document |

---

## Home Page Sections

### 1. Hero Section
- Headline: "Protect Your Banking Before You Click"
- Subtext explaining the product
- Two CTA buttons: "Get Protected" and "See How It Works"
- **Interactive phone demo** with two toggleable scenarios:
  - **Safe Link:** WhatsApp message from "Mom" with legitimate SBI link → browser opens → AnteClick scans → green "Verified Safe" toast → page loads normally
  - **Phishing Link:** WhatsApp message from unknown number with fake KYC scam → browser opens → AnteClick scans → signals detected one by one → full warning overlay with confidence bar, signal chips, and "Leave Website" button
- "Replay Demo" button to restart animation

### 2. Problem Section
- Statistics cards showing the scale of phishing in India:
  - ₹1,750 Cr+ lost to phishing (2023)
  - 5 Lakh+ fake banking apps detected yearly
  - 10,000+ new phishing URLs created daily
  - 82% of attacks target mobile users

### 3. Features Section (8 cards)
- Real-Time URL Monitoring
- AI Threat Scoring (16 heuristic signals)
- Instant Warnings (<300ms)
- Backend Verification
- Banking Domain Whitelist
- Smart Notifications
- Zero Data Collection
- Typo Domain Detection (Levenshtein)

### 4. How It Works (4 steps with hover phone previews)
Each step card shows a mini phone animation on hover:
- **Step 1:** WhatsApp chat → user taps phishing link → browser opens
- **Step 2:** URL scanned → 5 heuristic signals scored one by one → final score badge
- **Step 3:** Warning overlay slides down with signal chips, confidence bar, action buttons
- **Step 4:** Success screen → threat logged → dashboard with protection stats

### 5. Stats Section (count-up animation)
Numbers animate from 0 to final value when scrolled into view:
- 16 Heuristic Signals
- <300ms Detection Speed
- 0 bytes Data Collected
- 99% Detection Accuracy

### 6. Security Section (6 privacy principles)
- No Data Collection
- No Credential Storage
- HTTPS Only
- Local-First Analysis
- No Cloud Backups
- Open Architecture
- Trust banner: "No MITM · No TLS Decryption · No Packet Inspection · No Credential Access"

### 7. FAQ Section (8 questions, accordion)
Animated expand/collapse with questions covering:
- How detection works
- Password/data access
- Why Accessibility Service is needed
- Offline capability
- Supported browsers
- Performance impact
- Browsing history storage
- Android version support

### 8. CTA / Download Section
- "Start Protecting Your Banking Today"
- Google Play button + APK download button
- "Requires Android 12+. Free to use. No ads. No tracking."

---

## Components

| Component | File | Purpose |
|-----------|------|---------|
| Navbar | `src/components/Navbar.jsx` | Fixed top nav with logo, section links, mobile menu |
| Footer | `src/components/Footer.jsx` | Brand info, product links, legal links |
| ScrollToTop | `src/components/ScrollToTop.jsx` | Scrolls to top on route change |

---

## Design System

### Colors (Dark Navy Cybersecurity Theme)

| Token | Hex | Usage |
|-------|-----|-------|
| Navy (bg) | `#0A192F` | Page background |
| Navy Light | `#112240` | Cards, sections |
| Navy Lighter | `#1D3461` | Borders, accents |
| Blue | `#2563EB` | Primary buttons, CTAs |
| Cyan | `#38BDF8` | Highlights, accents, icons |
| Alert Red | `#EF4444` | Phishing warnings |
| Warning Amber | `#F59E0B` | Caution indicators |
| Success Green | `#10B981` | Safe states |
| Text | `#E2E8F0` | Primary text |
| Text Muted | `#94A3B8` | Secondary text |

### Visual Effects
- **Glass cards:** `backdrop-filter: blur(12px)` with subtle cyan border
- **Glow effects:** Blue and cyan box-shadows on CTAs and phone frame
- **Grid background:** Subtle cyan grid lines on hero section
- **Gradient orbs:** Blurred blue/cyan circles for depth

### Typography
- Font: Inter (system fallback)
- Headings: Bold, white
- Body: Regular, muted gray
- Mono: For URLs and code-like elements

### Animations (Framer Motion)
- Fade-in on scroll (`whileInView`)
- Staggered card reveals
- Phone demo phase transitions
- Count-up numbers on scroll
- FAQ accordion expand/collapse
- Hover effects on cards and buttons

---

## File Structure

```
web/
├── public/
│   └── shield.svg              # Favicon
├── src/
│   ├── components/
│   │   ├── Navbar.jsx          # Navigation bar
│   │   ├── Footer.jsx          # Page footer
│   │   └── ScrollToTop.jsx     # Route change scroll handler
│   ├── pages/
│   │   ├── Home.jsx            # Landing page (assembles sections)
│   │   ├── PrivacyPolicy.jsx   # Privacy policy content
│   │   └── TermsOfService.jsx  # Terms & conditions content
│   ├── sections/
│   │   ├── Hero.jsx            # Hero + interactive phone demo
│   │   ├── Problem.jsx         # Statistics cards
│   │   ├── Features.jsx        # 8 feature cards
│   │   ├── HowItWorks.jsx      # 4 steps + hover phone previews
│   │   ├── Stats.jsx           # Count-up metrics
│   │   ├── Security.jsx        # Privacy principles
│   │   ├── FAQ.jsx             # Accordion questions
│   │   └── CTA.jsx             # Download section
│   ├── App.jsx                 # Router + layout
│   ├── main.jsx                # Entry point
│   └── index.css               # Tailwind + custom styles
├── index.html                  # HTML template
├── vite.config.js              # Vite + React + Tailwind config
├── vercel.json                 # SPA routing for Vercel
├── package.json                # Dependencies & scripts
└── .gitignore                  # node_modules, dist
```

---

## Development

```bash
# Install dependencies
npm install

# Start dev server (http://localhost:5173)
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

---

## Deployment (Vercel)

### Option 1: CLI
```bash
npx vercel
```

### Option 2: Git Integration
1. Push to GitHub
2. Connect repo in Vercel dashboard
3. Set root directory to `web`
4. Framework: Vite
5. Build command: `npm run build`
6. Output directory: `dist`

The `vercel.json` handles SPA routing — all paths serve `index.html`.

---

## Responsive Design

- **Desktop:** Two-column hero (text + phone), 4-column feature grid
- **Tablet:** Two-column feature grid, stacked hero
- **Mobile:** Single column, collapsible nav menu, touch-friendly buttons

---

## Performance

- Vite tree-shaking removes unused code
- Tailwind CSS purges unused styles
- Framer Motion animations use `whileInView` with `once: true` (no re-animation)
- No external fonts loaded (uses system Inter)
- Total bundle: ~134 KB gzipped (JS + CSS)
