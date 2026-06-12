$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = "D:\JDK177"
$javaExe = Join-Path $javaHome "bin\java.exe"
$mavenLauncherJar = "D:\maven363-iot\boot\plexus-classworlds-2.6.0.jar"
$mavenConf = "D:\maven363-iot\bin\m2.conf"
$mavenHome = "D:\maven363-iot"
$localRepo = Join-Path $projectRoot ".m2repo"
$jarPath = Join-Path $projectRoot "target\knowledge-agent-0.0.1-SNAPSHOT.jar"

if (-not (Test-Path $javaExe)) {
    throw "Java 17 not found at $javaExe"
}

if (-not (Test-Path $mavenLauncherJar)) {
    throw "Maven launcher jar not found at $mavenLauncherJar"
}

if ([string]::IsNullOrWhiteSpace($env:DEEPSEEK_API_KEY)) {
    throw "Missing environment variable DEEPSEEK_API_KEY"
}

if ([string]::IsNullOrWhiteSpace($env:EMBEDDING_API_KEY)) {
    throw "Missing environment variable EMBEDDING_API_KEY"
}

Set-Location $projectRoot

Write-Host "Using Java:" -ForegroundColor Cyan
& $javaExe -version

Write-Host ""
Write-Host "Packaging project..." -ForegroundColor Cyan
& $javaExe `
    "-Dmaven.repo.local=$localRepo" `
    "-Dclassworlds.conf=$mavenConf" `
    "-Dmaven.home=$mavenHome" `
    "-Dlibrary.jansi.path=D:\maven363-iot\lib\jansi-native" `
    "-Dmaven.multiModuleProjectDirectory=$projectRoot" `
    "-classpath" $mavenLauncherJar `
    "org.codehaus.plexus.classworlds.launcher.Launcher" `
    "package" `
    "-DskipTests"

if (-not (Test-Path $jarPath)) {
    throw "Packaged jar not found at $jarPath"
}

Write-Host ""
Write-Host "Starting application on http://localhost:8080 ..." -ForegroundColor Cyan
& $javaExe "-jar" $jarPath
