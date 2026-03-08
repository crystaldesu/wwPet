$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Set-Location -Path $PSScriptRoot

function Stop-PackagedAppProcesses {
    param(
        [string]$AppName,
        [string]$AppDir
    )

    $normalizedAppDir = [System.IO.Path]::GetFullPath($AppDir).TrimEnd('\').ToLowerInvariant()
    $processes = Get-CimInstance Win32_Process -Filter "name = '$AppName.exe' or name = 'java.exe' or name = 'javaw.exe'"
    foreach ($process in $processes) {
        $candidatePaths = @()
        if ($process.ExecutablePath) {
            $candidatePaths += $process.ExecutablePath
        }
        if ($process.CommandLine) {
            $candidatePaths += $process.CommandLine
        }

        $matchesAppDir = $false
        foreach ($candidatePath in $candidatePaths) {
            if ($candidatePath -and $candidatePath.ToLowerInvariant().Contains($normalizedAppDir)) {
                $matchesAppDir = $true
                break
            }
        }

        if ($matchesAppDir) {
            Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }

    Start-Sleep -Milliseconds 300
}

function Remove-DirectoryIfExists {
    param(
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        return
    }

    Remove-Item $Path -Recurse -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 300

    if (Test-Path $Path) {
        throw "Could not replace existing package output at: $Path . Close the running packaged app and try again."
    }
}

$jpackageExe = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\\jpackage.exe"))) {
    $jpackageExe = Join-Path $env:JAVA_HOME "bin\\jpackage.exe"
}
if (-not $jpackageExe) {
    $defaultJpackage = "C:\\Program Files\\Java\\jdk-17\\bin\\jpackage.exe"
    if (Test-Path $defaultJpackage) {
        $jpackageExe = $defaultJpackage
    }
}
if (-not $jpackageExe) {
    throw "jpackage.exe was not found. Install JDK 17 or set JAVA_HOME."
}

$jarExe = Join-Path (Split-Path $jpackageExe) "jar.exe"
$buildDir = Join-Path $PSScriptRoot "build"
$classesDir = Join-Path $buildDir "classes"
$inputDir = Join-Path $buildDir "jpackage-input"
$stagingDir = Join-Path $buildDir "jpackage-output"
$distDir = Join-Path $PSScriptRoot "dist"
$appName = "wwPet"
$appVersion = "1.0.0"
$iconFile = Join-Path $PSScriptRoot "data\\assets\\icon.ico"
$zipFile = Join-Path $distDir "$appName-windows-x64.zip"
$finalAppDir = Join-Path $distDir $appName

Remove-Item $buildDir -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $zipFile -Force -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

$sourceFiles = Get-ChildItem -Path (Join-Path $PSScriptRoot "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sourceFiles) {
    throw "No Java source files were found."
}

& javac -encoding UTF-8 -d $classesDir $sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed."
}

$jarFile = Join-Path $inputDir "$appName.jar"
& $jarExe --create --file $jarFile --main-class com.wwpet.Main -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "JAR packaging failed."
}

Copy-Item -Path (Join-Path $PSScriptRoot "data") -Destination (Join-Path $inputDir "data") -Recurse -Force

$jpackageArgs = @(
    "--type", "app-image",
    "--name", $appName,
    "--dest", $stagingDir,
    "--input", $inputDir,
    "--main-jar", "$appName.jar",
    "--main-class", "com.wwpet.Main",
    "--app-version", $appVersion,
    "--vendor", "Rosei",
    "--description", "wwPet desktop pet",
    "--java-options", "-Dfile.encoding=UTF-8"
)
if (Test-Path $iconFile) {
    $jpackageArgs += @("--icon", $iconFile)
}

& $jpackageExe @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "EXE packaging failed."
}

Stop-PackagedAppProcesses -AppName $appName -AppDir $finalAppDir
Remove-DirectoryIfExists -Path $finalAppDir

$stagedAppDir = Join-Path $stagingDir $appName
if (-not (Test-Path $stagedAppDir)) {
    throw "Packaged app image was not generated."
}

Move-Item -Path $stagedAppDir -Destination $finalAppDir -Force
Compress-Archive -Path $finalAppDir -DestinationPath $zipFile -Force

$exePath = Join-Path $distDir "$appName\\$appName.exe"
Write-Host ""
Write-Host "Package ready: $exePath"
Write-Host "Release zip : $zipFile"
