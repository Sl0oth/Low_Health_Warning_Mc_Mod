#!/usr/bin/env python3
"""Build once per compatibility group; audit the identical jar on every target. Never publishes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--group', default='all', help='Group ID from release-targets.json, or all')
parser.add_argument('--targets', default='scripts/release-targets.json', help='Pinned compatibility-group manifest')
args = parser.parse_args()
groups = json.loads((ROOT/args.targets).read_text())['groups']
if args.group != 'all':
    groups = [g for g in groups if g['id'] == args.group]
    if not groups:
        parser.error('Unknown group')
props = dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines()
             if '=' in line and not line.lstrip().startswith('#'))
version = props['mod_version']
output = ROOT/'build/releases'/version
output.mkdir(parents=True,exist_ok=True)
staging=ROOT/'build/candidates'/version
staging.mkdir(parents=True,exist_ok=True)
logs = ROOT/'build/audit'
logs.mkdir(parents=True,exist_ok=True)
java_paths = [ROOT/f'build/toolchains/java{v}/Contents/Home' for v in (17,21)]
java_paths = [str(p) for p in java_paths if p.is_dir()]
extra = ['-Porg.gradle.java.installations.paths='+','.join(java_paths)] if java_paths else []

def options(target):
    return [f'-Pminecraft_version={target["minecraft"]}', f'-Pfabric_version={target["fabric"]}',
            f'-Pmodmenu_version={target["modmenu"]}', *extra]

def run(command, log):
    with log.open('w') as stream:
        result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode:
        print('\n'.join(log.read_text(errors='replace').splitlines()[-50:]),file=sys.stderr)
        raise SystemExit(f'Failed; full log: {log}')

for group in groups:
    baseline = next(t for t in group['targets'] if t['minecraft']==group['build'])
    print(f'Building group {group["id"]}',flush=True)
    run(['./gradlew','build',*options(baseline),f'-Pminecraft_range={group["range"]}'],logs/f'build-{group["id"]}.log')
    jar = staging/f'lowhelthwarning-{group["id"]}-{version}.jar'
    shutil.copy2(ROOT/f'build/libs/lowhelthwarning-mc{group["build"]}-{version}.jar',jar)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    verified=[]
    for target in group['targets']:
        mc=target['minecraft']
        print(f'  Auditing {mc} with the same jar',flush=True)
        started=time.time()
        run(['./gradlew','runPackagedClientAudit',*options(target),f'-Paudit_jar={jar}'],logs/f'packaged-{mc}.log')
        assert hashlib.sha256(jar.read_bytes()).hexdigest()==digest, 'Candidate changed during audit'
        evidence=output/'reports'/mc
        evidence.mkdir(parents=True,exist_ok=True)
        shutil.copy2(logs/f'packaged-{mc}.log',evidence/'audit.log')
        for screenshot in (ROOT/f'build/run/packaged-{mc}/screenshots').glob('*.png'):
            if screenshot.stat().st_mtime >= started:
                shutil.copy2(screenshot,evidence/screenshot.name)
        verified.append(mc)
        print(f'  Passed {mc}',flush=True)
    manifest={'artifact':jar.name,'version':version,'loader':'fabric','sha256':digest,
              'runtime_tested':verified,'java':17 if group['build'] in ('1.20','1.20.1','1.20.2','1.20.3','1.20.4') else (25 if group['build'].startswith('26.') else 21),
              'dependencies':group['targets'],'status':'release candidate; not published'}
    shutil.copy2(jar, output/jar.name)
    (output/jar.with_suffix('.json').name).write_text(json.dumps(manifest,indent=2)+'\n')
print(f'Verified candidates: {output}',flush=True)
