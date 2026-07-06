# AnteClick v1.0 Known Limitations

The following limitations are documented for the initial Play Store release (v1.0) of AnteClick:

- **Browser Protection Only:** Protects only supported web browsers (Chrome, Firefox, Brave, Samsung Internet, Edge) and standard in-app WebViews (e.g., WhatsApp, Telegram, Instagram). Unorthodox browsers or custom views that do not expose standard URL address bars are not supported.
- **Post-Open Detection:** Detects and warns about phishing websites *after* they are opened in a browser. It does not block the initial click or preemptively prevent the connection.
- **No SMS Scanning:** Does not parse, read, or scan SMS message content directly.
- **No Notification Scanning:** Does not scan incoming push notifications.
- **No Absolute Guarantee:** Does not guarantee 100% detection of all phishing websites. Advanced zero-day or targeted phishing links may not match local heuristics or backend signature databases.
- **Accessibility Service Required:** Relies completely on the Android Accessibility Service permission. If the user disables this permission, real-time protection is deactivated.
- **Internet Dependency:** Offline fallback mode handles local heuristics scoring, but full backend verification and intelligence updates require an active internet connection.
