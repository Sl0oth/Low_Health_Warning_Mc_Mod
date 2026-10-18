#!/usr/bin/env python3
"""Collect only matrix-selected, checksum-verified candidates. Does not publish."""
from pathlib import Path
import hashlib,json,shutil,struct,zipfile
ROOT=Path(__file__).resolve().parents[1]
props=dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
version=props['mod_version']
source=ROOT/'build/releases'/version
output=ROOT/'build/releases'/f'{version}-complete'
output.mkdir(parents=True,exist_ok=True)
groups=json.loads((ROOT/'scripts/release-targets.json').read_text())['groups']
expected={f'lowhelthwarning-{g["id"]}-{version}.jar':('fabric',[t['minecraft'] for t in g['targets']]) for g in groups}
expected[f'lowhelthwarning-1.8.9-forge-{version}.jar']=('forge',['1.8.9'])
rows=[]
selected=[]
count=0
for name,(loader,targets) in expected.items():
    jar=source/name
    manifest=jar.with_suffix('.json')
    data=json.loads(manifest.read_text())
    assert data['loader']==loader and data['runtime_tested']==targets,(name,'unexpected targets')
    assert data['sha256']==hashlib.sha256(jar.read_bytes()).hexdigest(),(name,'checksum mismatch')
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        assert not any('ForgeAudit' in n or 'ConfigAuditGameTest' in n or '/audit/' in n or 'gametest' in n.lower() for n in z.namelist()),'test fixture in release'
        entry='slooth/lowhealth/forge/LowHealthForge.class' if loader=='forge' else 'slooth/lowhealth/LowHealthWarningClient.class'
        assert struct.unpack('>H',z.read(entry)[6:8])[0]==data['java']+44
    for target in targets:
        report=source/'reports'/(target+'-forge' if loader=='forge' else target)
        log=(report/'audit.log').read_text()
        assert ('LOWHEALTH_FORGE_AUDIT_PASS' if loader=='forge' else 'BUILD SUCCESSFUL') in log
        assert len(list(report.glob('*.png'))) >= (14 if loader=='forge' else 15)
    count+=len(targets)
    rows.append(f'| {name} | {loader.title()} | {", ".join(targets)} | {data["java"]} |')
    for p in (jar,manifest):shutil.copy2(p,output/p.name);selected.append(output/p.name)
notes=f'''# Low Health Warning {version}

{len(expected)} release-candidate jars cover {count} runtime-tested Minecraft/loader targets. Install exactly one jar matching your game version and loader. Nothing has been published.

| File | Loader | Tested Minecraft versions | Minimum Java for jar |
| --- | --- | --- | --- |
'''+ '\n'.join(rows)+'''

Fabric needs Loader 0.19.5+, Fabric API and Fabric Language Kotlin. Mod Menu provides the config-menu entry; H opens settings in game. Forge needs Forge 11.15.1.2318 on Minecraft 1.8.9 and Java 8, with no Fabric dependencies. Minecraft 1.20.5+ itself needs Java 21 even when a shared 1.20.x mod jar targets Java 17; 26.x needs Java 25. Exact tested dependencies are in the manifests.

Checks used actual packaged clients, isolated profiles and new test worlds: config controls and persistence, migration, malformed-file backup, Cancel/Escape/Reset, window sizes, all preview modes, live warnings, absorption and game modes, and sound events. Forge also passed its Mods-menu Config entry. Representative rendered screenshots were visually inspected.

The per-target logs and screenshots remain alongside the individual release candidates under ../VERSION/reports/. The zip excludes those large diagnostics. SHA-256 checksums are in the JSON manifests.

Original four verified candidates are preserved separately in backups/verified-1.2.1-rc.1/. The source was restored from the author's published 1.2.0 jars; it was not recovered from an original newer checkout.

Not certified: multiplayer, arbitrary mod packs, human audible playback, or the GitHub-hosted workflow before it actually runs there. No NeoForge build is included.
'''.replace('../VERSION/',f'../{version}/')
(output/'RELEASE-NOTES.md').write_text(notes)
shutil.copy2(ROOT/'CHANGELOG.md',output/'CHANGELOG.md')
selected.extend([output/'RELEASE-NOTES.md',output/'CHANGELOG.md'])
bundle=output.parent/f'low-health-warning-{version}-complete-bundle.zip'
with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED) as z:
    for p in selected:z.write(p,p.name)
with zipfile.ZipFile(bundle) as z:assert z.testzip() is None
print(f'Packaged {len(expected)} jars for {count} tested targets: {bundle}')
