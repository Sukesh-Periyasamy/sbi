# AnteClick Rebranding Migration Script
# Performs production-safe rebranding from TrustShield to AnteClick

param(
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Stop"
$rootPath = "c:\AndroidProjects\sbi"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AnteClick Rebranding Migration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "⚠️  DRY RUN MODE - No files will be modified" -ForegroundColor Yellow
    Write-Host ""
}

# Track changes
$filesModified = 0
$errors = @()

function Update-FileContent {
    param(
        [string]$FilePath,
        [hashtable]$Replacements
    )
    
    try {
        if (-not (Test-Path $FilePath)) {
            Write-Host "  ⚠️  File not found: $FilePath" -ForegroundColor Yellow
            return
        }
        
        $content = Get-Content $FilePath -Raw -Encoding UTF8
        $originalContent = $content
        
        foreach ($key in $Replacements.Keys) {
            $content = $content -replace $key, $Replacements[$key]
        }
        
        if ($content -ne $originalContent) {
            if (-not $DryRun) {
                Set-Content -Path $FilePath -Value $content -Encoding UTF8 -NoNewline
            }
            Write-Host "  ✓ Updated: $(Split-Path $FilePath -Leaf)" -ForegroundColor Green
            $script:filesModified++
        }
    }
    catch {
        $script:errors += "Error updating $FilePath : $_"
        Write-Host "  ✗ Error: $(Split-Path $FilePath -Leaf)" -ForegroundColor Red
    }
}

# ============================================================================
# PHASE 1: ANDROID KOTLIN FILES
# ============================================================================

Write-Host "Phase 1: Updating Android Kotlin Files..." -ForegroundColor Cyan

$kotlinFiles = Get-ChildItem -Path "$rootPath\app\src\main\java\com\trustshield" -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue

if ($kotlinFiles) {
    foreach ($file in $kotlinFiles) {
        $replacements = @{
            'package com\.trustshield\.app' = 'package com.anteclick.app'
            'import com\.trustshield\.app' = 'import com.anteclick.app'
            'private const val TAG = "TrustShield"' = 'private const val TAG = "AnteClick"'
            'TAG = "TrustShield"' = 'TAG = "AnteClick"'
            'TrustShieldColors' = 'AnteClickColors'
            'TrustShieldType' = 'AnteClickType'
            'TrustShieldTheme' = 'AnteClickTheme'
            '"TrustShield Backend' = '"AnteClick Backend'
            '"trustshield-backend' = '"anteclick-backend'
            'TrustShield Backend' = 'AnteClick Backend'
            'trustshield:' = 'anteclick:'
        }
        Update-FileContent -FilePath $file.FullName -Replacements $replacements
    }
}
else {
    Write-Host "  ⚠️  No Kotlin files found in trustshield package" -ForegroundColor Yellow
}

# ============================================================================
# PHASE 2: RENAME THEME FILE
# ============================================================================

Write-Host ""
Write-Host "Phase 2: Renaming Theme File..." -ForegroundColor Cyan

$themeFile = "$rootPath\app\src\main\java\com\trustshield\app\ui\theme\TrustShieldTheme.kt"
$newThemeFile = "$rootPath\app\src\main\java\com\trustshield\app\ui\theme\AnteClickTheme.kt"

if (Test-Path $themeFile) {
    if (-not $DryRun) {
        Rename-Item -Path $themeFile -NewName "AnteClickTheme.kt" -Force
    }
    Write-Host "  ✓ Renamed: TrustShieldTheme.kt → AnteClickTheme.kt" -ForegroundColor Green
    $filesModified++
}
else {
    Write-Host "  ⚠️  Theme file not found" -ForegroundColor Yellow
}

# ============================================================================
# PHASE 3: MOVE PACKAGE DIRECTORY
# ============================================================================

Write-Host ""
Write-Host "Phase 3: Moving Package Directory..." -ForegroundColor Cyan

$oldPackageDir = "$rootPath\app\src\main\java\com\trustshield"
$newPackageDir = "$rootPath\app\src\main\java\com\anteclick"

if (Test-Path $oldPackageDir) {
    if (-not $DryRun) {
        # Create parent directory if it doesn't exist
        $parentDir = Split-Path $newPackageDir -Parent
        if (-not (Test-Path $parentDir)) {
            New-Item -Path $parentDir -ItemType Directory -Force | Out-Null
        }
        
        # Move directory
        Move-Item -Path $oldPackageDir -Destination $newPackageDir -Force
    }
    Write-Host "  ✓ Moved: com\trustshield → com\anteclick" -ForegroundColor Green
    $filesModified++
}
else {
    Write-Host "  ⚠️  Package directory not found or already moved" -ForegroundColor Yellow
}

