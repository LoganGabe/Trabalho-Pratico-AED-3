$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    New-Item -ItemType Directory -Force out | Out-Null
    $fontesCatalogo = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    [IO.File]::WriteAllLines((Join-Path (Get-Location) 'out/fontes.txt'), $fontesCatalogo)
    & javac --release 11 -encoding UTF-8 -d out '@out/fontes.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }
    Write-Host 'Compilado. Execute: java -cp out Main'
} finally { Pop-Location }
