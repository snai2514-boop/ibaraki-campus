"""Draw the app's code-native book/calendar mark; no private imagery."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]/'CampusAssistant/Assets.xcassets'
icons=root/'AppIcon.appiconset';icons.mkdir(parents=True,exist_ok=True)
(root/'Contents.json').write_text(json.dumps({'info':{'author':'xcode','version':1}}))
im=Image.new('RGB',(1024,1024),'#147cff');d=ImageDraw.Draw(im)
d.rounded_rectangle((206,246,818,804),radius=95,fill='white')
d.rectangle((206,367,818,385),fill='#d9eaff')
for x in (346,678):d.rounded_rectangle((x-22,190,x+22,300),radius=22,fill='white')
for x in (337,491,645):
    for y in (454,595):d.rounded_rectangle((x,y,x+87,y+87),radius=18,fill='#d9eaff')
d.line((505,629,545,666,655,521),fill='#147cff',width=38)
im.save(icons/'AppIcon.png')
(icons/'Contents.json').write_text(json.dumps({'images':[{'filename':'AppIcon.png','idiom':'universal','platform':'ios','size':'1024x1024'}],'info':{'author':'xcode','version':1}}))
print('Generated app icon')
