const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict');
const source=fs.readFileSync('CampusAssistant/SchoolScripts/school-registration.js','utf8');
function fixture(options={}) {
  let clicks=0;
  const form={action:'https://csweb.ibaraki.ac.jp/campusweb/submit',querySelectorAll:()=>boxes};
  const visible=()=>[{}];
  const boxes=[0,1].map(()=>({checked:false,disabled:false,getClientRects:visible,form,dispatchEvent(){}}));
  const cell=textContent=>({textContent});
  const header={textContent:'科目コード 科目名 曜日時限 単位 状況',cells:['科目コード','科目名','曜日時限','単位','状況'].map(cell)};
  const rows=['ABC123','ABC456'].map((code,i)=>({cells:[code,'课程'+i,'金 '+(i+1),'2',''].map(cell),querySelectorAll:()=>[boxes[i]]}));
  const table={querySelectorAll:()=>[header,...rows]};
  const button={tagName:'BUTTON',textContent:'登録',getClientRects:visible,form,click(){clicks++;}};
  const document={body:{textContent:'学生番号 A123 2026年度 3クォーター 履修登録・登録状況照会'},querySelectorAll(selector) {
    if(selector==='iframe,frame')return [];
    if(selector==='table')return [table];
    return options.ambiguous?[button,{...button}]:[button];
  }};
  const location={protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/',origin:'https://csweb.ibaraki.ac.jp'};
  function run(request={action:'read',student:'A123'}) {return vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify(request)),{location,document,URL});}
  return {run,boxes,rows,document,location,form,button,clicks:()=>clicks};
}
let checks=0;
function check(name,fn){fn();checks++;console.log('PASS '+name);}
check('read extracts available rows without mutations',()=>{const f=fixture(),s=f.run();assert.equal(s.rows.length,2);assert.ok(s.rows.every(r=>r.available));assert.equal(f.clicks(),0);assert.ok(f.boxes.every(b=>!b.checked));});
check('confirmed batch clicks once and exactly selected boxes',()=>{const f=fixture(),s=f.run();assert.ok(f.run({action:'submit',student:'A123',signature:s.signature,ids:[s.rows[1].id]}).sent);assert.equal(f.clicks(),1);assert.equal(f.boxes[0].checked,false);assert.equal(f.boxes[1].checked,true);});
check('stale signature is rejected without a click',()=>{const f=fixture(),s=f.run();f.rows[0].cells[1].textContent='Changed';assert.equal(f.run({action:'submit',signature:s.signature,ids:[s.rows[0].id]}).ready,false);assert.equal(f.clicks(),0);});
check('wrong live account rejects read',()=>{const f=fixture();assert.equal(f.run({action:'read',student:'B456'}).ready,false);});
check('wrong live account rejects submission',()=>{const f=fixture(),s=f.run();assert.equal(f.run({action:'submit',student:'B456',signature:s.signature,ids:[s.rows[0].id]}).ready,false);assert.equal(f.clicks(),0);});
check('closed period never offers registration',()=>{const f=fixture();f.document.body.textContent+=' 登録期間外';assert.ok(f.run().rows.every(r=>!r.available));});
check('missing year or term never enables registration',()=>{const f=fixture();f.document.body.textContent='学生番号 A123 履修登録';assert.ok(f.run().rows.every(r=>!r.available));});
check('ambiguous buttons disable availability',()=>{assert.ok(fixture({ambiguous:true}).run().rows.every(r=>!r.available));});
check('duplicate selected ids rejected',()=>{const f=fixture(),s=f.run();assert.equal(f.run({action:'submit',signature:s.signature,ids:[s.rows[0].id,s.rows[0].id]}).ready,false);assert.equal(f.clicks(),0);});
check('empty selected ids rejected',()=>{const f=fixture(),s=f.run();assert.equal(f.run({action:'submit',signature:s.signature,ids:[]}).ready,false);});
check('registered row is not eligible',()=>{const f=fixture();f.rows[0].cells[4].textContent='登録済';assert.equal(f.run().rows[0].available,false);});
check('prechecked unrelated choice prevents submitting',()=>{const f=fixture();f.boxes[0].checked=true;const s=f.run();assert.equal(f.run({action:'submit',signature:s.signature,ids:[s.rows[1].id]}).ready,false);assert.equal(f.clicks(),0);});
check('cross origin cannot read',()=>{const f=fixture();f.location.hostname='example.com';assert.equal(f.run().ready,false);});
check('same request cannot repeat',()=>{const f=fixture(),s=f.run(),r={action:'submit',signature:s.signature,ids:[s.rows[0].id]};f.run(r);f.run(r);assert.equal(f.clicks(),1);});
check('foreign form action rejects without mutation',()=>{const f=fixture(),s=f.run();f.form.action='https://example.com/submit';assert.equal(f.run({action:'submit',signature:s.signature,ids:[s.rows[0].id]}).ready,false);assert.equal(f.clicks(),0);assert.equal(f.boxes[0].checked,false);});
check('dashboard without student number may only navigate read-only menu',()=>{const f=fixture();f.document.body.textContent='学校首页';f.button.textContent='履修登録・登録状況照会';assert.equal(f.run().navigated,true);assert.equal(f.clicks(),1);assert.ok(f.boxes.every(b=>!b.checked));});
check('missing student number cannot submit or navigate during submission',()=>{const f=fixture();f.document.body.textContent='学校首页';f.button.textContent='履修登録・登録状況照会';assert.equal(f.run({action:'submit',student:'A123',ids:['A']}).ready,false);assert.equal(f.clicks(),0);});
check('unchecked or unknown status is never successful enrollment',()=>{const f=fixture(),s=f.run();assert.equal(f.run({action:'result',ids:s.rows.map(r=>r.id),scope:s.scope}).result,'unconfirmed');assert.equal(f.clicks(),0);});
check('success requires all submitted courses explicitly registered',()=>{const f=fixture(),s=f.run();f.rows[0].cells[4].textContent='登録済';const req={action:'result',ids:s.rows.map(r=>r.id),scope:s.scope};assert.equal(f.run(req).result,'unconfirmed');f.rows[1].cells[4].textContent='登録済';assert.equal(f.run(req).result,'success');});
check('school confirmation submits only exact approved list once',()=>{const f=fixture();f.button.textContent='確定';f.document.body.textContent+=' 履修登録内容確認';const s=f.run(),r={action:'result',ids:s.rows.map(r=>r.id),scope:s.scope};assert.equal(f.run(r).result,'confirm');assert.equal(f.run({...r,action:'confirm',signature:s.signature}).sent,true);assert.equal(f.run({...r,action:'confirm',signature:s.signature}).ready,false);assert.equal(f.clicks(),1);});
check('extra course on school confirmation prevents automatic confirmation',()=>{const f=fixture();f.button.textContent='確定';f.document.body.textContent+=' 履修登録内容確認';const s=f.run();assert.equal(f.run({action:'result',ids:[s.rows[0].id],scope:s.scope}).result,'unconfirmed');assert.equal(f.clicks(),0);});
console.log(`${checks} registration checks passed`);
