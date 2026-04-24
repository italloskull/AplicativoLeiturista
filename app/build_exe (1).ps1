# ================================================================
#  Mathools Builder + Installer (PyInstaller onedir + Inno Setup)
# ================================================================

Write-Host "================================" -ForegroundColor Cyan
Write-Host " Mathools Builder + Installer" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 1) Ativar .venv automaticamente
if ($null -eq $env:VIRTUAL_ENV) {
    Write-Host "Ambiente virtual nao detectado. Ativando automaticamente..." -ForegroundColor Yellow
    if (Test-Path ".venv\Scripts\Activate.ps1") {
        . ".venv\Scripts\Activate.ps1"
        Write-Host "Ambiente ativado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "ERRO: Pasta '.venv' nao encontrada." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Ambiente Python: $($env:VIRTUAL_ENV)" -ForegroundColor Green
}
Write-Host ""

# 1.1) Bloquear Python da Store
$pyExe = (python -c "import sys; print(sys.executable)" 2>$null).Trim()
$pyBase = (python -c "import sys; print(sys.base_prefix)" 2>$null).Trim()
if ($pyExe -match "WindowsApps" -or $pyBase -match "WindowsApps") {
    Write-Host "ERRO: Python da Microsoft Store detectado (WindowsApps)." -ForegroundColor Red
    Write-Host "Use Python instalado via python.org e recrie a .venv." -ForegroundColor Yellow
    Write-Host "Python atual: $pyExe" -ForegroundColor DarkGray
    exit 1
}
Write-Host "Python runtime check: OK ($pyExe)" -ForegroundColor Green
Write-Host ""

# 2) Modo
Write-Host "O que voce deseja fazer?" -ForegroundColor Yellow
Write-Host "[ 1 ] Build local (sem publicar)"
Write-Host "[ 2 ] Build + publicar release no GitHub"
Write-Host "[ 3 ] Build de teste + enviar para branch test-builds"
Write-Host ""
$choice = Read-Host "Digite 1, 2 ou 3"

$Release = $false
$TestBuild = $false
$Version = ""
$Token = ""
$Notes = ""
$REPO = "MatheyPY/Mathools"

if ($choice -eq "2") {
    $Release = $true
    Write-Host ""
    Write-Host "--- CONFIGURANDO RELEASE ---" -ForegroundColor Cyan
    $Token = Read-Host "Cole o seu Token do GitHub (ghp_...)"
    if ([string]::IsNullOrWhiteSpace($Token)) {
        Write-Host "ERRO: Token obrigatorio para publicar." -ForegroundColor Red
        exit 1
    }

    $headers = @{
        "Authorization" = "Bearer $Token"
        "Accept" = "application/vnd.github+json"
        "X-GitHub-Api-Version" = "2022-11-28"
    }

    $validVersion = $false
    while (-not $validVersion) {
        $Version = Read-Host "Digite a NOVA versao (ex: 1.9.0)"
        if ([string]::IsNullOrWhiteSpace($Version)) {
            Write-Host "ERRO: Versao nao pode ficar em branco." -ForegroundColor Red
            exit 1
        }
        $TAG = "v$Version"
        try {
            Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/tags/$TAG" -Headers $headers -Method GET -ErrorAction Stop | Out-Null
            Write-Host "AVISO: A versao $TAG ja existe. Digite outra." -ForegroundColor Yellow
        } catch {
            if ($_.Exception.Message -match "404") {
                $validVersion = $true
                Write-Host "Versao $TAG liberada para uso." -ForegroundColor Green
            } else {
                Write-Host "ERRO ao validar versao/tag: $_" -ForegroundColor Red
                exit 1
            }
        }
    }

    $Notes = Read-Host "Digite as novidades dessa versao"
    if ([string]::IsNullOrWhiteSpace($Notes)) {
        $Notes = "Atualizacao $Version"
    }
} elseif ($choice -eq "3") {
    $TestBuild = $true
    Write-Host ""
    Write-Host "--- CONFIGURANDO BUILD DE TESTE ---" -ForegroundColor Cyan
    $Token = Read-Host "Cole o seu Token do GitHub (ghp_...)"
    if ([string]::IsNullOrWhiteSpace($Token)) {
        Write-Host "ERRO: Token obrigatorio para enviar." -ForegroundColor Red
        exit 1
    }
    $headers = @{
        "Authorization"        = "Bearer $Token"
        "Accept"               = "application/vnd.github+json"
        "X-GitHub-Api-Version" = "2022-11-28"
    }
} elseif ($choice -ne "1") {
    Write-Host "Opcao invalida." -ForegroundColor Red
    exit 1
}

Write-Host ""

# 3) Verificacoes basicas
$required = @("launcher_gui.py", "LOGIN_GUI.py", "updater.py", "MathoolsInstaller.iss", "Logo-mathey-tk-3.ico")
$missing = @()
foreach ($f in $required) {
    if (-not (Test-Path $f)) { $missing += $f }
}
if ($missing.Count -gt 0) {
    Write-Host "ERRO: Arquivos obrigatorios ausentes:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}

# Arquivos de runtime que impactam funcionalidades centrais
$runtimeRequired = @(
    "config.toml",
    "cURL 8091.txt",
    "cURL 8117.txt",
    "cURL 8121.txt",
    "cURL 50012.txt"
)
$runtimeMissing = @()
foreach ($f in $runtimeRequired) {
    if (-not (Test-Path $f)) { $runtimeMissing += $f }
}
if ($runtimeMissing.Count -gt 0) {
    Write-Host "ERRO: Arquivos de runtime ausentes (funcionalidades vao falhar):" -ForegroundColor Red
    $runtimeMissing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}

$updaterContent = Get-Content "updater.py" -Raw
$versionMatch = [regex]::Match($updaterContent, 'CURRENT_VERSION\s*=\s*"([^"]+)"')
$currentVersion = if ($versionMatch.Success) { $versionMatch.Groups[1].Value } else { "0.0.0" }
$appVersion = if ($Release) { $Version } else { $currentVersion }

if ($Release) {
    Write-Host "Atualizando CURRENT_VERSION para $Version no updater.py..." -ForegroundColor Cyan
    $updaterNew = $updaterContent -replace 'CURRENT_VERSION\s*=\s*"[^"]+"', "CURRENT_VERSION = `"$Version`""
    Set-Content "updater.py" $updaterNew -Encoding UTF8
}

# 4) Dependencias
Write-Host "Atualizando dependencias..." -ForegroundColor Cyan
python -m pip install --upgrade pip -q
if (Test-Path "requirements.txt") {
    python -m pip install --quiet -r "requirements.txt"
} else {
    Write-Host "AVISO: requirements.txt nao encontrado. Instalando apenas dependencias minimas do build." -ForegroundColor Yellow
}
python -m pip install --quiet pyinstaller==6.19.0 pyinstaller-hooks-contrib requests==2.32.5 selenium webdriver-manager
Write-Host "Dependencias: OK" -ForegroundColor Green
Write-Host ""

# 5) Limpeza
Write-Host "Limpando builds anteriores..." -ForegroundColor Cyan
$processes = Get-Process -Name Mathools -ErrorAction SilentlyContinue
if ($processes) {
    Write-Host "Processo Mathools em execucao detectado. Finalizando..." -ForegroundColor Yellow
    $processes | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

function Remove-PathIfExists($path) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
    }
}

$maxRetries = 3
for ($i = 1; $i -le $maxRetries; $i++) {
    Remove-PathIfExists "build"
    Remove-PathIfExists "dist"
    Get-ChildItem -Path . -Filter "*.spec" -File | Remove-Item -Force -ErrorAction SilentlyContinue
    if (-not (Test-Path "build") -and -not (Test-Path "dist") -and -not (Get-ChildItem -Path . -Filter "*.spec" -File)) {
        break
    }
    Start-Sleep -Seconds 1
}

if (Test-Path "build" -or Test-Path "dist" -or (Get-ChildItem -Path . -Filter "*.spec" -File)) {
    Write-Host "ERRO: Nao foi possivel limpar builds anteriores. Feche Mathools/explorador de arquivos e tente novamente." -ForegroundColor Red
    exit 1
}
Write-Host "Limpeza: OK" -ForegroundColor Green
Write-Host ""

# 6) Build PyInstaller (onedir)
if (-not (Test-Path "hooks")) {
    New-Item -ItemType Directory -Path "hooks" | Out-Null
}

$addData = @(
    "--add-data=Logo-mathey-tk-3.ico;.",
    "--add-data=config.toml;.",
    "--add-data=`"cURL 8091.txt`";.",
    "--add-data=`"cURL 8117.txt`";.",
    "--add-data=`"cURL 8121.txt`";.",
    "--add-data=`"cURL 50012.txt`";.",
    "--add-data=payload_os_pesquisar.txt;.",
    "--add-data=payload_os_load.txt;.",
    "--add-data=payload_os_imprimir.txt;."
)
if (Test-Path "stats.json") { $addData += "--add-data=stats.json;." }
if (Test-Path "imagem a direita do loguin.png") { $addData += "--add-data=`"imagem a direita do loguin.png`";." }
if (Test-Path "imagem_a_direita_do_loguin.png") { $addData += "--add-data=imagem_a_direita_do_loguin.png;." }
if (Test-Path "imagem a direita do loguin.jpg") { $addData += "--add-data=`"imagem a direita do loguin.jpg`";." }
if (Test-Path "imagem_a_direita_do_loguin.jpg") { $addData += "--add-data=imagem_a_direita_do_loguin.jpg;." }
if (Test-Path "Itapoa_branca.png") { $addData += "--add-data=Itapoa_branca.png;." }
if (Test-Path "Logo mathey tk 1.png") { $addData += "--add-data=`"Logo mathey tk 1.png`";." }
if (Test-Path "itapoa_informa.png") { $addData += "--add-data=itapoa_informa.png;." }
if (Test-Path "email_templates") { $addData += "--add-data=email_templates;email_templates" }

$hiddenImports = @(
    "--hidden-import=mathtools_1_0",
    "--hidden-import=waterfy_engine",
    "--hidden-import=auth_system",
    "--hidden-import=roteiricacao",
    "--hidden-import=login_gui",
    "--hidden-import=dashboard_html",
    "--hidden-import=updater",
    "--hidden-import=tkinter.filedialog",
    "--hidden-import=tkinter.messagebox",
    "--hidden-import=urllib3.util.retry",
    "--hidden-import=requests.packages.urllib3",
    "--hidden-import=email_merge_module",
    "--hidden-import=fatura_anexo_email",
    "--hidden-import=config_loader",
    "--hidden-import=audit_logger",
    "--hidden-import=retry_utils",
    "--hidden-import=circuit_breaker",
    "--hidden-import=schema_validator",
    "--hidden-import=html_sanitizer",
    "--hidden-import=timezone_utils",
    "--hidden-import=pypdf",
    "--hidden-import=docx_merge_module",
    "--hidden-import=previsao",
    "--hidden-import=pn_generator",
    "--hidden-import=text_utils"
)

$collectAll = @(
    "--collect-all=flet",
    "--collect-all=flet_desktop",
    "--collect-all=pandas",
    "--collect-all=numpy",
    "--collect-all=matplotlib",
    "--collect-all=seaborn",
    "--collect-all=folium",
    "--collect-all=geopy",
    "--collect-all=openpyxl",
    "--collect-all=PIL",
    "--collect-all=cryptography",
    "--collect-all=selenium",
    "--collect-all=webdriver_manager"
)

$pyiArgs = @(
    "--onedir",
    "--noconsole",
    "--icon=Logo-mathey-tk-3.ico",
    "--name=Mathools",
    "--specpath=.",
    "--noupx",
    "--noconfirm",
    "--additional-hooks-dir=hooks"
) + $addData + $hiddenImports + $collectAll + @("launcher_gui.py")

Write-Host "Gerando app (onedir)..." -ForegroundColor Cyan
python -m PyInstaller @pyiArgs

$appExe = "dist\Mathools\Mathools.exe"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $appExe)) {
    Write-Host "ERRO: Falha ao gerar app onedir." -ForegroundColor Red
    exit 1
}
Write-Host "Build app: OK ($appExe)" -ForegroundColor Green

# Smoke test
Write-Host "Validando executavel gerado (smoke test)..." -ForegroundColor Cyan
$smokeScript = @'
import subprocess, time, os, sys

def kill_tree(pid):
    try:
        subprocess.run(
            ["taskkill", "/PID", str(pid), "/T", "/F"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )
    except Exception:
        pass

exe = r"dist\Mathools\Mathools.exe"
if not os.path.exists(exe):
    print("SMOKE_FAIL: exe nao encontrado")
    sys.exit(2)
p = subprocess.Popen([exe], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(6)
if p.poll() is None:
    kill_tree(p.pid)
    time.sleep(2)
    print("SMOKE_OK")
    sys.exit(0)

kill_tree(p.pid)
time.sleep(2)
print(f"SMOKE_FAIL: exit={p.returncode}")
sys.exit(3)
'@
$smokeScript | python -
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: smoke test falhou. Nao publicar este build." -ForegroundColor Red
    exit 1
}
Write-Host "Smoke test: OK" -ForegroundColor Green

# 7) Gerar instalador Inno Setup
$isccCandidates = @(
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "${env:ProgramFiles}\Inno Setup 6\ISCC.exe",
    "${env:LOCALAPPDATA}\Programs\Inno Setup 6\ISCC.exe"
)
$iscc = $isccCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) {
    try {
        $cmd = Get-Command ISCC.exe -ErrorAction Stop
        if ($cmd -and $cmd.Source -and (Test-Path $cmd.Source)) {
            $iscc = $cmd.Source
        }
    } catch {}
}
if (-not $iscc) {
    Write-Host "ERRO: ISCC.exe (Inno Setup) nao encontrado." -ForegroundColor Red
    Write-Host "Instale o Inno Setup 6 para gerar o instalador." -ForegroundColor Yellow
    exit 1
}

Write-Host "Gerando instalador com Inno Setup... ($iscc)" -ForegroundColor Cyan
& $iscc "/DAppVersion=$appVersion" "/DSourceDir=$PWD" "/DOutputDir=$PWD\dist" "MathoolsInstaller.iss" | Out-Null

$installer = "dist\MathoolsSetup.exe"
if (-not (Test-Path $installer)) {
    Write-Host "ERRO: instalador nao foi gerado." -ForegroundColor Red
    exit 1
}
$installerSize = [math]::Round((Get-Item $installer).Length / 1MB, 1)
Write-Host "Instalador: OK ($installerSize MB)" -ForegroundColor Green
Write-Host ""

# ================================================================
# 8) Build local — encerra aqui
# ================================================================
if (-not $Release -and -not $TestBuild) {
    Write-Host "Build local concluido com sucesso." -ForegroundColor Green
    Write-Host "App: $appExe" -ForegroundColor Cyan
    Write-Host "Setup: $installer" -ForegroundColor Cyan
    exit 0
}

# ================================================================
# 9) Modo teste — envia para branch test-builds
# ================================================================
if ($TestBuild) {
    Write-Host "Enviando build de teste para branch test-builds..." -ForegroundColor Cyan

    $testBranch = "test-builds"
    $timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
    Write-Host "Pasta do build: $timestamp" -ForegroundColor DarkGray

    # Garante que a branch existe, criando a partir da main se necessario
    try {
        Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/git/ref/heads/$testBranch" -Headers $headers -Method GET -ErrorAction Stop | Out-Null
        Write-Host "Branch $testBranch ja existe." -ForegroundColor DarkGray
    } catch {
        Write-Host "Criando branch $testBranch..." -ForegroundColor Yellow
        $mainSha = (Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/git/ref/heads/main" -Headers $headers).object.sha
        $createBranch = @{ ref = "refs/heads/$testBranch"; sha = $mainSha } | ConvertTo-Json
        Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/git/refs" -Headers $headers -Method POST -Body $createBranch -ContentType "application/json" | Out-Null
        Write-Host "Branch criada: OK" -ForegroundColor Green
    }

    # Faz upload do MathoolsSetup.exe como asset de uma pre-release de teste
    # (API de conteudo tem limite de 100 MB — usar release asset para o instalador)
    $testTag = "test-latest"
    Write-Host "Publicando pre-release de teste ($testTag)..." -ForegroundColor Cyan

    # Remove a release de teste anterior se existir
    try {
        $oldRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/tags/$testTag" -Headers $headers -Method GET -ErrorAction Stop
        Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/$($oldRelease.id)" -Headers $headers -Method DELETE | Out-Null
        Write-Host "  Release anterior removida." -ForegroundColor DarkGray
    } catch {}

    # Remove a tag anterior se existir
    try {
        Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/git/refs/tags/$testTag" -Headers $headers -Method DELETE | Out-Null
    } catch {}

    # Cria nova pre-release de teste
    try {
        $testReleaseBody = @{
            tag_name         = $testTag
            target_commitish = "test-builds"
            name             = "Test Build (latest)"
            body             = "Build de teste automatico. Nao usar em producao."
            draft            = $false
            prerelease       = $true
        } | ConvertTo-Json -Compress
        $testRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases" -Headers $headers -Method POST -Body ([System.Text.Encoding]::UTF8.GetBytes($testReleaseBody)) -ContentType "application/json; charset=utf-8"
        Write-Host "  Pre-release criada: OK" -ForegroundColor Green
    } catch {
        Write-Host "  ERRO ao criar pre-release: $_" -ForegroundColor Red
        exit 1
    }

    # Upload do instalador como asset da pre-release
    $testUploadUrl = ($testRelease.upload_url -replace '\{.*\}', '') + "?name=MathoolsSetup.exe"
    try {
        $setupBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $installer))
        Invoke-RestMethod -Uri $testUploadUrl -Headers $headers -Method POST -Body $setupBytes -ContentType "application/octet-stream" | Out-Null
        Write-Host "  MathoolsSetup.exe enviado: OK" -ForegroundColor Green
    } catch {
        Write-Host "  ERRO ao enviar MathoolsSetup.exe: $_" -ForegroundColor Red
        exit 1
    }

    # Lista de arquivos de dados para enviar via API de conteudo
    $testFiles = [System.Collections.Generic.List[string]]@(
        "Logo-mathey-tk-3.ico",
        "config.toml",
        "cURL 8091.txt",
        "cURL 8117.txt",
        "cURL 8121.txt",
        "cURL 50012.txt",
        "payload_os_pesquisar.txt",
        "payload_os_load.txt",
        "payload_os_imprimir.txt"
    )

    # .py dos modulos internos do projeto (listados no --hidden-import)
    $projectPyFiles = @(
        "launcher_gui.py",
        "LOGIN_GUI.py",
        "updater.py",
        "mathtools_1_0.py",
        "waterfy_engine.py",
        "auth_system.py",
        "roteiricacao.py",
        "login_gui.py",
        "dashboard_html.py",
        "email_merge_module.py",
        "fatura_anexo_email.py",
        "config_loader.py",
        "audit_logger.py",
        "retry_utils.py",
        "circuit_breaker.py",
        "schema_validator.py",
        "html_sanitizer.py",
        "timezone_utils.py",
        "docx_merge_module.py",
        "previsao.py",
        "pn_generator.py",
        "text_utils.py"
    )
    foreach ($f in $projectPyFiles) {
        if (Test-Path $f) { $testFiles.Add($f) }
    }

    # Opcionais — so envia se existirem
    $optionalFiles = @(
        "stats.json",
        "imagem a direita do loguin.png",
        "imagem_a_direita_do_loguin.png",
        "imagem a direita do loguin.jpg",
        "imagem_a_direita_do_loguin.jpg",
        "Itapoa_branca.png",
        "Logo mathey tk 1.png",
        "itapoa_informa.png"
    )
    foreach ($f in $optionalFiles) {
        if (Test-Path $f) { $testFiles.Add($f) }
    }

    # Pasta email_templates — envia cada arquivo individualmente
    if (Test-Path "email_templates") {
        Get-ChildItem -Path "email_templates" -File -Recurse | ForEach-Object {
            $testFiles.Add($_.FullName)
        }
    }

    # Funcao auxiliar para enviar um arquivo para a branch
    function Send-FileToBranch {
        param(
            [string]$LocalPath,
            [string]$RemotePath
        )
        $fileBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $LocalPath))
        $b64 = [Convert]::ToBase64String($fileBytes)
        $apiUrl = "https://api.github.com/repos/$REPO/contents/$RemotePath"

        # Verifica se ja existe para pegar o sha e fazer update
        $existingSha = $null
        try {
            $existing = Invoke-RestMethod -Uri "${apiUrl}?ref=$testBranch" -Headers $headers -Method GET -ErrorAction Stop
            $existingSha = $existing.sha
        } catch {}

        $body = @{
            message = "test: atualiza $RemotePath"
            content = $b64
            branch  = $testBranch
        }
        if ($existingSha) { $body.sha = $existingSha }

        Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method PUT -Body ($body | ConvertTo-Json -Depth 5) -ContentType "application/json" | Out-Null
    }

    # Envia cada arquivo
    $successCount = 0
    $failCount = 0
    foreach ($filePath in $testFiles) {
        if (-not (Test-Path $filePath)) {
            Write-Host "  AVISO: $filePath nao encontrado, pulando." -ForegroundColor Yellow
            continue
        }

        # Monta o caminho remoto com timestamp para manter historico
        $fileName = $filePath -replace '^.*?email_templates', 'email_templates'
        if ($filePath -notmatch "email_templates") {
            $fileName = Split-Path $filePath -Leaf
        }
        $remotePath = "test-builds/$timestamp/$fileName"

        try {
            Send-FileToBranch -LocalPath $filePath -RemotePath $remotePath
            Write-Host "  Enviado: $fileName" -ForegroundColor Green
            $successCount++
        } catch {
            Write-Host "  ERRO ao enviar $fileName`: $_" -ForegroundColor Red
            $failCount++
        }
    }

    Write-Host ""
    if ($failCount -gt 0) {
        Write-Host "AVISO: $failCount arquivo(s) falharam no envio." -ForegroundColor Yellow
    }
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host " BUILD DE TESTE ENVIADO!" -ForegroundColor Green
    Write-Host " Instalador : https://github.com/$REPO/releases/tag/$testTag" -ForegroundColor Green
    Write-Host " Arquivos   : $successCount enviados para branch $testBranch" -ForegroundColor Green
    Write-Host " URL branch : https://github.com/$REPO/tree/$testBranch/test-builds/$timestamp" -ForegroundColor Green
    Write-Host "=============================================" -ForegroundColor Green
    exit 0
}

