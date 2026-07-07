# =====================================================================
# DINDORI PRANIT YADNYIKI - UNIFIED ADMIN BUILD & PUBLISH SCRIPT
# =====================================================================
# This script builds the Next.js Web Admin Panel, embeds it into the .NET Core 
# API wwwroot, packages the Electron app, publishes the API server to a single
# publish.zip, and outputs the desktop zip package to the root workspace.
# =====================================================================

$ErrorActionPreference = "Stop"
$PSScriptRoot = Get-Location

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 1: Building Web Admin Panel..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location (Join-Path $PSScriptRoot "web-admin-panel")
if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules not found in web-admin-panel. Running npm install..." -ForegroundColor Yellow
    npm install
}
$env:NEXT_PUBLIC_BASE_PATH = "/admin"
npm run build
Remove-Item env:NEXT_PUBLIC_BASE_PATH -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 2: Embedding Web Admin Panel into API wwwroot..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$wwwrootAdmin = Join-Path $PSScriptRoot "backend-dotnet\DindoriPranitAPI\wwwroot\admin"
if (Test-Path $wwwrootAdmin) {
    Remove-Item $wwwrootAdmin -Recurse -Force
}
New-Item -ItemType Directory -Path $wwwrootAdmin -Force | Out-Null

$outFolder = Join-Path $PSScriptRoot "web-admin-panel\out"
Copy-Item -Path (Join-Path $outFolder "*") -Destination $wwwrootAdmin -Recurse -Force
Write-Host "SUCCESS: Embedded static admin UI files inside $wwwrootAdmin" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 3: Building Electron Desktop App..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location (Join-Path $PSScriptRoot "web-admin-panel")
if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules not found in web-admin-panel. Running npm install..." -ForegroundColor Yellow
    npm install
}
# Run the desktop packager script with zip output enabled
powershell -ExecutionPolicy Bypass -File .\build-desktop.ps1 -ZipOutput

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 4: Publishing Desktop Zip to Root..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$sourceZip = Join-Path $PSScriptRoot "web-admin-panel\release\Dindori-Pranit-Admin-win32-x64.zip"
$destZip = Join-Path $PSScriptRoot "Dindori-Pranit-Admin-Desktop.zip"

if (Test-Path $sourceZip) {
    Copy-Item -Path $sourceZip -Destination $destZip -Force
    Write-Host "SUCCESS: Published desktop package to: $destZip" -ForegroundColor Green
} else {
    Write-Error "ERROR: Expected build zip not found at $sourceZip"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 5: Publishing API Project with Embedded Admin Panel..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location (Join-Path $PSScriptRoot "backend-dotnet\DindoriPranitAPI")
$localPublish = Join-Path $PSScriptRoot "backend-dotnet\DindoriPranitAPI\publish"

# Clean up older local publish folder if exists
if (Test-Path $localPublish) {
    Remove-Item $localPublish -Recurse -Force
}

# Run dotnet publish
dotnet publish --configuration Release --output $localPublish --self-contained false

# Zip the final publish folder and output to root
$destPublishZip = Join-Path $PSScriptRoot "publish.zip"
if (Test-Path $destPublishZip) {
    Remove-Item $destPublishZip -Force
}

Write-Host "Zipping unified publish package..." -ForegroundColor Yellow
Compress-Archive -Path (Join-Path $localPublish "*") -DestinationPath $destPublishZip
Write-Host "SUCCESS: Published unified server zip to: $destPublishZip" -ForegroundColor Green

Set-Location $PSScriptRoot
Write-Host ""
Write-Host "Unified Build & Publish Completed Successfully!" -ForegroundColor Green
