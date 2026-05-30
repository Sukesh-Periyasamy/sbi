// Test link datasets
const sets = {
    set1: {
        type: 'high',
        links: [
            'secure-sbi-login.xyz', 'sbi-verify-account.top', 'sbi-kyc-update.click',
            'sbi-reward-center.shop', 'onlinesbi-secure-login.xyz',
            'hdfc-kyc-update.top', 'hdfcbank-secure-login.xyz', 'hdfc-reward-program.click',
            'hdfc-customer-verify.shop', 'hdfcbannk.xyz',
            'icici-secure-login.top', 'icici-verify-account.xyz', 'icici-banking-update.click',
            'icicibank-login.shop', 'icicii-bank.xyz',
            'axis-kyc-update.top', 'axisbank-secure-login.xyz', 'axis-bank-verification.click',
            'axis-reward-center.shop', 'axissbank.xyz'
        ]
    },
    set2: {
        type: 'high',
        links: [
            'paytm-secure-login.xyz', 'paytm-kyc-update.top', 'paytm-reward-claim.click',
            'phonepe-verification.xyz', 'phonepe-secure-login.top', 'phonepe-kyc-update.click',
            'gpay-secure-login.xyz', 'googlepay-verification.top',
            'upi-reward-claim.click', 'upi-secure-update.shop'
        ]
    },
    set3: {
        type: 'warn',
        links: [
            'sbii.com', 'paytmm.com', 'hdfcbannk.com', 'icicii-bank.com', 'axissbank.com',
            'phonpe.com', 'goooglepay.com', 'onlinesbii.com', 'hdfcsecuree.com', 'paytm-secure.com'
        ]
    },
    set4: {
        type: 'high',
        links: [
            'paypa1-login.xyz', 'g00glepay-secure.top', 'sb1-login.xyz',
            'hdfc-secure-l0gin.click', 'icici-verificat10n.shop'
        ]
    },
    set5: {
        type: 'warn',
        links: [
            'xjskq-login-verification.xyz', 'banking-secure-update-887.top',
            'verify-account-urgent-992.click', 'customer-kyc-check.shop',
            'secure-update-wallet-login.live'
        ]
    },
    safe: {
        type: 'safe',
        links: [
            'google.com', 'youtube.com', 'github.com', 'microsoft.com', 'apple.com',
            'amazon.in', 'flipkart.com', 'wikipedia.org', 'reddit.com', 'openai.com',
            'sbi.co.in', 'onlinesbi.sbi', 'hdfcbank.com', 'icicibank.com', 'axisbank.com',
            'paytm.com', 'phonepe.com', 'gpay.google', 'npci.org.in', 'upi.npci.org.in'
        ]
    }
};

// Render links
Object.entries(sets).forEach(([id, data]) => {
    const container = document.getElementById(id);
    if (!container) return;

    data.links.forEach(domain => {
        const a = document.createElement('a');
        a.href = `https://${domain}`;
        a.className = `link-card ${data.type}`;
        a.textContent = domain;
        a.addEventListener('click', (e) => {
            logEntry(domain, data.type);
        });
        container.appendChild(a);
    });
});

// Rotation test logging
document.querySelectorAll('.rotation-test a').forEach(a => {
    a.addEventListener('click', (e) => {
        const domain = a.href.replace('https://', '');
        logEntry(domain, 'high', true);
    });
});

// Log functions
function logEntry(domain, type, isRotation = false) {
    const log = document.getElementById('log');
    const time = new Date().toLocaleTimeString();
    const prefix = isRotation ? '[ROTATION] ' : '';
    const entry = document.createElement('div');
    entry.className = 'entry';
    entry.textContent = `${time} ${prefix}→ ${domain} (expected: ${type.toUpperCase()})`;
    log.insertBefore(entry, log.firstChild);
}

function clearLog() {
    document.getElementById('log').innerHTML = '';
}
