param([string]$Base)
$ErrorActionPreference = 'Stop'
$raizCatalogo = Split-Path $PSScriptRoot -Parent
if ([string]::IsNullOrWhiteSpace($Base)) { $Base = $raizCatalogo }
if (-not [IO.Path]::IsPathRooted($Base)) { $Base = Join-Path (Get-Location).Path $Base }
$baseCatalogo = [IO.Path]::GetFullPath($Base)
& (Join-Path $PSScriptRoot 'compilar.ps1')
New-Item -ItemType Directory -Force -Path $baseCatalogo | Out-Null
Push-Location $baseCatalogo
try {
    & java -cp (Join-Path $raizCatalogo 'out') Main
    if ($LASTEXITCODE -ne 0) { throw "A aplicação encerrou com código $LASTEXITCODE." }
} finally { Pop-Location }
