$exitCode = $LASTEXITCODE

$message = if ($null -eq $exitCode -or $exitCode -eq 0) {
    "Global Search: Fixed setting deduplication key in SearchIndexer so all 500+ settings are fully preserved when editing or rescanning, made Xaero config saving resilient, and updated JARs in Downloads and MultiMC!"
} else {
    "Global Search: Build failed with exit code $exitCode"
}

Invoke-RestMethod -Method Post `
  -Uri "https://ntfy.sh/antigravity" `
  -Headers @{ Title = "Global Search Mod"; Priority = "4"; Tags = "compass,mag,sparkles,crossed_swords" } `
  -Body $message