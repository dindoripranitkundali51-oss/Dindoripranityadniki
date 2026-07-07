Add-Type -AssemblyName "System.Data"
$connString = "Server=tcp:DindoriPranitDb.mssql.somee.com,1433;Database=DindoriPranitDb;User Id=dindoriadmin_SQLLogin_1;Password=98njtsyv8o;Encrypt=true;TrustServerCertificate=true;"
$connection = New-Object System.Data.SqlClient.SqlConnection($connString)
try {
    $connection.Open()
    Write-Output "SUCCESS: Local connection to Somee database succeeded!"
    $command = $connection.CreateCommand()
    $command.CommandText = "SELECT COUNT(*) FROM Admins"
    $result = $command.ExecuteScalar()
    Write-Output "Admins count: $result"
} catch {
    Write-Output "FAILED: $($_.Exception.Message)"
} finally {
    $connection.Close()
}
