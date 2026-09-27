"""Run an exported NeoForge runtime with actual frozen mod JARs, never workspace classes.
All instances, Java argument files, logs and screenshots remain inside the project.
"""
import argparse, json, os, pathlib, subprocess, shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser()
p.add_argument('manifest',type=pathlib.Path)
p.add_argument('kind',choices=['client','server','gametest'])
p.add_argument('directory')
p.add_argument('--name',default='FeastOne')
p.add_argument('--connect',default='127.0.0.1:25575')
p.add_argument('--background',action='store_true')
p.add_argument('--testmod',action='store_true')
p.add_argument('--acceptance',action='store_true')
a=p.parse_args()
runtime=json.loads(a.manifest.read_text(encoding='utf-8'))
work=(ROOT/a.directory).resolve()
if not work.is_relative_to(ROOT): raise SystemExit('Run directory must be inside the project')
(work/'mods').mkdir(parents=True,exist_ok=True)
for filename in ['unity_feast-1.0.0.jar']+(['unity_feast-1.0.0-tests.jar'] if a.testmod else []):
    shutil.copy2(ROOT/'build/libs'/filename,work/'mods'/filename)
java=next((ROOT/'.tools/jdk').glob('*/bin/java.exe'))
args=['-Xmx2G','--sun-misc-unsafe-memory-access=allow','--enable-native-access=ALL-UNNAMED','--add-opens=java.base/java.lang.invoke=ALL-UNNAMED','--add-exports=jdk.naming.dns/com.sun.jndi.dns=java.naming','-Dfile.encoding=UTF-8',f'-Djava.io.tmpdir={ROOT / ".tools/tmp"}','-Dneoforge.enableGameTest=true',f'-Dunity_feast.acceptance={str(a.acceptance).lower()}',f'-Dunity_feast.project={ROOT}','-cp',';'.join(runtime['classpath'])]
main={'client':'Client','server':'Server','gametest':'GameTestServer'}[a.kind]
args += ['net.neoforged.fml.startup.'+main]
if a.kind=='client':
    args += ['--version',runtime['neoforge'],'--assetIndex','30','--assetsDir',str(ROOT/'.gradle-user-home/caches/neoformruntime/assets'),'--username',a.name,'--quickPlayMultiplayer',a.connect,'--width','1280','--height','800']
    if not (work/'options.txt').exists():
        (work/'options.txt').write_text('onboardAccessibility:false\nlang:zh_cn\nguiScale:2\npauseOnLostFocus:false\nrenderDistance:5\nsimulationDistance:5\nmaxFps:30\nsoundCategory_master:0.0\n',encoding='utf-8')
elif a.kind=='server': args+=['--nogui']
argfile=work/'java-args.txt'
argfile.write_text('\n'.join('"'+s.replace('\\','\\\\').replace('"','\\"')+'"' for s in args)+'\n',encoding='utf-8')
env=dict(os.environ)
env.pop('MOD_CLASSES',None)
env['TEMP']=env['TMP']=str(ROOT/'.tools/tmp')
if a.background:
    with (work/'console.log').open('w',encoding='utf-8') as out:
        child=subprocess.Popen([str(java),'@'+str(argfile)],cwd=work,env=env,stdout=out,stderr=subprocess.STDOUT,stdin=subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW)
    (work/'process.json').write_text(json.dumps({'pid':child.pid,'java':str(java),'runtime':runtime['neoforge']},indent=2),encoding='utf-8')
    print(f'{a.kind} PID {child.pid}; logs: {work / "console.log"}')
else:
    raise SystemExit(subprocess.call([str(java),'@'+str(argfile)],cwd=work,env=env))
