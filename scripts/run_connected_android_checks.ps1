param(
  [switch]$WaitForDevice
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Get-ConnectedDeviceCount {
  $lines = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\bdevice\b" }
  return @($lines).Count
}

if ($WaitForDevice) {
  Write-Host "Waiting for Android device..."
  adb wait-for-device | Out-Null
}

$deviceCount = Get-ConnectedDeviceCount
if ($deviceCount -lt 1) {
  throw "No connected Android device found. Connect a phone/emulator and re-run this script."
}

Write-Host "Connected Android devices: $deviceCount"

& .\gradlew.bat :app:connectedDebugAndroidTest --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) {
  throw "connectedDebugAndroidTest failed."
}

Write-Host "Connected Android instrumentation run passed."
