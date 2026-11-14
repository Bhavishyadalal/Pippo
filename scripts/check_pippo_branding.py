#!/usr/bin/env python3
"""Check Android branding and the package/resource/JNI contracts after a rename."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors = []
def require(condition, message):
    if not condition:
        errors.append(message)

gradle = (ROOT / 'app/build.gradle.kts').read_text()
require('namespace = "app.pippo.music"' in gradle, 'Wrong Android namespace')
require('applicationId = "app.pippo.music"' in gradle, 'Wrong production application ID')
require('pippoRequireReleaseSigning' in gradle, 'Missing production signing guard')

old = re.compile(r'mochy', re.I)
checked = 0
for base in ('app/src', 'modules', 'providers'):
    for path in (ROOT / base).rglob('*'):
        if not path.is_file() or path.suffix not in ('.kt', '.kts', '.xml', '.cpp', '.h', '.proto'):
            continue
        checked += 1
        text = path.read_text()
        require('com.mochyy.mochymusic' not in text, f'Old package in {path.relative_to(ROOT)}')
        # Attribution is intentionally preserved in About's upstream-credit entries.
        for number, line in enumerate(text.splitlines(), 1):
            if old.search(line) and path.name != 'AboutSheet.kt':
                errors.append(f'Old brand in {path.relative_to(ROOT)}:{number}')
        if path.suffix == '.xml':
            try:
                element = ET.fromstring(text)
            except ET.ParseError as exc:
                errors.append(f'Invalid XML {path.relative_to(ROOT)}: {exc}')
                continue
            for string in element.findall('string'):
                if string.get('name') == 'app_name':
                    require(''.join(string.itertext()) == ('Pippo Dev' if '/src/debug/' in str(path) else 'Pippo'), f'Wrong launcher label in {path}')

manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
ns = '{http://schemas.android.com/apk/res/android}'
for alias in manifest.findall('./application/activity-alias'):
    require(alias.get(ns+'label') == '@string/app_name', f'Launcher alias has independent label: {alias.get(ns+"name")}')
for intent in ET.parse(ROOT / 'app/src/main/res/xml/shortcuts.xml').findall('./shortcut/intent'):
    require(intent.get(ns+'targetPackage') == '@string/pippo_application_id', 'Shortcut ignores build variant package')
    require(intent.get(ns+'targetClass') == 'app.pippo.music.MainActivity', 'Wrong shortcut activity')

native = (ROOT / 'app/src/main/kotlin/app/pippo/music/playback/audio/pass/PassNative.kt').read_text()
cpp = '\n'.join(p.read_text() for p in (ROOT / 'app/src/main/cpp').glob('*.cpp'))
for method in re.findall(r'external fun (\w+)\(', native):
    require(f'Java_app_pippo_music_playback_audio_pass_PassNative_{method}(' in cpp, f'Missing renamed JNI method: {method}')
require('System.loadLibrary("pippopass")' in native, 'Wrong JNI library load name')
require('add_library(pippopass SHARED' in (ROOT / 'app/src/main/cpp/CMakeLists.txt').read_text(), 'Wrong native library target')

updates = (ROOT / 'app/src/main/kotlin/app/pippo/music/core/updater/GithubUpdates.kt').read_text()
require('const val GithubOwner = "Bhavishyadalal"' in updates, 'Updater owner mismatch')
require('const val GithubRepo = "Pippo"' in updates, 'Updater repository mismatch')
require(__import__('hashlib').sha256((ROOT/'LICENSE').read_bytes()).hexdigest() == 'fce02ebb691c768cde194afbe91b8025fd7b1f49031f33996deb246a5926f0e2', 'Upstream license changed')
if errors:
    print('\n'.join(errors), file=sys.stderr)
    sys.exit(1)
print(f'Pippo identity checks passed across {checked} source/resource files, launcher aliases, shortcuts, updater and JNI.')
