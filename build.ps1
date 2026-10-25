$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force 'build/classes' | Out-Null
    $sources = @(Get-ChildItem -LiteralPath 'src/decaf' -Filter '*.java' | ForEach-Object { $_.FullName })
    & javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d 'build/classes' @sources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
