/* Theme pack v1: local data, no CSS, scripts, network URLs, or archive path extraction. */
(function(root){
 'use strict';
 const keys=['primary','onPrimary','background','surface','text','muted','accent'];
 function manifest(value){
  if(value?.format!=='ibaraki-campus-theme'||value.version!==1)throw Error('不支持的美化包格式或版本');
  const name=typeof value.name==='string'?value.name.trim():'',author=typeof value.author==='string'?value.author.trim():'';
  if(!name||name.length>60||author.length>80)throw Error('名称或作者过长');
  const palette=id=>{const p=Object.fromEntries(keys.map(k=>{const c=value[id]?.[k];if(typeof c!=='string'||!/^#[0-9a-fA-F]{6}$/.test(c))throw Error('颜色必须为 #RRGGBB');return [k,c];}));p.dialog=value[id].dialog??p.surface;if(!/^#[0-9a-fA-F]{6}$/.test(p.dialog))throw Error('弹窗颜色必须为 #RRGGBB');return p;};
  const background=value.background||'';
  if(!['','background.png','background.jpg'].includes(background))throw Error('背景必须是包内 PNG 或 JPEG');
  const appIcon=value.appIcon??'default';if(!['default','sky','night'].includes(appIcon))throw Error('不支持的桌面图标名称');
  let startup=null;if(value.startup!=null){const frames=value.startup.frames,d=value.startup.frameDurationMs??150;if(!Array.isArray(frames)||frames.length<1||frames.length>8||new Set(frames).size!==frames.length||frames.some(f=>typeof f!=='string'||!/^startup-[1-8]\.png$/.test(f))||!Number.isInteger(d)||d<80||d>350)throw Error('启动动画格式无效');startup={frames,frameDurationMs:d};}
  return {format:value.format,version:1,name,author,appIcon,startup,light:palette('light'),dark:palette('dark'),background};
 }
 function parse(bytes,zip=root.fflate){
  if(bytes.length<22||bytes.length>2000000)throw Error('美化包须为 2 MB 以内的 ZIP');
  let expanded=0;const files=new Map(),allowed=['theme.json','background.png','background.jpg'];
  const unzip=new zip.Unzip(file=>{
   if((!allowed.includes(file.name)&&!/^startup-[1-8]\.png$/.test(file.name))||files.has(file.name))throw Error('包内含多余或重复文件');
   const limit=file.name==='theme.json'?16384:file.name.startsWith('startup-')?128000:1000000;
   if(file.originalSize>limit)throw Error('主题配置或背景图片过大');
   const chunks=[];let length=0;files.set(file.name,null);
   file.ondata=(error,chunk,final)=>{if(error)throw error;length+=chunk.length;expanded+=chunk.length;if(expanded>2000000)throw Error('解压后的美化包超过 2 MB');if(length>limit)throw Error('主题配置或背景图片过大');chunks.push(chunk);if(final){const data=new Uint8Array(length);let i=0;for(const c of chunks){data.set(c,i);i+=c.length;}files.set(file.name,data);}};
   file.start();
  });
  unzip.register(zip.UnzipInflate);
  for(let i=0;i<bytes.length;i+=256)unzip.push(bytes.subarray(i,i+256),i+256>=bytes.length);
  if(!files.get('theme.json')||[...files.values()].some(v=>v===null))throw Error('美化包不完整');
  const result=manifest(JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(files.get('theme.json'))));
  const expected=['theme.json',...(result.background?[result.background]:[]),...(result.startup?.frames||[])];if(files.size!==expected.length||expected.some(f=>!files.has(f)))throw Error('图片缺失或包含多余文件');
  return {...result,image:result.background?files.get(result.background):null,frames:(result.startup?.frames||[]).map(f=>files.get(f))};
 }
 root.CampusThemePack={manifest,parse};if(typeof module!=='undefined')module.exports=root.CampusThemePack;
})(globalThis);

function applyThemePack(pack=state.themePack){
 const root=document.documentElement;
 if(!pack){delete root.dataset.themePack;for(const k of ['blue','ink','muted','line','pack-bg','pack-surface','pack-dialog','pack-button-text','pack-accent','pack-image'])root.style.removeProperty('--'+k);return;}
 const valid=CampusThemePack.manifest(pack),p=matchMedia('(prefers-color-scheme: dark)').matches?valid.dark:valid.light;
 root.dataset.themePack='1';for(const [k,v] of Object.entries({blue:p.primary,ink:p.text,muted:p.muted,line:p.accent,'pack-bg':p.background,'pack-surface':p.surface,'pack-dialog':p.dialog,'pack-button-text':p.onPrimary,'pack-accent':p.accent}))root.style.setProperty('--'+k,v);
 root.style.setProperty('--pack-image',/^data:image\/(png|jpeg);base64,[A-Za-z0-9+/=]+$/.test(pack.imageURL||'')?'url("'+pack.imageURL+'")':'none');
}
async function previewThemePack(bytes){
 const p=CampusThemePack.parse(bytes);let imageURL=null;
 if(p.image){let raw='';for(const b of p.image)raw+=String.fromCharCode(b);imageURL='data:image/'+(p.background.endsWith('.png')?'png':'jpeg')+';base64,'+btoa(raw);
  const img=new Image();await new Promise((resolve,reject)=>{img.onload=resolve;img.onerror=()=>reject(Error('背景图片无法读取'));img.src=imageURL;});
  if(img.naturalWidth>4096||img.naturalHeight>4096||img.naturalWidth*img.naturalHeight>8000000)throw Error('背景图片尺寸过大');
 }
 const frameURLs=[];for(const bytes of p.frames){let raw='';for(const b of bytes)raw+=String.fromCharCode(b);const url='data:image/png;base64,'+btoa(raw),img=new Image();await new Promise((resolve,reject)=>{img.onload=resolve;img.onerror=()=>reject(Error('动画图片无法读取'));img.src=url;});if(img.naturalWidth>1024||img.naturalHeight>1024)throw Error('动画图片超过 1024 像素');frameURLs.push(url);}
 const pack={...CampusThemePack.manifest(p),imageURL,frameURLs};applyThemePack(pack);
 const showPreview=()=>modal('美化包预览',`<p>${h(pack.name)} · ${h(pack.author)}</p><p>正在预览，应用后保存。桌面图标：${h(pack.appIcon)}；动画 ${pack.frameURLs.length} 帧。</p>`,[['取消',()=>{applyThemePack();}],['预览动画',()=>playThemeStartup(pack,showPreview)],['应用',()=>saveThemePack(pack)]]);showPreview();
 $('dialog').addEventListener('cancel',()=>applyThemePack(),{once:true});
}
function themeSettings(){
 modal('主题与美化包',`<p>当前：${h(state.themePack?.name||'默认外观')}</p><p>导入这个 App 的 ZIP 美化包，支持浅色、深色任意配色、背景、弹窗、内置桌面图标及启动动画。</p>`,[['恢复默认',()=>saveThemePack(null)],['制作教程',()=>bridge('openExternal',{url:'https://github.com/snai2514-boop/ibaraki-campus/blob/main/THEME-PACKS.md'})],['导入 ZIP',async()=>{
  if(native){const base64=await bridge('importTheme');await previewThemePack(Uint8Array.from(atob(base64),c=>c.charCodeAt(0)));}
  else{document.getElementById('theme-file')?.remove();const input=document.createElement('input');input.id='theme-file';input.type='file';input.accept='.zip';input.hidden=true;document.body.append(input);input.onchange=async()=>{try{const file=input.files[0];if(!file)return;if(file.size>2000000)throw Error('ZIP 超过 2 MB');await previewThemePack(new Uint8Array(await file.arrayBuffer()));}catch(e){applyThemePack();toast(e.message);}};input.click();}
 }],['关闭',()=>{}]]);
}
if(typeof matchMedia==='function')matchMedia('(prefers-color-scheme: dark)').addEventListener('change',()=>{if(typeof state!=='undefined')applyThemePack();});

async function saveThemePack(pack){
 const old=state.themePack;try{if(native)await bridge('setAppIcon',{icon:pack?.appIcon||'default'});state.themePack=pack;await persist();render();}
 catch(e){state.themePack=old;applyThemePack();if(native)try{await bridge('setAppIcon',{icon:old?.appIcon||'default'});}catch(rollback){toast('图标恢复失败，请在主题设置重新应用');}throw e;}
}
function playThemeStartup(pack=state.themePack,onFinish=()=>{}){
 if(!pack?.startup||!pack.frameURLs?.length||matchMedia('(prefers-reduced-motion: reduce)').matches){onFinish();return;}
 const valid=CampusThemePack.manifest(pack),urls=pack.frameURLs;
 if(urls.length!==valid.startup.frames.length||urls.some(u=>!/^data:image\/png;base64,[A-Za-z0-9+/=]+$/.test(u))){onFinish();return;}
 document.getElementById('theme-startup')?.remove();const overlay=document.createElement('div');overlay.id='theme-startup';overlay.setAttribute('role','dialog');overlay.setAttribute('aria-label','启动动画');
 const img=document.createElement('img');img.alt='';const skip=document.createElement('button');skip.textContent='跳过';overlay.append(img,skip);document.body.append(overlay);let index=0,timer;
 const finish=()=>{clearTimeout(timer);overlay.remove();onFinish();};skip.onclick=finish;overlay.onkeydown=e=>{if(e.key==='Escape')finish();};skip.focus();
 const next=()=>{if(!overlay.isConnected)return;if(index>=urls.length){finish();return;}img.src=urls[index++];timer=setTimeout(next,valid.startup.frameDurationMs);};next();
}
