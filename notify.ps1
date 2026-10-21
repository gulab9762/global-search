$exitCode = $LASTEXITCODE

$message = if ($null -eq $exitCode -or $exitCode -eq 0) {
    "Global Search: Smart Setting Modal Popup implemented! Identifies value type (Boolean, Integer, Decimal, Choice, Text) with type badges, dual TRUE/FALSE buttons, and a draggable slider bar alongside direct number input for FOV and numeric settings!"
} else {
    "Global Search: Build failed with exit code $exitCode"
}

Invoke-RestMethod -Method Post `
  -Uri "https://ntfy.sh/antigravity" `
  -Headers @{ Title = "Global Search Mod"; Priority = "4"; Tags = "compass,mag,sparkles,crossed_swords" } `
  -Body $message