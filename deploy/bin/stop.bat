@echo off
rem Stop the console-launched PES process (matches pes-app.jar in command line).
rem For the WinSW service use: pes-app.exe stop
setlocal
echo Stopping PES (pes-app.jar) ...
powershell -NoProfile -Command ^
  "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*pes-app.jar*' } | ForEach-Object { Write-Host ('Killing PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }"
endlocal
