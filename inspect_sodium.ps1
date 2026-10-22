Add-Type -AssemblyName System.IO.Compression.FileSystem

$jarPath = "D:/multimc/instances/26.3/.minecraft/mods/sodium-fabric-0.9.2+mc26.3.jar"
if (Test-Path $jarPath) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    Write-Host "=== SODIUM OPTIONS CLASSES ==="
    $zip.Entries | Where-Object { $_.FullName -like "*options*" -or $_.FullName -like "*config*" } | Select-Object -First 40 FullName
    $zip.Dispose()
}

$irisPath = "D:/multimc/instances/26.3/.minecraft/mods/iris-fabric-1.11.7+mc26.3.jar"
if (Test-Path $irisPath) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($irisPath)
    Write-Host "=== IRIS OPTIONS CLASSES ==="
    $zip.Entries | Where-Object { $_.FullName -like "*ColorSpace*" -or $_.FullName -like "*options*" -or $_.FullName -like "*config*" -or $_.FullName -like "*IrisConfig*" -or $_.FullName -like "*IrisVideoSettings*" } | Select-Object -First 40 FullName
    $zip.Dispose()
}
