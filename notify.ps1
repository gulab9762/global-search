$exitCode = $LASTEXITCODE

$message = if ($null -eq $exitCode -or $exitCode -eq 0) {
    "Global Search: Bottom helper text, keybind listening prompt, and expanded details now dynamically follow the active theme colors!"
} else {
    "Global Search: Build failed with exit code $exitCode"
}

Invoke-RestMethod -Method Post `
  -Uri "https://ntfy.sh/antigravity" `
  -Headers @{ Title = "Global Search Mod"; Priority = "4"; Tags = "compass,mag,sparkles,crossed_swords" } `
  -Body $message