param(
  [switch]$ZipOutput
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

foreach ($path in @(".\dist", ".\dist2", ".\release")) {
  if (Test-Path $path) {
    try {
      Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction Stop
    } catch {
      Write-Warning "Could not fully remove $path. Continuing with fresh output paths where possible."
    }
  }
}

npm run packager:win
if ($LASTEXITCODE -ne 0) {
  throw "Native Windows package build failed."
}

$outputDir = Join-Path $PSScriptRoot "release\Dindori Pranit Admin-win32-x64"
$exePath = Join-Path $outputDir "Dindori Pranit Admin.exe"
if (-not (Test-Path $exePath)) {
  throw "Expected EXE not found at $exePath"
}

if ($ZipOutput) {
  $zipPath = Join-Path $PSScriptRoot "release\Dindori-Pranit-Admin-win32-x64.zip"
  if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
  Compress-Archive -Path (Join-Path $outputDir "*") -DestinationPath $zipPath
}

Write-Host "Desktop package ready at: $exePath"
