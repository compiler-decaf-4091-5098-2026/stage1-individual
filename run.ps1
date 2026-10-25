$ErrorActionPreference = 'Stop'
$classes = Join-Path $PSScriptRoot 'build/classes'
if (!(Test-Path (Join-Path $classes 'decaf/Main.class'))) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'build.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
& java '-Dfile.encoding=UTF-8' -cp $classes decaf.Main @args
exit $LASTEXITCODE
