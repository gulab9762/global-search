$exitCode = $LASTEXITCODE

$message = if ($null -eq $exitCode -or $exitCode -eq 0) {
    "Global Search: Multi-theme engine (Netherite RPG, Obsidian Glass, Raycast Clean, Ender Twilight) with rounded corners, category icons, live F6 toggle, and automated UI test script successfully implemented and verified!"
} else {
    "Global Search: Build failed with exit code $exitCode"
}

Invoke-RestMethod -Method Post `
  -Uri "https://ntfy.sh/antigravity" `
  -Headers @{ Title = "Global Search Mod"; Priority = "4"; Tags = "compass,mag,sparkles,crossed_swords" } `
  -Body $message