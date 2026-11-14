$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$grito = Join-Path $root 'grito.ogg'
if (-not (Test-Path $grito)) { throw 'Falta grito.ogg' }

$png = Join-Path $root 'build-pruebas'
if (-not (Test-Path (Join-Path $png 'susto_0.png'))) {
    throw 'Faltan las caras. Compila antes para generarlas.'
}

$stage = Join-Path $root 'build-pack'
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Force -Path $stage | Out-Null
$zipActual = Join-Path $root '..\..\web\pack\cofre-venta.zip'
tar -xf $zipActual -C $stage

$itemDir = Join-Path $stage 'assets\preguntones\items'
$modelDir = Join-Path $stage 'assets\preguntones\models\item'
$texDir = Join-Path $stage 'assets\preguntones\textures\item'
$sndDir = Join-Path $stage 'assets\preguntones\sounds\susto'
$langDir = Join-Path $stage 'assets\minecraft\lang'
New-Item -ItemType Directory -Force -Path $itemDir, $modelDir, $texDir, $sndDir, $langDir | Out-Null

$utf8 = New-Object System.Text.UTF8Encoding $false
Copy-Item $grito (Join-Path $sndDir 'grito.ogg')
Get-ChildItem $sndDir -Filter '*.ogg' | Where-Object { $_.Name -ne 'grito.ogg' } | Remove-Item -Force
[System.IO.File]::WriteAllText((Join-Path $stage 'assets\preguntones\sounds.json'), @"
{
  "susto.grito": {
    "sounds": [
      { "name": "preguntones:susto/grito", "volume": 1.0, "pitch": 1.0 }
    ]
  }
}
"@, $utf8)
$langTexto = "{`n  `"subtitles.preguntones.susto.grito`": `"\u200b`"`n}`n"
foreach ($idioma in @('es_es', 'es_mx', 'es_ar', 'en_us', 'en_gb')) {
    [System.IO.File]::WriteAllText((Join-Path $langDir "$idioma.json"), $langTexto, $utf8)
}

$misc = Join-Path $stage 'assets\minecraft\textures\misc'
New-Item -ItemType Directory -Force -Path $misc | Out-Null
$cara = Join-Path $root 'cara-ia.png'
if (-not (Test-Path $cara)) { throw 'Falta cara-ia.png' }
Copy-Item $cara (Join-Path $misc 'pumpkinblur.png')

for ($i = 0; $i -lt 6; $i++) {
    Copy-Item (Join-Path $png "susto_$i.png") (Join-Path $texDir "susto_$i.png")
    [System.IO.File]::WriteAllText((Join-Path $itemDir "susto_$i.json"), @"
{
  "model": {
    "type": "minecraft:model",
    "model": "preguntones:item/susto_$i"
  }
}
"@, $utf8)
    [System.IO.File]::WriteAllText((Join-Path $modelDir "susto_$i.json"), @"
{
  "textures": {
    "0": "preguntones:item/susto_$i",
    "particle": "preguntones:item/susto_$i"
  },
  "elements": [
    {
      "shade": false,
      "from": [0, 0, 7.5],
      "to": [16, 16, 8.5],
      "faces": {
        "south": { "uv": [0, 0, 16, 16], "texture": "#0" },
        "north": { "uv": [0, 0, 16, 16], "texture": "#0" }
      }
    }
  ]
}
"@, $utf8)
}

$destino = Join-Path $root '..\..\web\pack\cofre-venta.zip'
if (Test-Path $destino) { Remove-Item $destino }
Push-Location $stage
tar -a -c -f $destino *
Pop-Location
$hash = (Get-FileHash -Algorithm SHA1 $destino).Hash.ToLower()
Write-Host "sha1 $hash"
Write-Host "zip $destino"
