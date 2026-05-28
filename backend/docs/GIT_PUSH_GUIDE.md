# Git Push Guide - AnteClick Backend

## Quick Commands (Copy-Paste Ready)

### If you DON'T have a GitHub repository yet:

```bash
# Navigate to backend directory
cd c:\AndroidProjects\sbi\backend

# Initialize git (if not already done)
git init

# Add all files
git add .

# Commit
git commit -m "AnteClick backend - production ready v1.0"

# Create repository on GitHub first, then:
git remote add origin https://github.com/YOUR_USERNAME/AnteClick-backend.git

# Push to GitHub
git push -u origin main
```

### If you ALREADY have a GitHub repository:

```bash
# Navigate to backend directory
cd c:\AndroidProjects\sbi\backend

# Add all files
git add .

# Commit
git commit -m "AnteClick backend - production ready v1.0"

# Push to GitHub
git push
```

---

## Detailed Step-by-Step Instructions

### Step 1: Check Current Git Status

```bash
cd c:\AndroidProjects\sbi\backend
git status
```

**Expected output:**
- If git is initialized: Shows modified/untracked files
- If git is NOT initialized: `fatal: not a git repository`

---

### Step 2: Initialize Git (if needed)

```bash
git init
```

**Output:** `Initialized empty Git repository in c:/AndroidProjects/sbi/backend/.git/`

---

### Step 3: Add All Files

```bash
git add .
```

This stages all files for commit.

**Verify:**
```bash
git status
```

Should show files in green (staged for commit).

---

### Step 4: Commit Changes

```bash
git commit -m "AnteClick backend - production ready v1.0"
```

**Output:** Shows number of files changed and insertions.

---

### Step 5: Create GitHub Repository

1. Go to https://github.com
2. Click "+" → "New repository"
3. Fill in:
   - **Repository name:** `AnteClick-backend`
   - **Description:** `Production backend for AnteClick phishing detection app`
   - **Visibility:** Private (recommended) or Public
   - **DO NOT** initialize with README, .gitignore, or license
4. Click "Create repository"

---

### Step 6: Connect to GitHub

