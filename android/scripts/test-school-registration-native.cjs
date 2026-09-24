const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('app/src/main/assets/school-registration-native.js','utf8');
function fixture() {
  let page, submissions=0, year='2026', identity='A123';
  const storage=new Map(), sessionStorage={getItem:k=>storage.get(k),setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)};
  const location={protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/',origin:'https://csweb.ibaraki.ac.jp',href:'https://csweb.ibaraki.ac.jp/campusweb/campussquare.do'};
  const cell=textContent=>({textContent});
  function node(text,onclick,click=()=>{}) {return {tagName:'INPUT',value:text,textContent:text,disabled:false,getAttribute:k=>k==='onclick'?onclick:null,getClientRects:()=>[{}],click};}
  let form,table,button,rows,links,registered=[],closed=false;
  function grid() {
    links=[1,5,6].map(p=>node('未登録',`return SelectCallA('1','${p}')`,()=>list(p)));
    page={body:{textContent:`履修登録・登録状況照会 学生番号 ${identity} 年度・学期 ${year}年度 3クォーター ${closed?'履修登録期間外':''}`},
      querySelector:q=>q==='table.rishu-koma'?{}:null,
      querySelectorAll:q=>q==='table.rishu-koma-inner'?registered.map(code=>({textContent:code+' 科目 教員 2.0単位'})):q==='table.rishu-etc tr'?[]:links};
  }
  function list(period=1) {
    form={action:location.href,querySelectorAll:q=>q==='table'?[table]:[]};
    button=node('　登録　',`rishuInsert('${year}','15','R2014');`,()=>submissions++);button.form=form;
    const header={textContent:'開講学部 学期 開講 曜日・時限 時間割コード 科目 担当 遠隔授業 コマ重複可 気になる',cells:['開講学部','学期','開講','曜日・時限','時間割コード','科目','担当','遠隔授業','コマ重複可','気になる',''].map(cell)};
    rows=[{cells:['学部','3Q','後期','月'+period,'R2014','プログラミングⅠ','教員','○','×','','登録'].map(cell),querySelectorAll:()=>[button]}];
    rows.forEach(r=>Object.defineProperty(r,'textContent',{get:()=>r.cells.map(c=>c.textContent).join(' ')}));
    table={get rows(){return [header,...rows];}};
    const back=node('履修登録画面に戻る','komaDisp()',grid);back.form=form;
    page={body:{textContent:`履修登録・登録状況照会 月曜日${period}限で履修登録する科目を選択してください`},
      querySelector:q=>q==='form#rishuReferSelectForm'?form:null,
      querySelectorAll:()=>[button,back]};
  }
  function run(r={}) {return vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify({action:'read',student:'A123',...r})),{document:page,location,sessionStorage,URL,Date});}
  function open() {const g=run();assert.equal(g.kind,'grid');assert.equal(run({action:'open',scope:g.scope,slot:'1-1'}).kind,'navigated');return run({scope:g.scope,slot:'1-1'});}
  grid();
  return {run,open,grid,list,location,storage,submissions:()=>submissions,get rows(){return rows},get form(){return form},get button(){return button},get page(){return page},setIdentity:s=>identity=s,setYear:s=>year=s,setRegistered:s=>registered=s,setClosed:s=>closed=s};
}
let checks=0;function check(name,f){f();checks++;console.log('PASS '+name);}
check('grid discovers fifth but does not add sixth period',()=>{const f=fixture(),g=f.run();assert.equal(g.scope,'2026-Q3');assert.deepEqual(Array.from(g.slots,s=>s.key),['1-1','1-5']);assert.equal(f.submissions(),0);});
check('read-only collection preserves teacher, remote marker and missing credits',()=>{const f=fixture(),s=f.open(),r=s.rows[0];assert.equal(s.kind,'list');assert.equal(r.name,'プログラミングⅠ');assert.equal(r.teacher,'教員');assert.equal(r.faculty,'学部');assert.equal(r.remote,'○');assert.equal(r.credits,'');assert.equal(r.available,true);assert.equal(f.submissions(),0);});
check('exact approved course dispatches once; repeated request cannot dispatch',()=>{const f=fixture(),s=f.open(),r=s.rows[0],q={action:'submit',scope:s.scope,slot:'1-1',id:r.id,signature:r.signature};assert.equal(f.run(q).kind,'sent');assert.equal(f.run(q).kind,'error');assert.equal(f.submissions(),1);});
check('changed course details reject old approval without submitting',()=>{const f=fixture(),s=f.open(),r=s.rows[0];f.rows[0].cells[5].textContent='別科目';assert.equal(f.run({action:'submit',scope:s.scope,id:r.id,signature:r.signature}).kind,'error');assert.equal(f.submissions(),0);});
check('candidate page without verified grid session is rejected',()=>{const f=fixture();f.list();assert.equal(f.run().kind,'error');assert.equal(f.submissions(),0);});
check('expired grid session is rejected',()=>{const f=fixture();f.open();const k='campus-native-registration-context',c=JSON.parse(f.storage.get(k));c.time=1;f.storage.set(k,JSON.stringify(c));assert.equal(f.run().kind,'error');});
check('wrong account never opens or submits courses',()=>{const f=fixture();f.setIdentity('B456');f.grid();assert.equal(f.run({action:'open',slot:'1-1'}).kind,'error');assert.equal(f.submissions(),0);});
check('changed scope never opens a course',()=>{const f=fixture();assert.equal(f.run({action:'open',scope:'2025-Q3',slot:'1-1'}).kind,'error');});
check('wrong returned time slot is rejected',()=>{const f=fixture();f.open();assert.equal(f.run({slot:'2-1'}).kind,'error');});
check('closed registration exposes no available slot',()=>{const f=fixture();f.setClosed(true);f.grid();assert.equal(f.run().slots.length,0);assert.equal(f.run({action:'open',slot:'1-1'}).kind,'error');});
check('registered courses are never offered for duplicate registration',()=>{const f=fixture();f.setRegistered(['R2014']);f.grid();assert.equal(f.open().rows[0].available,false);});
check('external submission address rejected',()=>{const f=fixture();f.open();f.form.action='https://example.com/submit';assert.equal(f.run().kind,'error');assert.equal(f.submissions(),0);});
check('mismatched course code in handler rejected',()=>{const f=fixture();f.open();f.rows[0].cells[4].textContent='S1111';assert.equal(f.run().kind,'error');});
check('malformed table cannot be mistaken for empty available list',()=>{const f=fixture();f.open();f.rows[0].cells.pop();assert.equal(f.run().kind,'error');});
check('explicit empty table is valid',()=>{const f=fixture();f.open();f.rows.splice(0,1,{cells:[{textContent:'該当するデータはありません'}],textContent:'該当するデータはありません'});assert.equal(f.run().kind,'list');assert.equal(f.run().rows.length,0);});
check('another origin cannot read or submit',()=>{const f=fixture();f.location.hostname='example.com';assert.equal(f.run().kind,'error');});
check('safe back navigation returns current enrolled codes',()=>{const f=fixture();f.open();assert.equal(f.run({action:'back'}).kind,'navigated');assert.equal(f.run().kind,'grid');assert.equal(f.submissions(),0);});
check('future school year is derived from the live grid',()=>{const f=fixture();f.setYear('2027');f.grid();const s=f.open();assert.equal(s.scope,'2027-Q3');assert.equal(s.rows[0].available,true);});
check('duplicate course identity fails closed',()=>{const f=fixture();f.open();f.rows.push(f.rows[0]);assert.equal(f.run().kind,'error');assert.equal(f.submissions(),0);});
check('duplicate read-only menu entries use the verified registration flow',()=>{
  const f=fixture();let clicks=0;
  const menu={tagName:'A',textContent:'履修登録・登録状況照会',getClientRects:()=>[],getAttribute:()=>'/campusweb/campussquare.do?_flowId=RSW0001000-flow',click(){clicks++;}};
  f.page.body.textContent='HOME';f.page.querySelector=()=>null;f.page.querySelectorAll=()=>[menu,{...menu}];
  assert.equal(f.run({action:'start'}).kind,'navigated');assert.equal(clicks,1);assert.equal(f.submissions(),0);
});
console.log(`${checks} native registration checks passed`);
