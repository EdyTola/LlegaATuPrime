$ErrorActionPreference = 'Stop'

$Nombre = 'Llega a tu Prime'
$Version = '1.0.0'
$Proveedor = 'bey'
$Descripcion = 'Entrenamiento, nutricion y habitos saludables'
$ClasePrincipal = 'com.bey.desktop.beyLanzador'
$UrlInno = 'https://github.com/jrsoftware/issrc/releases/download/is-6_7_4/innosetup-6.7.4.exe'
$HashInno = 'A549929319F9120716677C685535CA4671E4B937B5FDC87E2585F32D8EB30E8C'

$Raiz = Split-Path -Parent $PSScriptRoot
$Empaquetado = Join-Path $Raiz 'empaquetado'
$Trabajo = Join-Path $Raiz 'target\instalador'
$Entrada = Join-Path $Trabajo 'entrada'
$Destino = Join-Path $Raiz 'dist'
$ImagenApp = Join-Path $Destino 'app-image'
$Herramientas = Join-Path $Empaquetado 'herramientas'
$DirInno = Join-Path $Herramientas 'inno'

function Escribir-Paso([string]$texto) {
    Write-Host ''
    Write-Host "==> $texto" -ForegroundColor Cyan
}

function Buscar-Jpackage {
    $candidatos = @()
    if ($env:JAVA_HOME) { $candidatos += (Join-Path $env:JAVA_HOME 'bin\jpackage.exe') }
    $comando = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if ($comando) { $candidatos += $comando.Source }
    foreach ($base in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path $base) {
            Get-ChildItem $base -Directory | Sort-Object Name -Descending | ForEach-Object {
                $candidatos += (Join-Path $_.FullName 'bin\jpackage.exe')
            }
        }
    }
    foreach ($c in $candidatos) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    throw 'No se encontro jpackage.exe. Instala un JDK 17 o superior (no un JRE) y define JAVA_HOME.'
}

