# Script to remove BOM and fix package declarations in all Java files
param(
    [string]$path = "C:\Users\User\Desktop\pharmacy-system"
)

$files = @()
Get-ChildItem -Path $path -Recurse -Filter "*.java" | ForEach-Object {
    $files += $_
}

Write-Host "Processing $($files.Count) Java files..."

$bomRemoved = 0
$packagesFixed = 0

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw
    $modified = $false

    # Fix corrupted package declarations (package ackage, package kage, etc)
    if ($content -match 'package \w+age ') {
        $content = $content -replace 'package \w+age ', 'package '
        $packagesFixed++
        $modified = $true
    }

    # Remove BOM if present
    $rawBytes = [System.IO.File]::ReadAllBytes($file.FullName)
    if ($rawBytes.Length -gt 3 -and $rawBytes[0] -eq 0xEF -and $rawBytes[1] -eq 0xBB -and $rawBytes[2] -eq 0xBF) {
        $modified = $true
        $bomRemoved++
    }

    if ($modified) {
        # Write back as UTF8 without BOM
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        Write-Host "Fixed: $($file.Name)"
    }
}

Write-Host "BOM removed from $bomRemoved files"
Write-Host "Package declarations fixed in $packagesFixed files"
Write-Host 'Complete!'
