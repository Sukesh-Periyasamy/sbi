#!/bin/bash

# AnteClick Backend - Railway Quick Deploy Script
# This script automates the deployment to Railway

set -e

echo "🚀 AnteClick Backend - Railway Deployment"
echo "==========================================="
echo ""

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI not found. Installing..."
    npm install -g @railway/cli
    echo "✅ Railway CLI installed"
fi

# Login to Railway
echo ""
echo "📝 Logging in to Railway..."
railway login

# Initialize project
echo ""
echo "🔧 Initializing Railway project..."
railway init

# Add Redis
echo ""
echo "📦 Adding Redis addon..."
railway add redis

# Generate secure API key
echo ""
echo "🔐 Generating secure API key..."
API_KEY=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")
echo "Generated API Key: $API_KEY"
echo "⚠️  SAVE THIS KEY - You'll need it for the Android app!"

# Set environment variables
echo ""
echo "⚙️  Setting environment variables..."
railway variables set API_KEY="$API_KEY"
railway variables set ENVIRONMENT=production
railway variables set LOG_LEVEL=INFO
railway variables set RATE_LIMIT_PER_MINUTE=60
railway variables set RATE_LIMIT_PER_HOUR=1000

# Deploy
echo ""
echo "🚀 Deploying to Railway..."
railway up

# Get domain
echo ""
echo "🌐 Getting deployment URL..."
DOMAIN=$(railway domain)

echo ""
echo "✅ Deployment complete!"
echo "==========================================="
echo ""
echo "📋 Deployment Summary:"
echo "  • API URL: https://$DOMAIN"
echo "  • API Key: $API_KEY"
echo "  • Health Check: https://$DOMAIN/health"
echo ""
echo "🔧 Next Steps:"
echo "  1. Test the API:"
echo "     curl -H \"X-API-Key: $API_KEY\" \"https://$DOMAIN/analyze?domain=sbi-login.xyz\""
echo ""
echo "  2. Update Android app (ThreatRepository.kt):"
echo "     private const val BASE_URL = \"https://$DOMAIN/\""
echo "     private const val API_KEY = \"$API_KEY\""
echo ""
echo "  3. Monitor logs:"
echo "     railway logs"
echo ""
echo "🎉 Your AnteClick backend is live!"
