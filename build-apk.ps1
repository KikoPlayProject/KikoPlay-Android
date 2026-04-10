param(
    [ValidateSet("debug", "release")]
    [string]$Variant = "debug",

    [switch]$Clean,

    [switch]$NoDaemon
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$preferredGradleHomes = @(
    (Join-Path $projectRoot ".gradle-user-home-allzip"),
    (Join-Path $projectRoot ".gradle-user-home-build")
)

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Cannot find gradlew.bat in $projectRoot"
}

$javaHomes = @()
if ($env:JAVA_HOME) {
    $javaHomes += $env:JAVA_HOME
}
$javaHomes += @(
    "D:\Program Files\Android\Android Studio\jbr"
)

$javaHome = $null
foreach ($candidate in $javaHomes) {
    if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate "bin\java.exe"))) {
        $javaHome = $candidate
        break
    }
}

if (-not $javaHome) {
    throw "Java not found. Please install Android Studio or set JAVA_HOME to a valid JDK/JBR."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$gradleUserHome = $preferredGradleHomes | Select-Object -First 1
foreach ($candidate in $preferredGradleHomes) {
    if (Test-Path -LiteralPath $candidate) {
        $gradleUserHome = $candidate
        break
    }
}
$env:GRADLE_USER_HOME = $gradleUserHome

$tasks = @()
if ($Clean) {
    $tasks += "clean"
}

$assembleTask = if ($Variant -eq "release") {
    ":app:assembleRelease"
} else {
    ":app:assembleDebug"
}
$tasks += $assembleTask

$gradleArgs = @("--console=plain")
if ($NoDaemon) {
    $gradleArgs += "--no-daemon"
}
$gradleArgs += $tasks

Write-Host "Project: $projectRoot"
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "GRADLE_USER_HOME: $env:GRADLE_USER_HOME"
Write-Host "Running: .\gradlew.bat $($gradleArgs -join ' ')"

Push-Location $projectRoot
try {
    & $gradleWrapper @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

$apkDir = Join-Path $projectRoot "app\build\outputs\apk\$Variant"
$apk = Get-ChildItem -LiteralPath $apkDir -Filter "*.apk" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($apk) {
    Write-Host ""
    Write-Host "APK generated:"
    Write-Host $apk.FullName
} else {
    Write-Host ""
    Write-Host "Build finished, but no APK was found under:"
    Write-Host $apkDir
}
