"""Package the mod and reproducible sources; exclude tools, caches and test worlds."""
from pathlib import Path
import hashlib
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
DIST.mkdir(exist_ok=True)
NAME = 'unity_feast-1.0.0'
jar = ROOT / 'build/libs' / f'{NAME}.jar'
if not jar.is_file():
    raise SystemExit('Build the mod first: tools/gradle.ps1 build')
shutil.copy2(jar, DIST / jar.name)

root_files = [
    '.gitignore', 'README.md', 'ASSET_SOURCES.md', 'LICENSE', 'TEMPLATE_LICENSE.txt',
    'COMPATIBILITY.md', 'TEST_REPORT.md', 'TASKS.md', 'plan.md',
    'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
]
evidence = [
    'DEVELOPMENT.md', 'build.log', 'build-testmod.log', 'junit.xml',
    'license-build.log', 'license-check.txt',
    'gametest-final-baseline.log', 'gametest-26.1.log', 'gametest-26.1.1.log',
    'gametest-26.1.2.111.log', 'multiplayer-before-restart.log',
    'multiplayer-after-restart.log', 'multiplayer-result.txt',
    'natural-fishing-result.txt', 'resource-check.txt', 'artifact-verification.txt',
    'gitignore-check.txt', 'asset-preview.png',
]
files = [ROOT / name for name in root_files]
files += [ROOT / 'docs' / name for name in evidence]
for directory in ['src', 'gradle/wrapper', 'tools', 'docs/screenshots']:
    files.extend(path for path in (ROOT / directory).rglob('*') if path.is_file()
                 and '__pycache__' not in path.parts and path.suffix not in ['.pyc', '.class', '.tmp', '.bak'])

source = DIST / f'{NAME}-source.zip'
with zipfile.ZipFile(source, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(set(files)):
        if path.is_file():
            entry = zipfile.ZipInfo(f'{NAME}-source/{path.relative_to(ROOT).as_posix()}')
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = (0o755 if path.name == 'gradlew' else 0o644) << 16
            archive.writestr(entry, path.read_bytes())
with zipfile.ZipFile(source) as archive:
    if archive.testzip() is not None:
        raise SystemExit('Source archive CRC verification failed')

lines = [f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}'
         for path in [DIST / jar.name, source]]
(DIST / 'SHA256SUMS.txt').write_text('\n'.join(lines) + '\n', encoding='utf-8')
print('\n'.join(lines))
print('Release directory:', DIST)
