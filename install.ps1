# YotubeNoAd Windows kurulumu (Chrome, Edge, Brave, Firefox)
# Tek satirla:  irm https://raw.githubusercontent.com/rextroanticheat/YotubeNoAd/main/install.ps1 | iex
$ErrorActionPreference = "Stop"

$zipUrl = "https://github.com/rextroanticheat/YotubeNoAd/archive/refs/heads/main.zip"
$dest   = Join-Path $env:LOCALAPPDATA "YotubeNoAd"
$files  = @("manifest.json","background.js","content.js","inject.js","popup.html","popup.js","rules.json","style.css")

# 1) Kaynak: betik klasoru (indirilmis proje) ya da GitHub'dan indir
$src = $null
if ($PSScriptRoot -and (Test-Path (Join-Path $PSScriptRoot "manifest.json"))) {
    $src = $PSScriptRoot
} else {
    Write-Host "GitHub'dan indiriliyor..."
    $tmp = Join-Path $env:TEMP ("YotubeNoAd_" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $tmp | Out-Null
    $zip = Join-Path $tmp "src.zip"
    Invoke-WebRequest -Uri $zipUrl -OutFile $zip -UseBasicParsing
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $src = Join-Path $tmp "YotubeNoAd-main"
}

# 2) Kalici klasore kopyala
New-Item -ItemType Directory -Path $dest -Force | Out-Null
foreach ($f in $files) { Copy-Item (Join-Path $src $f) (Join-Path $dest $f) -Force }
Set-Clipboard -Value $dest
Write-Host ""
Write-Host "Eklenti dosyalari: $dest"
Write-Host "(Yol panoya kopyalandi, klasor secerken Ctrl+V ile yapistirabilirsin.)"
Write-Host ""

# 3) Bulunan tarayicilarin eklenti sayfasini ac
$pf  = $env:ProgramFiles
$pf86 = ${env:ProgramFiles(x86)}
$lad = $env:LOCALAPPDATA
$browsers = @(
    @{ Name="Chrome";  Url="chrome://extensions"; Paths=@("$pf\Google\Chrome\Application\chrome.exe","$pf86\Google\Chrome\Application\chrome.exe","$lad\Google\Chrome\Application\chrome.exe") },
    @{ Name="Edge";    Url="edge://extensions";   Paths=@("$pf86\Microsoft\Edge\Application\msedge.exe","$pf\Microsoft\Edge\Application\msedge.exe") },
    @{ Name="Brave";   Url="brave://extensions";  Paths=@("$pf\BraveSoftware\Brave-Browser\Application\brave.exe","$pf86\BraveSoftware\Brave-Browser\Application\brave.exe","$lad\BraveSoftware\Brave-Browser\Application\brave.exe") },
    @{ Name="Firefox"; Url="about:debugging#/runtime/this-firefox"; Paths=@("$pf\Mozilla Firefox\firefox.exe","$pf86\Mozilla Firefox\firefox.exe") }
)

$found = $false
foreach ($b in $browsers) {
    $exe = $b.Paths | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $exe) { continue }
    $found = $true
    Write-Host "-> $($b.Name) aciliyor..."
    Start-Process -FilePath $exe -ArgumentList $b.Url
}

Write-Host ""
if (-not $found) {
    Write-Host "Desteklenen tarayici bulunamadi. Chrome, Edge, Brave veya Firefox kurup tekrar calistirin."
    return
}
Write-Host "Acilan eklenti sayfasinda:"
Write-Host "  Chrome / Edge / Brave : Gelistirici modu'nu ac > 'Paketlenmemis oge yukle' > yolu yapistir > Klasor sec"
Write-Host "  Firefox               : 'Gecici Eklenti Yukle' > $dest\manifest.json dosyasini sec (Firefox kapaninca silinir)"