# ================================================================
# 10) Publicar release oficial no GitHub
# ================================================================
Write-Host "Publicando Release v$Version no GitHub..." -ForegroundColor Cyan
$TAG = "v$Version"

$releaseNotes = if ($Notes) { $Notes } else { "Atualizacao $Version" }

$versionJson = @{
    version = $Version
    package_type = "installer"
    download_url = "https://github.com/$REPO/releases/download/$TAG/MathoolsSetup.exe"
    release_notes = $releaseNotes
} | ConvertTo-Json -Compress

try {
    $getFile = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/contents/version.json" -Headers $headers -Method GET
    $fileSha = $getFile.sha
} catch {
    $fileSha = $null
}

$b64Content = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($versionJson))
$updateBody = @{ message = "chore: bump version to $Version"; content = $b64Content }
if ($fileSha) { $updateBody.sha = $fileSha }

try {
    Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/contents/version.json" -Headers $headers -Method PUT -Body ($updateBody | ConvertTo-Json) | Out-Null
    Write-Host "version.json atualizado: OK" -ForegroundColor Green
} catch {
    Write-Host "ERRO ao atualizar version.json: $_" -ForegroundColor Red
    exit 1
}

try {
    $createBody = @{
        tag_name = $TAG
        name = $TAG
        body = $releaseNotes
        draft = $false
        prerelease = $false
        generate_release_notes = $false
    } | ConvertTo-Json -Compress
    $createBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($createBody)
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases" -Headers $headers -Method POST -Body $createBodyBytes -ContentType "application/json; charset=utf-8"
    Write-Host "Release criado: OK" -ForegroundColor Green
} catch {
    Write-Host "ERRO ao criar release: $_" -ForegroundColor Red
    exit 1
}

$uploadUrl = ($release.upload_url -replace '\{.*\}', '') + "?name=MathoolsSetup.exe"
try {
    $setupBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $installer))
    Invoke-RestMethod -Uri $uploadUrl -Headers $headers -Method POST -Body $setupBytes -ContentType "application/octet-stream" | Out-Null
    Write-Host ""
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host " RELEASE PUBLICADO COM SUCESSO!" -ForegroundColor Green
    Write-Host " Versao : v$Version" -ForegroundColor Green
    Write-Host " Asset  : MathoolsSetup.exe" -ForegroundColor Green
    Write-Host " URL    : https://github.com/$REPO/releases/tag/$TAG" -ForegroundColor Green
    Write-Host "=============================================" -ForegroundColor Green
} catch {
    Write-Host "ERRO ao enviar instalador: $_" -ForegroundColor Red
    exit 1
}