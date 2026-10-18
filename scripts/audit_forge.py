#!/usr/bin/env python3
"""Launch the packaged Forge mod with a separate test fixture in an isolated profile."""
from pathlib import Path
import json,os,subprocess,shutil,hashlib,urllib.request
ROOT=Path(__file__).resolve().parents[1]
project=ROOT/'forge-1.8.9'
env=os.environ.copy()
env['JAVA_HOME']=os.environ.get('JAVA21_HOME',str(ROOT/'build/toolchains/java21/Contents/Home'))
java8=Path(os.environ.get('JAVA8_HOME',str(ROOT/'build/toolchains/java8/Contents/Home')))
subprocess.run([str(project/'gradlew'),'-p',str(project),'writeAuditLaunch',f'-Porg.gradle.java.installations.paths={java8}'],env=env,check=True)
cfg=json.loads((project/'build/audit-launch.json').read_text())
def official_file(url,name,sha1):
    path=ROOT/'build/toolchains'/name
    path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists() or hashlib.sha1(path.read_bytes()).hexdigest()!=sha1:
        with urllib.request.urlopen(url,timeout=60) as response: data=response.read()
        if hashlib.sha1(data).hexdigest()!=sha1: raise RuntimeError('Official download checksum mismatch: '+name)
        path.write_bytes(data)
    return path
for part,sha in [('api','1dd66e68cccd907880229f9e2de1314bd13ff785'),('core','678861ba1b2e1fccb594bb0ca03114bb05da9695')]:
    name=f'log4j-{part}-2.0-beta9.jar'
    official_file(f'https://libraries.minecraft.net/org/apache/logging/log4j/log4j-{part}/2.0-beta9/{name}',name,sha)
official_file('https://launcher.mojang.com/v1/objects/50c9cc4af6d853d9fc137c84bcd153e2bd3a9a82/client-1.7.xml','client-1.7.xml','50c9cc4af6d853d9fc137c84bcd153e2bd3a9a82')
cfg['classpath']=[p for p in cfg['classpath'] if '/org.apache.logging.log4j/' not in p]+[str(ROOT/'build/toolchains'/f'log4j-{part}-2.0-beta9.jar') for part in ('api','core')]
run=ROOT/'build/run/packaged-1.8.9-forge'
(run/'mods').mkdir(parents=True,exist_ok=True)
for p in (run/'mods').glob('*.jar'): p.unlink() # Only this dedicated generated audit profile.
candidate=Path(cfg['candidate']); digest=hashlib.sha256(candidate.read_bytes()).hexdigest()
for key in ('candidate','fixture'):shutil.copy2(cfg[key],run/'mods'/Path(cfg[key]).name)
result=run/'audit-result.txt'
result.unlink(missing_ok=True)
command=[str(java8/'bin/java'),'-Xmx1G','-Djava.library.path='+cfg['natives'],'-Dorg.lwjgl.librarypath='+cfg['natives'],
         '-Dlog4j.configurationFile='+str(ROOT/'build/toolchains/client-1.7.xml'),'-Dlog4j2.formatMsgNoLookups=true','-cp',os.pathsep.join(cfg['classpath']),'net.minecraft.launchwrapper.Launch',
         '--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker','--username','LowHealthAudit','--version','1.8.9',
         '--gameDir',str(run),'--assetsDir',cfg['assets'],'--assetIndex',cfg['assetIndex'],'--accessToken','0','--userProperties','{}']
log=ROOT/'build/audit/packaged-1.8.9-forge.log'
log.parent.mkdir(parents=True,exist_ok=True)
with log.open('w') as stream: completed=subprocess.run(command,cwd=run,stdout=stream,stderr=subprocess.STDOUT,timeout=240)
if completed.returncode or not result.exists() or result.read_text().strip()!='PASS':
 print('\n'.join(log.read_text(errors='replace').splitlines()[-65:]));raise SystemExit(f'Forge audit failed: {log}')
assert hashlib.sha256(candidate.read_bytes()).hexdigest()==digest
release=ROOT/'build/releases/1.2.1-rc.1';release.mkdir(parents=True,exist_ok=True)
name='lowhelthwarning-1.8.9-forge-1.2.1-rc.1.jar';shutil.copy2(candidate,release/name)
(release/Path(name).with_suffix('.json')).write_text(json.dumps({'artifact':name,'version':'1.2.1-rc.1','loader':'forge','runtime_tested':['1.8.9'],'java':8,'forge':'11.15.1.2318','sha256':digest,'status':'release candidate; not published'},indent=2)+'\n')
report=release/'reports/1.8.9-forge';report.mkdir(parents=True,exist_ok=True)
shutil.copy2(log,report/'audit.log')
for p in (run/'screenshots').glob('*.png'):shutil.copy2(p,report/p.name)
print('Forge 1.8.9 packaged audit passed:',release/name)
