# AnteClick Backend - Railway Quick Deploy Script (Windows)
# This script automates the deployment to Railway

Write-Host "🚀 AnteClick Backend - Railway Deployment" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Railway CLI is installed
if (-not (Get-Command railway -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Railway CLI not found. Installing..." -ForegroundColor Red
    npm install -g @railway/cli
    Write-Host "✅ Railway CLI installed" -ForegroundColor Green
}

# Login to Railway
Write-Host ""
Write-Host "📝 Logging in to Railway..." -ForegroundColor Yellow
railway login

# Initialize project
Write-Host ""
Write-Host "🔧 Initializing Railway project..." -ForegroundColor Yellow
railway init

# Add Redis
Write-Host ""
Write-Host "📦 Adding Redis addon..." -ForegroundColor Yellow
railway add redis

# Generate secure API key
Write-Host ""
Write-Host "🔐 Generating secure API key..." -ForegroundColor Yellow
$API_KEY = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
Write-Host "Generated API Key: $API_KEY" -ForegroundColor Green
Write-Host "⚠️  SAVE THIS KEY - You'll need it for the Android app!" -ForegroundColor Yellow

# Set environment variables
Write-Host ""
Write-Host "⚙️  Setting environment variables..." -ForegroundColor Yellow
railway variables set API_KEY="$API_KEY"
railway variables set ENVIRONMENT=production
railway variables set LOG_LEVEL=INFO
railway variables set RATE_LIMIT_PER_MINUTE=60
railway variables set RATE_LIMIT_PER_HOUR=1000

# Deploy
Write-Host ""
Write-Host "🚀 Deploying to Railway..." -ForegroundColor Yellow
railway up

# Get domain
Write-Host ""
Write-Host "🌐 Getting deployment URL..." -ForegroundColor Yellow
$DOMAIN = railway domain

Write-Host ""
Write-Host "✅ Deployment complete!" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Deployment Summary:" -ForegroundColor Cyan
Write-Host "  • API URL: https://$DOMAIN" -ForegroundColor White
Write-Host "  • API Key: $API_KEY" -ForegroundColor White
Write-Host "  • Health Check: https://$DOMAIN/health" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Test the API:" -ForegroundColor White
Write-Host "     curl -H `"X-API-Key: $API_KEY`" `"https://$DOMAIN/analyze?domain=sbi-login.xyz`"" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Update Android app (ThreatRepository.kt):" -ForegroundColor White
Write-Host "     private const val BASE_URL = `"https://$DOMAIN/`"" -ForegroundColor Gray
Write-Host "     private const val API_KEY = `"$API_KEY`"" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Monitor logs:" -ForegroundColor White
Write-Host "     railway logs" -ForegroundColor Gray
Write-Host ""
Write-Host "🎉 Your AnteClick backend is live!" -ForegroundColor Green
