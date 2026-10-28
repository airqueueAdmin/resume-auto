param(
  [string]$Version = ''
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $projectRoot 'extension\manifest.json'
$manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
$packageVersion = if ($Version.Trim()) { $Version.Trim() } else { [string]$manifest.version }
$outputDirectory = Join-Path $projectRoot 'frontend\public\downloads'
$outputPath = Join-Path $outputDirectory "career-note-extension-v$packageVersion.zip"
$packageFiles = @('manifest.json', 'background.js', 'content.js', 'popup.html', 'popup.js')
$sourceFiles = $packageFiles | ForEach-Object { Join-Path $projectRoot "extension\$_" }

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
Compress-Archive -Path $sourceFiles -DestinationPath $outputPath -Force
Write-Output "Created $outputPath"
