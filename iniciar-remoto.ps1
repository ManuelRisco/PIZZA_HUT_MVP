param(
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $RootDir 'Backend\demo'
$FrontendDir = Join-Path $RootDir 'Frontend'
$DistIndex = Join-Path $FrontendDir 'dist\pizzahut\browser\index.html'
$ProxyScript = Join-Path $RootDir 'scripts\cloudflare-proxy.mjs'
$ProxyPort = 4205
$BackendPort = 8089

function Find-Cloudflared {
    $cmd = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        'C:\Program Files (x86)\cloudflared\cloudflared.exe',
        'C:\Program Files\cloudflared\cloudflared.exe'
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

function Test-BackendReady {
    try {
        $code = & curl.exe -s -o NUL -w '%{http_code}' --max-time 2 "http://127.0.0.1:$BackendPort/api/payments/mercadopago/config"
        return ($code -eq '200')
    } catch {
        return $false
    }
}

$CloudflaredExe = Find-Cloudflared
if (-not $CloudflaredExe) {
    Write-Host '[ERROR] No se encontro cloudflared.exe instalado.' -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host '   PIZZA HUT MVP - INICIO REMOTO CON CLOUDFLARE (HTTPS)     ' -ForegroundColor Cyan
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host ''

# 1. Verificar o iniciar Backend Spring Boot (puerto 8089)
if (Test-BackendReady) {
    Write-Host "[1/4] Backend Spring Boot ya esta corriendo en :$BackendPort (OK)" -ForegroundColor Green
} else {
    Write-Host "[1/4] Iniciando Backend Spring Boot en ventana aparte (:$BackendPort)..." -ForegroundColor Yellow
    $backendCmd = "Set-Location '$BackendDir'; .\gradlew.bat bootRun"
    Start-Process powershell -ArgumentList '-NoExit', '-NoProfile', '-Command', $backendCmd | Out-Null

    Write-Host '      Esperando a que Spring Boot termine de arrancar...' -ForegroundColor DarkGray
    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        Start-Sleep -Seconds 2
        if (Test-BackendReady) {
            $ready = $true
            break
        }
    }
    if (-not $ready) {
        Write-Host '[ERROR] Spring Boot no respondio en el puerto 8089 a tiempo. Revisa su ventana.' -ForegroundColor Red
        exit 1
    }
    Write-Host "      Backend listo en http://127.0.0.1:$BackendPort (OK)" -ForegroundColor Green
}

# 2. Compilar Frontend (salvo que se pase -NoBuild y ya exista dist)
if ($NoBuild -and (Test-Path $DistIndex)) {
    Write-Host '[2/4] Usando build existente de Frontend (-NoBuild).' -ForegroundColor Green
} else {
    Write-Host '[2/4] Compilando Frontend Angular (tarda ~10 segundos)...' -ForegroundColor Yellow
    Push-Location $FrontendDir
    try {
        & npx ng build --configuration development
        if ($LASTEXITCODE -ne 0) {
            throw 'Error al compilar Frontend Angular.'
        }
    } finally {
        Pop-Location
    }
    Write-Host '      Frontend compilado correctamente (OK)' -ForegroundColor Green
}

# 3. Liberar puerto 4205 si hubiera un proxy anterior y levantar el proxy local
Write-Host "[3/4] Iniciando proxy unificado en http://127.0.0.1:$ProxyPort..." -ForegroundColor Yellow
$oldPids = (netstat -ano | Select-String ":$ProxyPort\s+.*LISTENING") | ForEach-Object {
    ($_ -split '\s+')[-1]
} | Select-Object -Unique
foreach ($p in $oldPids) {
    if ($p -match '^\d+$') {
        Stop-Process -Id ([int]$p) -Force -ErrorAction SilentlyContinue
    }
}

$proxyProc = Start-Process -FilePath 'node' -ArgumentList "`"$ProxyScript`"" -NoNewWindow -PassThru
Start-Sleep -Seconds 1
if ($proxyProc.HasExited) {
    Write-Host '[ERROR] No se pudo iniciar el proxy Node en el puerto 4205.' -ForegroundColor Red
    exit 1
}
Write-Host "      Proxy activo en http://127.0.0.1:$ProxyPort -> Backend :$BackendPort (OK)" -ForegroundColor Green

# 4. Levantar Cloudflare Quick Tunnel y capturar la URL publica
Write-Host '[4/4] Abriendo tunel HTTPS en Cloudflare...' -ForegroundColor Yellow
Write-Host ''

$cfLog = Join-Path $env:TEMP "pizzahut_cloudflared_$([Guid]::NewGuid().ToString('N').Substring(0,8)).log"
$cfErrLog = "$cfLog.err"

try {
    $cfProc = Start-Process -FilePath $CloudflaredExe `
        -ArgumentList 'tunnel', '--url', "http://127.0.0.1:$ProxyPort", '--no-autoupdate' `
        -RedirectStandardOutput $cfLog `
        -RedirectStandardError $cfErrLog `
        -NoNewWindow -PassThru

    $publicUrl = $null
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Milliseconds 500
        $combined = ''
        if (Test-Path $cfLog) { $combined += (Get-Content $cfLog -Raw -ErrorAction SilentlyContinue) }
        if (Test-Path $cfErrLog) { $combined += (Get-Content $cfErrLog -Raw -ErrorAction SilentlyContinue) }
        if ($combined -match 'https://[a-z0-9-]+\.trycloudflare\.com') {
            $publicUrl = $Matches[0]
            break
        }
        if ($cfProc.HasExited) { break }
    }

    if ($publicUrl) {
        try { Set-Clipboard -Value $publicUrl } catch {}
        Write-Host '============================================================' -ForegroundColor Green
        Write-Host '   TU APLICACION YA ESTA EN LINEA (COPIADA AL PORTAPAPELES) ' -ForegroundColor Green
        Write-Host '' -ForegroundColor Green
        Write-Host "   URL: $publicUrl" -ForegroundColor White
        Write-Host '' -ForegroundColor Green
        Write-Host '   Abre ese enlace desde tu laptop del trabajo o celular.   ' -ForegroundColor Green
        Write-Host '   Presiona Ctrl + C aqui cuando quieras cerrar el tunel.   ' -ForegroundColor Yellow
        Write-Host '============================================================' -ForegroundColor Green
        Write-Host ''
        $cfProc.WaitForExit()
    } else {
        Write-Host '[ERROR] No se pudo obtener la URL de Cloudflare. Log:' -ForegroundColor Red
        if (Test-Path $cfErrLog) { Get-Content $cfErrLog | Select-Object -Last 20 }
    }
} finally {
    Write-Host ''
    Write-Host 'Cerrando tunel Cloudflare y proxy local...' -ForegroundColor DarkYellow
    if ($cfProc -and -not $cfProc.HasExited) { Stop-Process -Id $cfProc.Id -Force -ErrorAction SilentlyContinue }
    if ($proxyProc -and -not $proxyProc.HasExited) { Stop-Process -Id $proxyProc.Id -Force -ErrorAction SilentlyContinue }
    Remove-Item $cfLog, $cfErrLog -Force -ErrorAction SilentlyContinue
}
