# ================================================================
#  Oaplicativo Builder + GitHub Releaser (Gradle + GitHub API)
# ================================================================

Write-Host "================================" -ForegroundColor Cyan
Write-Host "    Oaplicativo Android Builder" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 1) Verificações de Ambiente (Android/Java)
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "ERRO: gradlew.bat nao encontrado. Voce esta na raiz do projeto Android?" -ForegroundColor Red
    exit 1
}

# Configura o JAVA_HOME para usar o JDK do Android Studio caso nao esteja configurado ou falhe
$androidStudioJdk = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $androidStudioJdk) {
    $env:JAVA_HOME = $androidStudioJdk
    Write-Host "Usando JDK do Android Studio: $env:JAVA_HOME" -ForegroundColor Gray
}

# 2) Modo
Write-Host "O que voce deseja fazer?" -ForegroundColor Yellow
Write-Host "[ 1 ] Build Local (Gera APK apenas no PC)"
Write-Host "[ 2 ] Build + Publicar Release Oficial no GitHub"
Write-Host ""
$choice = Read-Host "Digite 1 ou 2"

$Release = $false
$Version = ""
$Token = ""
$Notes = ""
$REPO = "italloskull/AplicativoLeiturista"

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
        "User-Agent" = "PowerShell-Builder"
    }

    $Version = Read-Host "Digite a TAG da versao (ex: 0.9.2)"
    $TAG = "v$Version"

    # Verifica se o repositório existe e o token é válido
    try {
        $repoInfo = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO" -Headers $headers -Method GET
        Write-Host "Conectado ao repositorio: $($repoInfo.full_name)" -ForegroundColor Green
    } catch {
        Write-Host "ERRO: Nao foi possivel acessar o repositorio '$REPO'. Verifique o Token e as permissoes." -ForegroundColor Red
        Write-Host "Detalhe: $_" -ForegroundColor DarkGray
        exit 1
    }

    $Notes = Read-Host "Digite as novidades dessa versao (Changelog)"
    if ([string]::IsNullOrWhiteSpace($Notes)) {
        $Notes = "Atualizacao $Version"
    }

    # --- ATUALIZACAO AUTOMATICA DO build.gradle.kts ---
    Write-Host "Sincronizando versao no build.gradle.kts para $Version..." -ForegroundColor Cyan
    $gradlePath = "app/build.gradle.kts"
    if (Test-Path $gradlePath) {
        $content = Get-Content $gradlePath -Raw

        # Atualiza o versionName
        $content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$Version`""

        # Incrementa o versionCode (opcional, mas recomendado pelo Android)
        if ($content -match 'versionCode\s*=\s*(\d+)') {
            $currentCode = [int]$matches[1]
            $newCode = $currentCode + 1
            $content = $content -replace "versionCode\s*=\s*$currentCode", "versionCode = $newCode"
            Write-Host "versionCode incrementado para $newCode" -ForegroundColor Gray
        }

        Set-Content $gradlePath $content -Encoding UTF8
        Write-Host "Arquivo build.gradle.kts atualizado com sucesso!" -ForegroundColor Green
    }
} elseif ($choice -ne "1") {
    Write-Host "Opcao invalida." -ForegroundColor Red
    exit 1
}

# 3) Limpeza e Build
Write-Host "Limpando e compilando APK de Release..." -ForegroundColor Cyan
.\gradlew clean assembleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: O Gradle falhou ao compilar o projeto." -ForegroundColor Red
    exit 1
}

# Procura o APK gerado
$apkPath = "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    $apkPath = Get-ChildItem "app\build\outputs\apk\release\*.apk" | Where-Object { $_.Name -notmatch "unsigned" } | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $apkPath -or -not (Test-Path $apkPath)) {
    $apkPath = Get-ChildItem "app\build\outputs\apk\release\*.apk" | Select-Object -First 1 -ExpandProperty FullName
}

if (-not (Test-Path $apkPath)) {
    Write-Host "ERRO: APK nao encontrado em app\build\outputs\apk\release\" -ForegroundColor Red
    exit 1
}

$apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 1)
Write-Host "Build concluido: $apkPath ($apkSize MB)" -ForegroundColor Green

# 4) Publicar no GitHub (Se escolhido modo 2)
if ($Release) {
    Write-Host "Criando Release $TAG no GitHub em https://api.github.com/repos/$REPO/releases ..." -ForegroundColor Cyan

    try {
        $releaseBody = @{
            tag_name = $TAG
            name = "Release $TAG"
            body = $Notes
            draft = $false
            prerelease = $false
            generate_release_notes = $true
        } | ConvertTo-Json -Compress

        $createRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases" -Headers $headers -Method POST -Body ([System.Text.Encoding]::UTF8.GetBytes($releaseBody)) -ContentType "application/json; charset=utf-8"

        Write-Host "Release criada (ID: $($createRelease.id))! Fazendo upload do APK..." -ForegroundColor Cyan

        $uploadUrl = ($createRelease.upload_url -replace '\{.*\}', '') + "?name=Oaplicativo_$Version.apk"
        $apkBytes = [System.IO.File]::ReadAllBytes($apkPath)

        Invoke-RestMethod -Uri $uploadUrl -Headers $headers -Method POST -Body $apkBytes -ContentType "application/vnd.android.package-archive" | Out-Null

        Write-Host ""
        Write-Host "=============================================" -ForegroundColor Green
        Write-Host " RELEASE PUBLICADA COM SUCESSO!" -ForegroundColor Green
        Write-Host " URL: https://github.com/$REPO/releases/tag/$TAG" -ForegroundColor Green
        Write-Host "=============================================" -ForegroundColor Green
    } catch {
        Write-Host "ERRO ao publicar no GitHub: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "Build local finalizado. O APK esta em:" -ForegroundColor Green
    Write-Host $apkPath -ForegroundColor Cyan
}
