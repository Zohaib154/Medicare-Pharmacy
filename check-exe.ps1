$proc = Start-Process -FilePath "C:\Users\User\Desktop\pharmacy-system\target\dist\MediCare\MediCare.exe" -PassThru
Start-Sleep -Seconds 5
Write-Host "Process ID: $($proc.Id)"
Write-Host "Process HasExited: $($proc.HasExited)"
if ($proc.HasExited) {
    Write-Host "Exit Code: $($proc.ExitCode)"
}
