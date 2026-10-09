# Points the CURRENT PowerShell window at JDK 21 (nothing global is changed).
# Usage (note the leading dot, so the settings stay in your window):   . .\scripts\use-jdk21.ps1
# Override the location with:  $env:JDK21_HOME = "D:\somewhere\jdk-21"
$jdk = if ($env:JDK21_HOME) { $env:JDK21_HOME } else { "$env:USERPROFILE\tools\jdk-21" }
if (-not (Test-Path "$jdk\bin\java.exe")) {
    Write-Error "JDK 21 not found at $jdk. Install Temurin 21 (https://adoptium.net) or set JDK21_HOME."
    return
}
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
& "$jdk\bin\java.exe" -version
