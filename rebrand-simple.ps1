# AnteClick Rebranding Script - Simplified Version
# Run this to complete the rebranding migration

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AnteClick Rebranding Migration" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$rootPath = "c:\AndroidProjects\sbi"
$filesModified = 0

# Phase 1: Update Kotlin files
Write-Host "Phase 1: Updating Kotlin files..." -ForegroundColor Yellow

$kotlinPath = "$rootPath\app\src\main\java\com\trustshield"
if (Test-Path $kotlinPath) {
    Get-ChildItem -Path $kotlinPath -Recurse -Filter "*.kt" | ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        $content = $content -replace 'package com\.trustshield\.app', 'package com.anteclick.app'
        $content = $content -replace 'import com\.trustshield\.app', 'import com.anteclick.app'
        $content = $content -replace 'TAG = "TrustShield"', 'TAG = "AnteClick"'
        $content = $content -replace 'TrustShieldColors', 'AnteClickColors'
        $content = $content -replace 'TrustShieldType', 'AnteClickType'
        $content = $content -replace 'TrustShieldTheme', 'AnteClickTheme'
        Set-Content -Path $_.FullName -Value $content -NoNewline
        $filesModified++
        Write-Host "  Updated: $($_.Name)" -ForegroundColor Green
    }
}

# Phase 2: Rename theme file
Write-Host "`nPhase 2: Renaming theme file..." -ForegroundColor Yellow

$themeFile = "$rootPath\app\src\main\java\com\trustshield\app\ui\theme\TrustShieldTheme.kt"
if (Test-Path $themeFile) {
    Rename-Item -Path $themeFile -NewName "AnteClickTheme.kt"
    Write-Host "  Renamed: TrustShieldTheme.kt -> AnteClickTheme.kt" -ForegroundColor Green
}

# Phase 3: Move package directory
Write-Host "`nPhase 3: Moving package directory..." -ForegroundColor Yellow

$oldDir = "$rootPath\app\src\main\java\com\trustshield"
$newDir = "$rootPath\app\src\main\java\com\anteclick"
if (Test-Path $oldDir) {
    Move-Item -Path $oldDir -Destination $newDir -Force
    Write-Host "  Moved: com\trustshield -> com\anteclick" -ForegroundColor Green
}

# Phase 4: Update backend files
Write-Host "`nPhase 4: Updating backend files..." -ForegroundColor Yellow

Get-ChildItem -Path "$rootPath\backend" -Recurse -Include "*.py","*.md","*.yaml","*.json","*.toml","*.sh","*.ps1","Dockerfile","docker-compose*.yml" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace 'TrustShield', 'AnteClick'
    $content = $content -replace 'trustshield', 'anteclick'
    Set-Content -Path $_.FullName -Value $content -NoNewline
    $filesModified++
    Write-Host "  Updated: $($_.Name)" -ForegroundColor Green
}

# Phase 5: Update root documentation
Write-Host "`nPhase 5: Updating root documentation..." -ForegroundColor Yellow

Get-ChildItem -Path $rootPath -Include "*.md","*.txt" -Depth 1 | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace 'TrustShield', 'AnteClick'
    $content = $content -replace 'trustshield', 'anteclick'
    $content = $content -replace 'com\.trustshield\.app', 'com.anteclick.app'
    Set-Content -Path $_.FullName -Value $content -NoNewline
    $filesModified++
    Write-Host "  Updated: $($_.Name)" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Migration Complete!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Files modified: $filesModified`n" -ForegroundColor Green

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Test Android build: cd app && ./gradlew build"
Write-Host "2. Deploy backend to Render with new name"
Write-Host "3. Update backend URL in ThreatRepository.kt"
Write-Host "4. Test end-to-end integration"
Write-Host "5. Commit: git add . && git commit -m 'Rebrand to AnteClick' && git push`n"
