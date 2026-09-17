const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const T=require('../CampusAssistant/Web/theme-pack.js'),zip=require('../CampusAssistant/Web/vendor/fflate.js');
const palette={primary:'#812AFF',onPrimary:'#FFFFFF',background:'#FAF0FF',surface:'#FFFFFF',text:'#171717',muted:'#555555',accent:'#EAD8FF'};
const config=()=>({format:'ibaraki-campus-theme',version:1,name:'Test',light:{...palette},dark:{...palette}});
const bytes=(c,extra={})=>zip.zipSync({'theme.json':zip.strToU8(JSON.stringify(c)),...extra});
test('independent arbitrary light dark and dialog colors',()=>{const c=config();c.light.dialog='#FF0077';const p=T.parse(bytes(c),zip);assert.equal(p.light.dialog,'#FF0077');assert.equal(p.dark.dialog,palette.surface);});
test('reject traversal, duplicate reference, missing image, illegal icons and size bombs',()=>{
 for(const extra of [{'../theme.json':new Uint8Array(1)},{'script.js':new Uint8Array(1)},{'background.png':new Uint8Array(1000001)}])assert.throws(()=>T.parse(bytes(config(),extra),zip));
 assert.throws(()=>T.parse(bytes({...config(),appIcon:'url'}),zip));
 assert.throws(()=>T.parse(bytes({...config(),startup:{frames:['startup-1.png'],frameDurationMs:90}}),zip));
 assert.throws(()=>T.manifest({...config(),startup:{frames:['startup-1.png','startup-1.png']}}));
});
test('distributed example provides all referenced frames and both palettes',()=>{
 const path=require('node:path').resolve(__dirname,fs.existsSync(require('node:path').resolve(__dirname,'../examples/themes/sky'))?'../examples/themes/sky':'../../examples/themes/sky');
 if(!fs.existsSync(path))return;const files={};for(const f of fs.readdirSync(path))files[f]=new Uint8Array(fs.readFileSync(require('node:path').join(path,f)));
 const p=T.parse(zip.zipSync(files),zip);assert.equal(p.appIcon,'sky');assert.equal(p.frames.length,6);assert.notEqual(p.light.dialog,p.light.surface);
});