Copy the commands from GitHub (they'll look like this):

```bash
git remote add origin https://github.com/YOUR_USERNAME/AnteClick-backend.git
git branch -M main
git push -u origin main
```

**Or manually:**

```bash
# Add remote
git remote add origin https://github.com/YOUR_USERNAME/AnteClick-backend.git

# Rename branch to main (if needed)
git branch -M main

# Push to GitHub
git push -u origin main
```

---

### Step 7: Verify Push

Go to your GitHub repository URL:
```
https://github.com/YOUR_USERNAME/AnteClick-backend
```

You should see all your backend files.

---

## What Gets Pushed?

### ✅ Files Included:

```
backend/
├── app/                    # All application code
├── tests/                  # All test files
├── requirements.txt        # Dependencies
├── Dockerfile             # Docker configuration
├── docker-compose.yml     # Local development
├── docker-compose.prod.yml # Production deployment
├── render.yaml            # Render configuration
├── railway.json           # Railway configuration
├── fly.toml               # Fly.io configuration
├── .env.example           # Environment template
├── .gitignore             # Git ignore rules
├── README.md              # Documentation
├── START_HERE.md
├── DEPLOYMENT.md
├── QUICK_DEPLOY.md
├── PRODUCTION_READINESS_REPORT.md
├── VERIFICATION_SUMMARY.md
├── DEPLOYMENT_CHECKLIST.md
├── ANDROID_INTEGRATION.md
├── ARCHITECTURE.md
├── BACKEND_SUMMARY.md
├── PLATFORM_COMPARISON.md
├── deploy-railway.sh
├── deploy-railway.ps1
└── verify_deployment.py
```

### ❌ Files Excluded (via .gitignore):

```
.env                       # Secrets (API keys, passwords)
__pycache__/              # Python cache
*.pyc                     # Compiled Python
.pytest_cache/            # Test cache
.venv/                    # Virtual environment
venv/                     # Virtual environment
*.log                     # Log files
.DS_Store                 # macOS files
```

---

## Troubleshooting

### Issue: `fatal: not a git repository`

**Solution:**
```bash
git init
```

---

### Issue: `remote origin already exists`

**Solution:**
```bash
# Remove old remote
git remote remove origin

# Add new remote
git remote add origin https://github.com/YOUR_USERNAME/AnteClick-backend.git
```

---

### Issue: `failed to push some refs`

**Cause:** Remote has changes you don't have locally

**Solution:**
```bash
# Pull remote changes first
git pull origin main --rebase

# Then push
git push origin main
```

---

### Issue: Authentication failed

**Solution (HTTPS):**
1. Use Personal Access Token instead of password
2. Go to GitHub → Settings → Developer settings → Personal access tokens
3. Generate new token (classic)
4. Select scopes: `repo` (full control)
5. Copy token
6. Use token as password when pushing

**Solution (SSH - Recommended):**
```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your_email@example.com"

# Copy public key
cat ~/.ssh/id_ed25519.pub

# Add to GitHub: Settings → SSH and GPG keys → New SSH key

# Change remote to SSH
git remote set-url origin git@github.com:YOUR_USERNAME/AnteClick-backend.git

# Push
git push
```

---

### Issue: Large files rejected

**Cause:** Files over 100MB

**Solution:**
```bash
# Check file sizes
git ls-files | xargs ls -lh | sort -k5 -h

# Remove large files from git
git rm --cached path/to/large/file

# Add to .gitignore
echo "path/to/large/file" >> .gitignore

# Commit and push
git commit -m "Remove large files"
git push
```

---

## After Pushing to GitHub

### Deploy to Render:

1. Go to https://render.com
2. Click "New +" → "Web Service"
3. Connect GitHub account
4. Select `AnteClick-backend` repository
5. Configure:
   - **Name:** `AnteClick-backend`
   - **Branch:** `main`
   - **Build Command:** Auto-detected from Dockerfile
   - **Start Command:** Auto-detected from Dockerfile
6. Add environment variables (see QUICK_DEPLOY.md)
7. Click "Create Web Service"

### Deploy to Railway:

```bash
# Install Railway CLI
iwr https://railway.app/install.ps1 | iex

# Login
railway login

# Link to project
railway link

# Deploy
railway up
```

---

## Git Workflow for Future Updates

### Making Changes:

```bash
# 1. Make your code changes

# 2. Check what changed
git status
git diff

# 3. Stage changes
git add .

# 4. Commit with descriptive message
git commit -m "Add OpenPhish integration"

# 5. Push to GitHub
git push

# 6. Render/Railway will auto-deploy
```

### Creating Branches:

```bash
# Create feature branch
git checkout -b feature/openphish-integration

# Make changes and commit
git add .
git commit -m "Add OpenPhish feed loader"

# Push branch
git push -u origin feature/openphish-integration

# Merge to main (on GitHub via Pull Request)
# Or locally:
git checkout main
git merge feature/openphish-integration
git push
```

---

## Commit Message Best Practices

### Good Commit Messages:

```bash
git commit -m "Add Redis caching for threat analysis"
git commit -m "Fix: Handle malformed URLs gracefully"
git commit -m "Update: Increase rate limit to 100/min"
git commit -m "Docs: Add Render deployment guide"
git commit -m "Test: Add comprehensive endpoint tests"
```

### Bad Commit Messages:

```bash
git commit -m "fix"
git commit -m "update"
git commit -m "changes"
git commit -m "asdf"
```

---

## Security Checklist

### ✅ Before Pushing:

- [ ] `.env` file is in `.gitignore`
- [ ] No API keys in code
- [ ] No passwords in code
- [ ] No database credentials in code
- [ ] `.env.example` has placeholder values only

### ⚠️ If You Accidentally Pushed Secrets:

```bash
# 1. Remove from git history
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env" \
  --prune-empty --tag-name-filter cat -- --all

# 2. Force push
git push origin --force --all

# 3. Rotate all exposed secrets immediately
# - Generate new API keys
# - Change passwords
# - Update environment variables on Render/Railway
```

---

## Quick Reference

### Common Commands:

```bash
# Check status
git status

# View changes
git diff

# Stage all files
git add .

# Stage specific file
git add path/to/file.py

# Commit
git commit -m "Your message"

# Push
git push

# Pull latest changes
git pull

# View commit history
git log --oneline

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# View remote URL
git remote -v

# Change remote URL
git remote set-url origin https://github.com/USER/REPO.git
```

---

## Next Steps After Push

1. ✅ Push code to GitHub
2. ✅ Deploy to Render (see QUICK_DEPLOY.md)
3. ✅ Configure environment variables
4. ✅ Test deployment
5. ✅ Update Android app with backend URL
6. ⏭️ Create privacy policy
7. ⏭️ Submit to Play Store

---

**Need Help?**
- Git Documentation: https://git-scm.com/doc
- GitHub Guides: https://guides.github.com
- Render Deployment: See `QUICK_DEPLOY.md`

🚀 **Ready to push!**
