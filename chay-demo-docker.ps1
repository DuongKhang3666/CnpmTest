# Chay demo bang Docker: MySQL + Backend chay trong Docker, giao dien web chay bang npm.
# Cach dung:  bam doi vao chay-demo-docker.bat
#         hoac: powershell -ExecutionPolicy Bypass -File .\chay-demo-docker.ps1
# Tuy chon:
#   -Translate   bat them dich may LibreTranslate (lan dau tai mo hinh ngon ngu, mat vai phut)
#   -Stop        tat MySQL + Backend (du lieu van con; them 'down -v' bang tay neu muon xoa du lieu)
# Yeu cau: Docker Desktop (dang chay) va Node.js LTS.
param(
    [switch]$Translate,
    [switch]$Stop
)

Set-Location -LiteralPath $PSScriptRoot
$ErrorActionPreference = 'Continue'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Exit-Script([int]$code) {
    Write-Host ''
    Read-Host 'Nhan Enter de dong cua so nay' | Out-Null
    exit $code
}

function Fail([string]$message) {
    Write-Host "[LOI] $message" -ForegroundColor Red
    Exit-Script 1
}

function New-RandomText([int]$length) {
    $chars = 'abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    $bytes = New-Object byte[] $length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $sb = New-Object System.Text.StringBuilder
    foreach ($b in $bytes) { [void]$sb.Append($chars[$b % $chars.Length]) }
    return $sb.ToString()
}

# Dat KEY=value trong mang cac dong cua file .env (them moi neu chua co)
function Set-EnvValue([string[]]$lines, [string]$key, [string]$value) {
    $found = $false
    $result = New-Object System.Collections.Generic.List[string]
    foreach ($line in $lines) {
        if ($line -match ('^\s*' + [regex]::Escape($key) + '=')) {
            $result.Add($key + '=' + $value)
            $found = $true
        } else {
            $result.Add($line)
        }
    }
    if (-not $found) { $result.Add($key + '=' + $value) }
    return , $result.ToArray()
}

# Dia chi IPv4 mang noi bo (Wi-Fi/LAN) cua may nay, de QR dung duoc tren dien thoai
function Get-LanIp {
    try {
        $item = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop |
            Where-Object {
                $_.IPAddress -match '^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[01])\.)' -and
                $_.InterfaceAlias -notmatch 'vEthernet|WSL|VirtualBox|VMware|Loopback|Bluetooth'
            } | Select-Object -First 1
        if ($item) { return $item.IPAddress }
    } catch { }
    return $null
}

function Test-Url([string]$url, [int]$timeoutSec) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $timeoutSec | Out-Null
        return $true
    } catch {
        return $false
    }
}

Write-Host '=============================================================='
Write-Host ' He thong thuyet minh da ngon ngu - chay demo bang Docker'
Write-Host '=============================================================='
Write-Host ''

# ---- 1. Kiem tra cong cu ---------------------------------------------------
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Fail 'Khong tim thay Docker. Cai Docker Desktop (https://www.docker.com/products/docker-desktop) roi chay lai.'
}
& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Fail 'Docker chua chay. Mo Docker Desktop, doi den khi bao "Engine running", roi chay lai.'
}
& docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Fail 'Docker Compose chua co. Cap nhat Docker Desktop len ban moi.'
}

if ($Stop) {
    & docker compose --profile translate down
    Write-Host 'Da tat MySQL + Backend. Dong cua so "Web 5173" (neu con mo).'
    Exit-Script $LASTEXITCODE
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Fail 'Khong tim thay Node.js (npm). Cai Node.js LTS (https://nodejs.org) roi chay lai.'
}

# ---- 2. File cau hinh .env -------------------------------------------------
$envFile = Join-Path $PSScriptRoot '.env'
$example = Join-Path $PSScriptRoot '.env.example'
$adminEmail = 'admin@gmail.com'
$adminPassword = ''
$backendPort = '3000'
$lanIp = Get-LanIp

