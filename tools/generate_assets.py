"""Original Unity Feast pixel art, models and data. Python 3 + Pillow; no downloaded art."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
import random

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
NS = 'unity_feast'

def write(path, data):
    p = RES / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def png(name, draw):
    im = Image.new('RGBA', (32, 32))
    draw(ImageDraw.Draw(im))
    p = RES / f'assets/{NS}/textures/{name}.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)
    return im

def dumpling(d):
    d.polygon([(2,21),(3,17),(6,12),(10,8),(15,6),(20,7),(25,11),(29,18),(30,22),(27,26),(7,27),(3,24)], fill='#7e6957')
    d.polygon([(3,21),(5,17),(8,12),(12,9),(16,8),(20,9),(24,13),(27,19),(28,22),(25,24),(8,25),(4,23)], fill='#d5c2a0')
    d.polygon([(5,19),(8,14),(13,10),(18,10),(23,14),(26,21),(23,23),(8,23)], fill='#fff1d2')
    d.line([(5,20),(9,17),(10,13),(13,16),(14,10),(17,15),(19,10),(21,17),(23,14),(25,20)], fill='#bba27e', width=2)
    d.line([(8,20),(10,19),(13,20),(16,18),(19,20),(23,21)], fill='#fffae9', width=2)

def beetle(d):
    for pts in [[(9,13),(5,10),(2,11)],[(8,18),(4,18),(1,21)],[(10,23),(6,26),(4,29)],[(23,13),(27,10),(30,11)],[(24,18),(28,18),(31,21)],[(22,23),(26,26),(28,29)]]:
        d.line(pts, fill='#242329', width=2)
    d.line([(13,7),(10,3),(8,2)], fill='#323038', width=1)
    d.line([(18,7),(21,3),(23,2)], fill='#323038', width=1)
    d.ellipse((11,5,21,13), fill='#15151c')
    d.ellipse((7,10,25,28), fill='#111117', outline='#3c3942', width=1)
    d.ellipse((9,11,22,25), fill='#25252e')
    d.line([(16,11),(16,26)], fill='#090b11', width=2)
    d.line([(11,13),(10,17),(10,20)], fill='#62616b', width=2)
    d.line([(20,13),(22,17)], fill='#464652', width=1)
    d.point((13,8), fill='#97969d')
    d.point((19,8), fill='#97969d')

def hotpot(d):
    d.polygon([(3,13),(28,13),(26,24),(22,28),(10,28),(6,24)], fill='#3f454f')
    d.polygon([(5,15),(26,15),(24,24),(21,26),(11,26),(8,23)], fill='#839fa6')
    d.polygon([(6,16),(10,19),(12,25),(9,23)], fill='#c7dcdb')
    d.ellipse((2,6,29,19), fill='#323944')
    d.ellipse((3,6,28,17), fill='#d8e5df')
    d.ellipse((5,8,26,16), fill='#a6906d')
    d.ellipse((6,8,25,14), fill='#f0e2b7')
    for x,y,c in [(10,10,'#fff5dc'),(18,12,'#fff5dc'),(22,10,'#c0a57d'),(14,12,'#918068'),(8,12,'#6f8a54'),(20,9,'#638348'),(16,10,'#494548')]:
        d.rectangle((x,y,x+2,y+1), fill=c)
    d.line([(9,4),(8,3),(9,1)], fill='#d7e4db')
    d.line([(21,4),(20,3),(21,1)], fill='#d7e4db')

def heart(d):
    # Hand-drawn 16 px silhouette, enlarged with nearest-neighbour for consistent pixels.
    pattern=['................','................','..###.....###...','.#####...#####..','#######.#######.','###############.','###############.','###############.','.#############..','..###########...','...#########....','....#######.....','.....#####......','......###.......','.......#........','................']
    for y,row in enumerate(pattern):
        for x,c in enumerate(row):
            if c=='#':
                edge = any(nx<0 or nx>=16 or ny<0 or ny>=16 or pattern[ny][nx]!='#' for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)])
                col='#3a151c' if edge else ('#b51e33' if y>8 or x>11 else '#ee3549')
                if (x,y) in [(3,4),(4,4),(3,5),(10,4),(11,4)]: col='#ffb2ad'
                d.rectangle((x*2,y*2,x*2+1,y*2+1),fill=col)

images = [png('item/dumpling',dumpling),png('item/water_beetle',beetle),png('item/water_beetle_hotpot',hotpot),png('item/unity_heart',heart)]
rng=random.Random(214)
def wood(d):
    d.rectangle((0,0,31,31),fill='#9c6238')
    for y in range(32):
        for x in range(32):
            n=rng.randrange(-10,11)
            base=(152,96,53) if y%8 not in [0,7] else ((111,63,36) if y%8==7 else (185,126,72))
            d.point((x,y),fill=tuple(v+n for v in base)+(255,))
    for x,y,l in [(2,3,10),(18,5,8),(5,11,15),(17,19,9),(3,27,18)]:
        d.line((x,y,x+l,y),fill='#7d492c')
        d.line((x+1,y+1,x+l-2,y+1),fill='#b97b48')
    for x,y in [(8,15),(24,7),(16,31)]: d.line((x,y-6,x,y),fill='#6f412b')
png('block/table_wood',wood)
def dough(d):
    d.rectangle((0,0,31,31),fill='#f4e4c4')
    for x,y in [(3,5),(14,9),(23,18),(8,26),(28,4),(19,28)]:
        d.point((x,y),fill='#ead7b4')
        d.point((x+1,y),fill='#fff0d1')
png('block/dumpling_dough',dough)

def cube(a,b,tex):
    return {'from':a,'to':b,'faces':{f:{'texture':tex} for f in ['up','down','north','south','east','west']}}
elements=[cube([0,11,0],[16,13,16],'#wood')]
for x,z in [(1,1),(12,1),(1,12),(12,12)]: elements.append(cube([x,0,z],[x+3,11,z+3],'#wood'))
for a,b in [([2,9,2],[14,11,3]),([2,9,13],[14,11,14]),([2,9,3],[3,11,13]),([13,9,3],[14,11,13])]: elements.append(cube(a,b,'#wood'))
write(f'assets/{NS}/models/block/table.json',{'parent':'minecraft:block/block','textures':{'wood':f'{NS}:block/table_wood','particle':f'{NS}:block/table_wood'},'elements':elements,'display':{'gui':{'rotation':[30,225,0],'translation':[0,1,0],'scale':[0.65,0.65,0.65]},'fixed':{'rotation':[0,0,0],'scale':[0.6,0.6,0.6]}}})
parts=[{'apply':{'model':f'{NS}:block/table'}}]
for name,x,z in [('nw',4,4),('ne',12,4),('se',12,12),('sw',4,12)]:
    # Narrow slices form a tapered half-moon belly, with a pinched pleated ridge.
    els=[]
    for dx,width,height in [(-2.4,.45,.55),(-1.6,.95,1.15),(-.8,1.3,1.7),(0,1.45,2.0),(.8,1.3,1.7),(1.6,.95,1.15),(2.4,.45,.55)]:
        els.append(cube([x+dx-.4,13,z-.6],[x+dx+.4,13+height,z+width],'#dough'))
        pleat=cube([x+dx-.13,13+height-.3,z-.72],[x+dx+.13,13+height+.4,z+.2],'#dough')
        pleat['rotation']={'origin':[x+dx,13+height,z],'axis':'z','angle':22.5 if dx<0 else -22.5}
        els.append(pleat)
    write(f'assets/{NS}/models/block/dumpling_{name}.json',{'textures':{'dough':f'{NS}:block/dumpling_dough','particle':f'{NS}:block/dumpling_dough'},'elements':els})
    parts.append({'when':{f'dumpling_{name}':'true'},'apply':{'model':f'{NS}:block/dumpling_{name}'}})
write(f'assets/{NS}/blockstates/table.json',{'multipart':parts})
ids=['table','dumpling','table_dumpling','water_beetle','water_beetle_hotpot','unity_heart']
for name in ids:
    model='dumpling' if name=='table_dumpling' else name
    modelref=f'{NS}:block/table' if name=='table' else f'{NS}:item/{model}'
    write(f'assets/{NS}/items/{name}.json',{'model':{'type':'minecraft:model','model':modelref}})
    if name not in ['table','table_dumpling']:
        write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{NS}:item/{name}'}})

zh=['桌子','饺子','桌饺','水蟑螂','清水龙虱打边炉','团结之心']
en=['Table','Dumpling','Table Dumpling','Water Beetle','Water Beetle Hotpot','Heart of Unity']
for locale,names in [('zh_cn',zh),('en_us',en)]:
    lang={(('block' if i=='table' else 'item')+f'.{NS}.{i}'):n for i,n in zip(ids,names)}
    lang[f'itemGroup.{NS}']='团结餐桌' if locale=='zh_cn' else 'Unity Feast'
    tips=(['普通饺子上桌，空手取回桌饺','清除负面效果；伤害吸收、生命恢复 15 秒','每颗永久增加 2 点生命上限；死亡是否保留遵循死亡不掉落规则'] if locale=='zh_cn' else ['Place dumplings; use an empty hand to take table dumplings','Removes harmful effects; Absorption and Regeneration for 15s','Adds 2 maximum health per heart; death retention follows keepInventory'])
    for id,tip in zip(['table','table_dumpling','unity_heart'],tips): lang[f'tooltip.{NS}.{id}']=tip
    write(f'assets/{NS}/lang/{locale}.json',lang)

def recipe(name,data,ingredient):
    write(f'data/{NS}/recipe/{name}.json',data)
    write(f'data/{NS}/advancement/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_ingredient':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':ingredient}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':f'{NS}:{name}'}}},'requirements':[['has_ingredient','has_the_recipe']],'rewards':{'recipes':[f'{NS}:{name}']}})
recipe('table',{'type':f'{NS}:same_planks_table','category':'building','pattern':['PPP','S S','S S'],'key':{'P':'#minecraft:planks','S':'minecraft:stick'},'result':{'id':f'{NS}:table','count':1}},'#minecraft:planks')
recipe('dumpling',{'type':'minecraft:crafting_shaped','category':'misc','pattern':[' P ','WWW'],'key':{'P':'minecraft:porkchop','W':'minecraft:wheat'},'result':{'id':f'{NS}:dumpling','count':3}},'minecraft:wheat')
recipe('water_beetle_hotpot',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['BBB','WWW'],'key':{'B':f'{NS}:water_beetle','W':'minecraft:wheat'},'result':{'id':f'{NS}:water_beetle_hotpot','count':1}},f'{NS}:water_beetle')
recipe('unity_heart',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[f'{NS}:table_dumpling',f'{NS}:water_beetle_hotpot'],'result':{'id':f'{NS}:unity_heart','count':1}},f'{NS}:table_dumpling')
pools=[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{NS}:table'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]
for slot in ['nw','ne','se','sw']:
    pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':f'{NS}:dumpling'}],'conditions':[{'condition':'minecraft:block_state_property','block':f'{NS}:table','properties':{f'dumpling_{slot}':'true'}},{'condition':'minecraft:survives_explosion'}]})
write(f'data/{NS}/loot_table/blocks/table.json',{'type':'minecraft:block','pools':pools})
write('data/minecraft/tags/block/mineable/axe.json',{'replace':False,'values':[f'{NS}:table']})
# Early 26.1 uses an index; later 26.1.2 treats every file as a modifier.
# This dual-format index is also a valid inert modifier (table-id condition never matches).
write('data/neoforge/loot_modifiers/global_loot_modifiers.json',{'replace':False,'entries':[f'{NS}:water_beetle_fishing'],'type':f'{NS}:water_beetle_fishing','conditions':[{'condition':'neoforge:loot_table_id','loot_table_id':f'{NS}:compatibility_index_never'}]})
write(f'data/{NS}/loot_modifiers/water_beetle_fishing.json',{'type':f'{NS}:water_beetle_fishing','conditions':[{'condition':'neoforge:loot_table_id','loot_table_id':'minecraft:gameplay/fishing'}]})

preview=Image.new('RGBA',(32*4,32),(35,41,46,255))
for i,im in enumerate(images): preview.alpha_composite(im,(i*32,0))
(ROOT/'docs').mkdir(exist_ok=True)
preview.resize((768,192),Image.Resampling.NEAREST).save(ROOT/'docs/asset-preview.png')
print('Generated original textures, models, recipes, advancements and loot resources.')
