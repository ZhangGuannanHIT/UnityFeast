"""Check shipped JSON references, texture validity and production JAR contents."""
from pathlib import Path
import json, zipfile, hashlib
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NS='unity_feast'
checked=0
for path in RES.rglob('*.json'):
    data=json.loads(path.read_text(encoding='utf-8')); checked+=1
    def walk(value,key=''):
        if isinstance(value,dict):
            for k,v in value.items():
                if k=='textures':
                    for ref in v.values():
                        if ref.startswith(NS+':'): assert (RES/f'assets/{NS}/textures/{ref.split(":",1)[1]}.png').is_file(), ref
                walk(v,k)
        elif isinstance(value,list):
            for v in value: walk(v,key)
        elif isinstance(value,str) and key in ['parent','model'] and value.startswith(NS+':'):
            assert (RES/f'assets/{NS}/models/{value.split(":",1)[1]}.json').is_file(), value
    walk(data)
for path in (RES/f'assets/{NS}/textures').rglob('*.png'):
    im=Image.open(path); im.verify()
    im=Image.open(path); assert im.size==(32,32) and im.mode=='RGBA',str(path)
for id,n in [('table',1),('dumpling',3),('water_beetle_hotpot',1),('unity_heart',1)]:
    recipe=json.loads((RES/f'data/{NS}/recipe/{id}.json').read_text())
    assert recipe['result']=={'id':NS+':'+id,'count':n}
assert len(list((RES/f'data/{NS}/recipe').glob('*.json')))==4
a=json.loads((RES/f'assets/{NS}/items/dumpling.json').read_text())
b=json.loads((RES/f'assets/{NS}/items/table_dumpling.json').read_text())
assert a==b, 'Dumpling and table dumpling must reuse exactly the same item model'
jar=ROOT/'build/libs/unity_feast-1.0.0.jar'
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    classes=[n for n in names if n.endswith('.class')]
    assert all(n.startswith('cn/zgnhit/unityfeast/') and '/test/' not in n for n in classes)
    for path in RES.rglob('*'):
        if path.is_file(): assert path.relative_to(RES).as_posix() in names,str(path)
    for id in ['table','dumpling','table_dumpling','water_beetle','water_beetle_hotpot','unity_heart']:
        assert f'assets/{NS}/items/{id}.json' in names
    assert 'META-INF/neoforge.mods.toml' in names
result=f'PASS: {checked} JSON files; all model and texture references; 6 item definitions; 4 recipes; PNGs; production JAR excludes test code.\nSHA-256: {hashlib.sha256(jar.read_bytes()).hexdigest()}\n'
print(result)
(ROOT/'docs/resource-check.txt').write_text(result,encoding='utf-8')