# ============================================================================
# PHASE 4: BACKEND PYTHON FILES
# ============================================================================

Write-Host ""
Write-Host "Phase 4: Updating Backend Python Files..." -ForegroundColor Cyan

$pythonFiles = Get-ChildItem -Path "$rootPath\backend" -Recurse -Include "*.py" -ErrorAction SilentlyContinue

foreach ($file in $pythonFiles) {
    $replacements = @{
        'TrustShield Backend API' = 'AnteClick Backend API'
        'TrustShield Backend' = 'AnteClick Backend'
        'trustshield-backend' = 'anteclick-backend'
        'TrustShield phishing' = 'AnteClick phishing'
        '"trustshield:' = '"anteclick:'
        'trustshield:' = 'anteclick:'
        'Starting TrustShield' = 'Starting AnteClick'
        'Shutting down TrustShield' = 'Shutting down AnteClick'
    }
    Update-FileContent -FilePath $file.FullName -Replacements $replacements
}

# ============================================================================
# PHASE 5: BACKEND CONFIGURATION FILES
# ============================================================================

Write-Host ""
Write-Host "Phase 5: Updating Backend Configuration Files..." -ForegroundColor Cyan

$configFiles = Get-ChildItem -Path "$rootPath\backend" -Include "*.yaml","*.json","*.toml","Dockerfile","docker-compose*.yml" -ErrorAction SilentlyContinue

foreach ($file in $configFiles) {
    $replacements = @{
        'trustshield-backend' = 'anteclick-backend'
        'TrustShield Backend' = 'AnteClick Backend'
        'TrustShield phishing' = 'AnteClick phishing'
        'description="TrustShield' = 'description="AnteClick'
    }
    Update-FileContent -FilePath $file.FullName -Replacements $replacements
}

# ============================================================================
# PHASE 6: DOCUMENTATION FILES
# ============================================================================

Write-Host ""
Write-Host "Phase 6: Updating Documentation Files..." -ForegroundColor Cyan

$docFiles = Get-ChildItem -Path "$rootPath\backend" -Include "*.md" -Recurse -ErrorAction SilentlyContinue
$docFiles += Get-ChildItem -Path "$rootPath" -Include "PROJECT_STATUS_REPORT.md","design.txt","idea.txt" -ErrorAction SilentlyContinue

foreach ($file in $docFiles) {
    $replacements = @{
        'TrustShield' = 'AnteClick'
        'Trust Shield' = 'AnteClick'
        'trustshield' = 'anteclick'
        'com\.trustshield\.app' = 'com.anteclick.app'
        'trustshield-backend' = 'anteclick-backend'
        'TRUSTSHIELD' = 'ANTECLICK'
    }
    Update-FileContent -FilePath $file.FullName -Replacements $replacements
}

# ============================================================================
# PHASE 7: DEPLOYMENT SCRIPTS
# ============================================================================

Write-Host ""
Write-Host "Phase 7: Updating Deployment Scripts..." -ForegroundColor Cyan

$scriptFiles = Get-ChildItem -Path "$rootPath\backend" -Include "*.sh","*.ps1" -ErrorAction SilentlyContinue

foreach ($file in $scriptFiles) {
    $replacements = @{
        'trustshield-backend' = 'anteclick-backend'
        'TrustShield' = 'AnteClick'
    }
    Update-FileContent -FilePath $file.FullName -Replacements $replacements
}

# ============================================================================
# SUMMARY
# ============================================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Migration Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "DRY RUN COMPLETE - No files were modified" -ForegroundColor Yellow
    Write-Host "Files that would be modified: $filesModified" -ForegroundColor Yellow
}
else {
    Write-Host "✓ Files Modified: $filesModified" -ForegroundColor Green
}

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "⚠️  Errors Encountered:" -ForegroundColor Red
    foreach ($error in $errors) {
        Write-Host "  - $error" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Next Steps" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Test Android build:" -ForegroundColor White
Write-Host "   cd app && ./gradlew build" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Update backend URL in ThreatRepository.kt:" -ForegroundColor White
Write-Host "   Change BASE_URL to new Render URL" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Deploy backend to Render:" -ForegroundColor White
Write-Host "   Update service name in Render dashboard" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Test end-to-end integration" -ForegroundColor White
Write-Host ""
Write-Host "5. Commit changes:" -ForegroundColor White
Write-Host "   git add ." -ForegroundColor Gray
Write-Host "   git commit -m `"Rebrand to AnteClick`"" -ForegroundColor Gray
Write-Host "   git push" -ForegroundColor Gray
Write-Host ""

if (-not $DryRun) {
    Write-Host "✅ Rebranding migration complete!" -ForegroundColor Green
}
else {
    Write-Host "Run without -DryRun flag to apply changes" -ForegroundColor Yellow
}
