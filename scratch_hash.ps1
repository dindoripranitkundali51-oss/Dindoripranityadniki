$password = "mangesh@11"
$salt = "admin_owner_kale"
$combined = [System.Text.Encoding]::UTF8.GetBytes($password + $salt)
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$hashBytes = $sha256.ComputeHash($combined)
$hashBase64 = [Convert]::ToBase64String($hashBytes)
Write-Output $hashBase64
