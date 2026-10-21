$exitCode = $LASTEXITCODE

$message = if ($null -eq $exitCode -or $exitCode -eq 0) {
    "Global Search: Value input box vertically centered with clean numeric formatting, slider bounds moved safely above track, and value type badge now dynamically adopts the active theme colors!"
} else {
    "Global Search: Build failed with exit code $exitCode"
}

Invoke-RestMethod -Method Post `
  -Uri "https://ntfy.sh/antigravity" `
  -Headers @{ Title = "Global Search Mod"; Priority = "4"; Tags = "compass,mag,sparkles,crossed_swords" } `
  -Body $message