function Buscar-Iscc {
    $candidatos = @()
    $comando = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($comando) { $candidatos += $comando.Source }
    foreach ($base in @("${env:ProgramFiles(x86)}\Inno Setup 6", "$env:ProgramFiles\Inno Setup 6", "$env:LOCALAPPDATA\Programs\Inno Setup 6", $DirInno)) {
        $candidatos += (Join-Path $base 'ISCC.exe')
    }
    foreach ($c in $candidatos) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

function Instalar-Inno {
    New-Item -ItemType Directory -Force $Herramientas | Out-Null
    $instalador = Join-Path $Herramientas 'innosetup.exe'
    Write-Host 'Descargando Inno Setup ...'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $UrlInno -OutFile $instalador -UseBasicParsing
    $firma = Get-AuthenticodeSignature $instalador
    if ($firma.Status -ne 'Valid' -or $firma.SignerCertificate.Subject -notmatch 'Pyrsys B.V.|Jordan Russell|Martijn Laan') {
        Remove-Item $instalador -Force
        throw "La firma digital de Inno Setup no es valida ($($firma.Status)). Se cancela la instalacion."
    }
    $hash = (Get-FileHash $instalador -Algorithm SHA256).Hash
    if ($hash -ne $HashInno) {
        Remove-Item $instalador -Force
        throw 'El archivo descargado de Inno Setup no coincide con la huella esperada. Se cancela la instalacion.'
    }
    Write-Host "Firma y huella verificadas: $($firma.SignerCertificate.Subject)"
    $proceso = Start-Process $instalador -ArgumentList '/VERYSILENT', '/SUPPRESSMSGBOXES', '/NORESTART', '/CURRENTUSER', "/DIR=$DirInno" -Wait -PassThru
    Remove-Item $instalador -Force
    if ($proceso.ExitCode -ne 0) { throw 'No se pudo instalar Inno Setup.' }
}

Escribir-Paso 'Buscando JDK (jpackage)'
$Jpackage = Buscar-Jpackage
$Jdeps = Join-Path (Split-Path -Parent $Jpackage) 'jdeps.exe'
Write-Host $Jpackage

Escribir-Paso 'Buscando Inno Setup (creador del Setup.exe)'
$Iscc = Buscar-Iscc
if (-not $Iscc) {
    Instalar-Inno
    $Iscc = Buscar-Iscc
}
if (-not $Iscc) { throw 'No se encontro ISCC.exe despues de instalar Inno Setup.' }
Write-Host $Iscc

Escribir-Paso 'Compilando la aplicacion con Maven'
Push-Location $Raiz
try {
    if (Test-Path $Trabajo) { Remove-Item $Trabajo -Recurse -Force }
    if (Test-Path $Destino) { Get-ChildItem $Destino | Remove-Item -Recurse -Force }
    New-Item -ItemType Directory -Force $Entrada | Out-Null
    New-Item -ItemType Directory -Force $Destino | Out-Null
    $mvn = Join-Path $Raiz 'mvnw.cmd'
    if (-not (Test-Path $mvn)) { $mvn = 'mvn.cmd' }
    & $mvn -q compile jar:jar dependency:copy-dependencies "-DoutputDirectory=$Entrada" -DincludeScope=runtime
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion con Maven fallo.' }
    $Jar = Get-ChildItem (Join-Path $Raiz 'target') -Filter 'bey-desktop-*.jar' | Select-Object -First 1
    if (-not $Jar) { throw 'No se genero el archivo JAR de la aplicacion.' }
    Copy-Item $Jar.FullName $Entrada

    Escribir-Paso 'Calculando los modulos de Java necesarios'
    $modulos = (& $Jdeps --ignore-missing-deps --multi-release base --print-module-deps --class-path (Join-Path $Entrada '*') $Jar.FullName 2>$null) | Select-Object -First 1
    $modulos = "$modulos,java.desktop,java.logging,java.xml,java.net.http,java.prefs,jdk.unsupported,jdk.crypto.ec"
    $modulos = ($modulos -split ',' | Where-Object { $_ } | Sort-Object -Unique) -join ','
    Write-Host $modulos

    Escribir-Paso 'Generando la aplicacion con su propio Java (jpackage)'
    $argumentos = @(
        '--type', 'app-image',
        '--name', $Nombre,
        '--app-version', $Version,
        '--vendor', $Proveedor,
        '--description', $Descripcion,
        '--icon', (Join-Path $Empaquetado 'bey.ico'),
        '--input', $Entrada,
        '--main-jar', $Jar.Name,
        '--main-class', $ClasePrincipal,
        '--add-modules', $modulos,
        '--java-options', '-Dfile.encoding=UTF-8',
        '--java-options', '--enable-native-access=ALL-UNNAMED',
        '--dest', $Trabajo,
        '--temp', (Join-Path $Trabajo 'temporal')
    )
    & $Jpackage @argumentos
    if ($LASTEXITCODE -ne 0) { throw 'jpackage no pudo generar la aplicacion.' }
    Move-Item (Join-Path $Trabajo $Nombre) $ImagenApp

    Escribir-Paso 'Creando el Setup.exe con Inno Setup'
    & $Iscc "/DOrigen=$ImagenApp" "/DSalida=$Destino" (Join-Path $Empaquetado 'LlegaATuPrime.iss')
    if ($LASTEXITCODE -ne 0) { throw 'Inno Setup no pudo crear el Setup.exe.' }

    Escribir-Paso 'Empaquetando para distribuir'
    $setup = Join-Path $Destino 'LlegaATuPrime_Setup.exe'
    Copy-Item (Join-Path $Empaquetado 'LEEME.txt') $Destino
    Compress-Archive -Path $setup, (Join-Path $Destino 'LEEME.txt') -DestinationPath (Join-Path $Destino 'LlegaATuPrime_Setup.zip') -Force
}
finally {
    Pop-Location
}

Escribir-Paso 'Listo'
foreach ($nombreArchivo in 'LlegaATuPrime_Setup.exe', 'LlegaATuPrime_Setup.zip') {
    $archivo = Get-Item (Join-Path $Destino $nombreArchivo)
    Write-Host ("{0}  ({1} MB)" -f $archivo.FullName, [math]::Round($archivo.Length / 1MB, 1)) -ForegroundColor Green
}
