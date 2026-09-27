# Run only in this project, with project-local tools, temporary files and caches.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$localJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home'
$env:TEMP = (New-Item -ItemType Directory -Force (Join-Path $projectRoot '.tools/tmp')).FullName
$env:TMP = $env:TEMP
$proxyArgs = @()
if ($env:HTTPS_PROXY) {
    $buildProxy = [Uri]$env:HTTPS_PROXY
    $proxyArgs = @("-Dhttps.proxyHost=$($buildProxy.Host)", "-Dhttps.proxyPort=$($buildProxy.Port)", "-Dhttp.proxyHost=$($buildProxy.Host)", "-Dhttp.proxyPort=$($buildProxy.Port)")
}
& (Join-Path $projectRoot 'gradlew.bat') @args @proxyArgs --console=plain --no-daemon
exit $LASTEXITCODE
