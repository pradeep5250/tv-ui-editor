param()
$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

$SDK = "C:\Users\DELL\AppData\Local\Android\Sdk"
$JAR = "$SDK\platforms\android-28\android.jar"
$BT = "$SDK\build-tools\34.0.0"
$JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"

if (-not (Test-Path $JAR)) { throw "android.jar not found at $JAR" }
if (-not (Test-Path "$BT\aapt2.exe")) { throw "aapt2 not found at $BT" }

$KEYSTORE = Join-Path $PSScriptRoot "release.keystore"
$KEYPASS = "dumbtvhome"

if (-not (Test-Path $KEYSTORE)) {
    Write-Host "Creating signing keystore..."
    & "$JAVA_HOME\bin\keytool.exe" -genkeypair -keystore "$KEYSTORE" -alias home -keyalg RSA -keysize 2048 -validity 10000 -storepass "$KEYPASS" -keypass "$KEYPASS" -dname "CN=dumbtv home"
}

$WORK = Join-Path $env:TEMP ("dumbtv-build-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path "$WORK\gen" | Out-Null
New-Item -ItemType Directory -Force -Path "$WORK\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "build" | Out-Null

try {
    Write-Host "1. Compiling resources with aapt2..."
    & "$BT\aapt2.exe" compile --dir res -o "$WORK\res.zip"
    if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

    Write-Host "2. Linking resources with aapt2..."
    & "$BT\aapt2.exe" link -I "$JAR" --manifest AndroidManifest.xml "$WORK\res.zip" `
        --min-sdk-version 25 --target-sdk-version 28 `
        --version-code 9 --version-name "1.8" `
        --java "$WORK\gen" -o "$WORK\app.apk"
    if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

    Write-Host "3. Collecting Java sources..."
    $sources = Get-ChildItem -Path "src", "$WORK\gen" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
    $sourcesFile = "$WORK\sources.txt"
    $sources | ForEach-Object { '"' + ($_.Replace('\', '/')) + '"' } | Out-File -FilePath $sourcesFile -Encoding ascii

    Write-Host "4. Compiling Java with javac..."
    & "$JAVA_HOME\bin\javac.exe" --release 8 -Xlint:all -Xlint:-options -encoding UTF-8 -classpath "$JAR" -d "$WORK\classes" "@$sourcesFile"
    if ($LASTEXITCODE -ne 0) { throw "javac failed" }

    Write-Host "5. Dexing classes with d8..."
    & "$JAVA_HOME\bin\jar.exe" -cf "$WORK\classes.jar" -C "$WORK\classes" .
    if ($LASTEXITCODE -ne 0) { throw "jar failed" }

    cmd.exe /c "`"$BT\d8.bat`" --release --min-api 25 --lib `"$JAR`" --output `"$WORK`" `"$WORK\classes.jar`""
    if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

    Write-Host "6. Adding classes.dex to APK..."
    & "$JAVA_HOME\bin\jar.exe" -uf "$WORK\app.apk" -C "$WORK" classes.dex
    if ($LASTEXITCODE -ne 0) { throw "jar update failed" }

    Write-Host "7. Zipaligning APK..."
    & "$BT\zipalign.exe" -f -p 4 "$WORK\app.apk" "$WORK\aligned.apk"
    if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

    Write-Host "8. Signing APK..."
    $outApk = Join-Path $PSScriptRoot "build\dumbtv-home.apk"
    cmd.exe /c "`"$BT\apksigner.bat`" sign --ks `"$KEYSTORE`" --ks-pass pass:$KEYPASS --key-pass pass:$KEYPASS --out `"$outApk`" `"$WORK\aligned.apk`""
    if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

    Write-Host "9. Verifying signed APK..."
    cmd.exe /c "`"$BT\apksigner.bat`" verify `"$outApk`""
    if ($LASTEXITCODE -ne 0) { throw "verify failed" }

    $size = (Get-Item $outApk).Length
    Write-Host "BUILD SUCCESSFUL! Created $outApk ($size bytes)"
}
finally {
    if (Test-Path $WORK) { Remove-Item -Recurse -Force $WORK -ErrorAction SilentlyContinue }
}
