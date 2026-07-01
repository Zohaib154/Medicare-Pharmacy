# MediCare Standalone Desktop Build Script
# Builds the fat JAR, bundles JavaFX modules, and creates a double-clickable app-image.

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "      MediCare Standalone Desktop Builder              " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Compile Spring Boot + JavaFX package
Write-Host "[1/5] Compiling Spring Boot and JavaFX package..." -ForegroundColor Green
$mvn = Join-Path $ProjectRoot "maven-dist\apache-maven-3.9.6\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    $mvn = "mvn"
}
& $mvn -f (Join-Path $ProjectRoot "pom.xml") clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile Maven project."
}
Write-Host "Maven compilation successful." -ForegroundColor Yellow

$jarPath = Join-Path $ProjectRoot "target\medicare-system-1.0.0.jar"
$javafxLib = Join-Path $ProjectRoot "target\javafx-lib"
if (-not (Test-Path $jarPath)) {
    throw "Expected JAR not found at $jarPath"
}
if (-not (Test-Path $javafxLib)) {
    throw "Expected JavaFX modules not found at $javafxLib"
}

# 2. Package standalone app-image using jpackage
Write-Host "[2/5] Building native app-image..." -ForegroundColor Green
$jpackageInput = Join-Path $ProjectRoot "target\jpackage-input"
$distDir = Join-Path $ProjectRoot "target\dist"
Remove-Item -Recurse -Force $jpackageInput, $distDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $jpackageInput -Force | Out-Null
Copy-Item $jarPath (Join-Path $jpackageInput "medicare-system-1.0.0.jar")

$javaCmd = (Get-Command java -ErrorAction Stop).Source
$javaHome = Split-Path (Split-Path $javaCmd)
$jpackage = Join-Path $javaHome "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) {
    throw "jpackage not found at $jpackage. Install JDK 17+ with jpackage support."
}

$iconPath = Join-Path $ProjectRoot "src\main\resources\rxpro.ico"

& $jpackage `
    --type app-image `
    --name MediCare `
    --input $jpackageInput `
    --main-jar medicare-system-1.0.0.jar `
    --main-class org.springframework.boot.loader.launch.JarLauncher `
    --dest $distDir `
    --icon $iconPath `
    --module-path $javafxLib `
    --add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base,javafx.media `
    --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" `
    --java-options "--add-opens=javafx.web/com.sun.webkit=ALL-UNNAMED"

if ($LASTEXITCODE -ne 0) {
    throw "jpackage app-image creation failed."
}

$medicareExe = Join-Path $distDir "MediCare\MediCare.exe"
if (-not (Test-Path $medicareExe)) {
    throw "Expected desktop launcher not found at $medicareExe"
}
Write-Host "Standalone desktop app created: $medicareExe" -ForegroundColor Yellow

# 3. Compile MediCareLauncher.cs (standalone exe launcher)
Write-Host "[3/5] Compiling MediCareLauncher.exe..." -ForegroundColor Green
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$launcherCs = Join-Path $ProjectRoot "MediCareLauncher.cs"
$launcherOut = Join-Path $ProjectRoot "MediCareLauncher.exe"
if ((Test-Path $csc) -and (Test-Path $launcherCs)) {
    & $csc /target:winexe /out:$launcherOut /r:System.Windows.Forms.dll /win32icon:$iconPath $launcherCs
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to compile MediCareLauncher.cs"
    }
    Write-Host "MediCareLauncher compiled successfully: $launcherOut" -ForegroundColor Yellow

    $distAppDir = Join-Path $distDir "MediCare"
    if (Test-Path $distAppDir) {
        Copy-Item $launcherOut (Join-Path $distAppDir "MediCareLauncher.exe") -Force
        Write-Host "MediCareLauncher copied to: $distAppDir" -ForegroundColor Yellow
    }
} else {
    Write-Host "Skipping MediCareLauncher build (CSC or MediCareLauncher.cs not found)." -ForegroundColor DarkYellow
}

# 4. Compile Uninstaller.cs (optional admin uninstall helper)
Write-Host "[4/5] Compiling administrative Uninstaller..." -ForegroundColor Green
$uninstallerCs = Join-Path $ProjectRoot "Uninstaller.cs"
if ((Test-Path $csc) -and (Test-Path $uninstallerCs)) {
    $uninstallerOut = Join-Path $distDir "MediCare\uninstall.exe"
    $manifestPath = Join-Path $ProjectRoot "app.manifest"
    & $csc /target:winexe /out:$uninstallerOut /win32manifest:$manifestPath /win32icon:$iconPath $uninstallerCs
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to compile Uninstaller.cs"
    }
    Write-Host "Uninstaller compiled successfully." -ForegroundColor Yellow
} else {
    Write-Host "Skipping uninstaller build (CSC or Uninstaller.cs not found)." -ForegroundColor DarkYellow
}

# 4. Package target/dist/RxPro/ into a ZIP archive
Write-Host "[5/5] Zipping application image..." -ForegroundColor Green
$zipPath = Join-Path $ProjectRoot "target\app.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $distDir "MediCare"), $zipPath)
Write-Host "Application packaged into $zipPath successfully." -ForegroundColor Yellow

# 5. Compile Installer.cs embedding the app.zip (optional setup EXE)
Write-Host "[6/6] Compiling C# Setup EXE..." -ForegroundColor Green
$installerCs = Join-Path $ProjectRoot "Installer.cs"
$installerOut = Join-Path $ProjectRoot "MediCareSetup.exe"
if ((Test-Path $csc) -and (Test-Path $installerCs)) {
    if (Test-Path $installerOut) {
        Remove-Item $installerOut -Force
    }
    $manifestPath = Join-Path $ProjectRoot "app.manifest"
    & $csc /target:winexe /out:$installerOut /r:System.IO.Compression.dll /r:System.IO.Compression.FileSystem.dll /r:System.Drawing.dll /resource:$zipPath,app.zip /resource:$iconPath,rxpro.ico /win32manifest:$manifestPath /win32icon:$iconPath $installerCs
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to compile Installer.cs"
    }
    Write-Host "Setup package compiled: $installerOut" -ForegroundColor Yellow

    $desktopPath = [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::DesktopDirectory)
    $desktopOut = Join-Path $desktopPath "MediCareSetup.exe"
    Copy-Item $installerOut $desktopOut -Force
    Write-Host "Setup copied to Desktop: $desktopOut" -ForegroundColor Yellow
} else {
    Write-Host "Skipping setup EXE build (CSC or Installer.cs not found)." -ForegroundColor DarkYellow
}

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "SUCCESS! Use MediCareLauncher.exe to start the app:" -ForegroundColor Green
Write-Host "  - Project root: MediCareLauncher.exe" -ForegroundColor Green
Write-Host "  - Standalone: target\dist\MediCare\MediCare.exe" -ForegroundColor Green
Write-Host "  - Installer: MediCareSetup.exe" -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Cyan
