param(
  [string]$MainClass = "Main"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$libDir = Join-Path $root "lib"
$h2Jar = Join-Path $libDir "h2.jar"
$classesDir = Join-Path $root "out-classes"

if (!(Test-Path $libDir)) { New-Item -ItemType Directory -Path $libDir | Out-Null }

if (!(Test-Path $h2Jar)) {
  Write-Host "Downloading H2..."
  $url = "https://repo1.maven.org/maven2/com/h2database/h2/2.3.232/h2-2.3.232.jar"
  Invoke-WebRequest -Uri $url -OutFile $h2Jar
}

if (Test-Path $classesDir) { Remove-Item -Recurse -Force $classesDir }
New-Item -ItemType Directory -Path $classesDir | Out-Null

$cp = "$h2Jar"

Write-Host "Compiling..."
javac -encoding UTF-8 -cp $cp -d $classesDir (Get-ChildItem -Recurse -Filter *.java -Path (Join-Path $root "src") | ForEach-Object { $_.FullName })

Write-Host "Running..."
java -cp "$classesDir;$cp" $MainClass