if (-not (Test-Path -LiteralPath $envFile)) {
    if (-not (Test-Path -LiteralPath $example)) { Fail 'Thieu file .env.example.' }
    Write-Host 'Chua co file .env: tao tu .env.example voi mat khau quan tri ngau nhien...'
    $lines = [System.IO.File]::ReadAllLines($example, [System.Text.Encoding]::UTF8)
    $adminPassword = New-RandomText 12
    $lines = Set-EnvValue $lines 'ADMIN_PASSWORD' $adminPassword
    $lines = Set-EnvValue $lines 'ADMIN_TOKEN_SECRET' (New-RandomText 32)
    $lines = Set-EnvValue $lines 'TRANSLATE_ENABLED' $(if ($Translate) { 'true' } else { 'false' })
    if ($lanIp) { $lines = Set-EnvValue $lines 'PUBLIC_HOST' $lanIp }
    [System.IO.File]::WriteAllLines($envFile, $lines, $utf8NoBom)
} else {
    Write-Host 'Dung file .env co san.'
    foreach ($line in [System.IO.File]::ReadAllLines($envFile, [System.Text.Encoding]::UTF8)) {
        if ($line -match '^\s*ADMIN_EMAIL=(.*)$') { $adminEmail = $Matches[1].Trim() }
        if ($line -match '^\s*ADMIN_PASSWORD=(.*)$') { $adminPassword = $Matches[1].Trim() }
        if ($line -match '^\s*BACKEND_PORT=(\d+)\s*$') { $backendPort = $Matches[1] }
    }
}

# Dich may chi co khi chay kem dich vu LibreTranslate (tuy chon -Translate); ban dich tay van dung duoc
$env:TRANSLATE_ENABLED = $(if ($Translate) { 'true' } else { 'false' })

# ---- 3. Dung MySQL + Backend bang Docker ----------------------------------
Write-Host ''
Write-Host 'Dang dung MySQL + Backend bang Docker (lan dau mat vai phut de tai image va build)...'
if ($Translate) {
    & docker compose --profile translate up -d --build
} else {
    & docker compose up -d --build
}
if ($LASTEXITCODE -ne 0) {
    Fail 'docker compose that bai. Xem thong bao o tren (cong 3000 hoac 3307 co the dang bi chuong trinh khac chiem).'
}

Write-Host ''
Write-Host 'Dang cho Backend san sang (toi da khoang 3 phut)...'
$healthUrl = 'http://localhost:' + $backendPort + '/api/user/health'
$ready = $false
for ($i = 0; $i -lt 90; $i++) {
    if (Test-Url $healthUrl 3) { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) { Fail 'Backend chua len. Xem log bang lenh: docker compose logs backend' }

# ---- 4. Giao dien web (Vite, cong 5173) ------------------------------------
$web = Join-Path $PSScriptRoot 'Frontend'
if (-not (Test-Path -LiteralPath (Join-Path $web 'node_modules'))) {
    Write-Host 'Cai thu vien giao dien lan dau (npm install)...'
    Push-Location -LiteralPath $web
    & npm install
    $npmCode = $LASTEXITCODE
    Pop-Location
    if ($npmCode -ne 0) { Fail 'npm install that bai.' }
}
if (-not (Test-Url 'http://localhost:5173/' 2)) {
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', 'title Web 5173 && npm run dev' -WorkingDirectory $web
    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Url 'http://localhost:5173/' 2) { break }
        Start-Sleep -Seconds 2
    }
}

# ---- 5. Mo trinh duyet -----------------------------------------------------
# Mo bang dia chi LAN (neu co) de ma QR trong trang quan tri dung duoc tren dien thoai.
$host5173 = $(if ($lanIp) { $lanIp } else { 'localhost' })
$adminUrl = 'http://' + $host5173 + ':5173/'
Start-Process $adminUrl

Write-Host ''
Write-Host '=============================================================='
Write-Host ' Da san sang.'
Write-Host ('  - Trang quan tri : ' + $adminUrl)
Write-Host ('  - Dang nhap      : ' + $adminEmail)
if ($adminPassword) {
    Write-Host ('  - Mat khau       : ' + $adminPassword + '   (luu trong file .env, khong dua len GitHub)')
} else {
    Write-Host '  - Mat khau       : .env de trong ADMIN_PASSWORD -> xem log: docker compose logs backend'
}
Write-Host '  - Vao "Mo QR dang nhap" de lay QR cho dien thoai (cung Wi-Fi).'
Write-Host '    Neu dien thoai khong vao duoc: chay mo-firewall-lan.ps1 bang quyen Administrator.'
Write-Host '  - Tat he thong   : dong cua so "Web 5173", roi chay chay-demo-docker.bat -Stop'
Write-Host '=============================================================='
Exit-Script 0
