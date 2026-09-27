"""Install a checksum-verified, pinned portable JDK into this project only."""
from pathlib import Path
import hashlib, urllib.request, zipfile
ROOT=Path(__file__).resolve().parents[1]
TOOLS=ROOT/'.tools'
TOOLS.mkdir(exist_ok=True)
URL='https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1.zip'
SHA256='00c847d804f4a78e9f04f2683faf14fed898535b177b7fc704486cb0284e9283'
archive=TOOLS/'jdk25.zip'
if not archive.exists(): urllib.request.urlretrieve(URL,archive)
if hashlib.sha256(archive.read_bytes()).hexdigest()!=SHA256: raise SystemExit('JDK SHA-256 mismatch; refusing to install')
with zipfile.ZipFile(archive) as z:
    destination=TOOLS/'jdk'
    for info in z.infolist():
        if not (destination/info.filename).resolve().is_relative_to(destination.resolve()): raise SystemExit('Unsafe archive path')
    z.extractall(destination)
print('Portable JDK ready:',next(destination.glob('*/bin/java.exe')))
print('Build with: powershell -ExecutionPolicy Bypass -File tools/gradle.ps1 build')
