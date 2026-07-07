param(
  [string]$DeviceId = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
  throw "adb not found in PATH."
}

$adbOutput = adb devices
$deviceLines = $adbOutput | Where-Object { $_ -match "device$" -and $_ -notmatch "^List of devices" }
if (-not $deviceLines) {
  throw "No connected Android device found. Attach a device and enable USB debugging."
}

$target = $DeviceId
if (-not $target) {
  $target = ($deviceLines[0] -split "\s+")[0]
}

Write-Host "Running connected Android instrumentation tests on device: $target"
& .\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dindoripranityadnyiki.ExampleInstrumentedTest
if ($LASTEXITCODE -ne 0) {
  throw "Connected Android instrumentation tests failed."
}

Write-Host "Connected Android instrumentation tests passed on: $target"
