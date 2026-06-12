$ErrorActionPreference = "Stop"

$pgRoot = "D:\PostgresSQL"
$psql = Join-Path $pgRoot "bin\psql.exe"
$extensionDir = Join-Path $pgRoot "share\extension"
$libDir = Join-Path $pgRoot "lib"
$downloadUrl = "https://raw.githubusercontent.com/andreiramani/pgvector_pgsql_windows/main/zip/0.8.2/vector.v0.8.2-pg18.zip"
$tempDir = Join-Path $env:TEMP "pgvector_install"
$zipPath = Join-Path $tempDir "vector.v0.8.2-pg18.zip"

if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Please run this script from an elevated PowerShell window."
}

if (-not (Test-Path $psql)) {
    throw "psql.exe not found at $psql"
}

if (Test-Path $tempDir) {
    Remove-Item -LiteralPath $tempDir -Recurse -Force
}

New-Item -ItemType Directory -Path $tempDir | Out-Null
Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath
Expand-Archive -LiteralPath $zipPath -DestinationPath $tempDir -Force

Copy-Item -LiteralPath (Join-Path $tempDir "share\extension\vector.control") -Destination (Join-Path $extensionDir "vector.control") -Force
Copy-Item -Path (Join-Path $tempDir "share\extension\vector--*.sql") -Destination $extensionDir -Force
Copy-Item -LiteralPath (Join-Path $tempDir "lib\vector.dll") -Destination (Join-Path $libDir "vector.dll") -Force

if (-not (Test-Path (Join-Path $extensionDir "vector--0.8.2.sql"))) {
    throw "vector SQL installation files were not copied successfully."
}

if (-not $env:PGPASSWORD) {
    throw "Please set PGPASSWORD before running this script."
}

& $psql -U postgres -d knowledge_agent -c "CREATE EXTENSION IF NOT EXISTS vector;"
& $psql -U postgres -d knowledge_agent -c "SELECT extname, extversion FROM pg_extension WHERE extname='vector';"

Write-Host "pgvector installation completed."
