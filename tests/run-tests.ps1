param([switch]$Interface)
$ErrorActionPreference = 'Stop'
$raizCatalogo = Split-Path $PSScriptRoot -Parent
$pastaTempCatalogo = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$temporarioCatalogo = Join-Path $pastaTempCatalogo ('aed3-tests-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $temporarioCatalogo | Out-Null
Push-Location $raizCatalogo
try {
    $classesCatalogo = Join-Path $temporarioCatalogo 'classes'
    New-Item -ItemType Directory -Path $classesCatalogo | Out-Null
    $fontesCatalogo = @(Get-ChildItem src,tests -Recurse -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    $listaCatalogo = Join-Path $temporarioCatalogo 'fontes.txt'
    [IO.File]::WriteAllLines($listaCatalogo, $fontesCatalogo)
    & javac --release 11 -encoding UTF-8 -d $classesCatalogo "@$listaCatalogo"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar os testes.' }
    $testesCatalogo = @('JogoDesenvolvedoraTest', 'ArquivoJogoDesenvolvedoraTest', 'RelacionamentoJogoDesenvolvedoraTest', 'CatalogoTest', 'TestSuite')
    if ($Interface) { $testesCatalogo += 'InterfaceGraficaTest' }
    foreach ($testeCatalogo in $testesCatalogo) {
        $baseCatalogo = Join-Path $temporarioCatalogo $testeCatalogo
        New-Item -ItemType Directory -Path $baseCatalogo | Out-Null
        Set-Location $baseCatalogo
        $opcoesCatalogo = @('-Dfile.encoding=UTF-8', '-Duser.language=en', '-Duser.country=US')
        & java @opcoesCatalogo -cp $classesCatalogo $testeCatalogo
        if ($LASTEXITCODE -ne 0) { throw "Falha em $testeCatalogo" }
    }
} finally {
    Pop-Location
    # Apenas a pasta com nome único criada por este script é removida.
    $caminhoCatalogo = [IO.Path]::GetFullPath($temporarioCatalogo)
    $prefixoTempCatalogo = $pastaTempCatalogo.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if ($caminhoCatalogo.StartsWith($prefixoTempCatalogo, [StringComparison]::OrdinalIgnoreCase) -and (Split-Path $caminhoCatalogo -Leaf) -match '^aed3-tests-[a-f0-9]{32}$') {
        Remove-Item -LiteralPath $caminhoCatalogo -Recurse -Force
    }
}
