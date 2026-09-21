const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const script = fs.readFileSync('app/src/main/assets/school-classrooms.js','utf8');
function run(text, { checked=true, host='csweb.ibaraki.ac.jp' }={}) {
  let clicks=0;
  const clone={textContent:text,querySelectorAll:()=>[]};
  const node={title:'',cloneNode:()=>clone,querySelectorAll:()=>[]};
  const head={textContent:'15Mon',classList:{contains:()=>false}};
  const content={children:text?[node]:[]};
  const cell={querySelector:q=>q==='.cal-head-number'?head:content};
  const table={querySelectorAll:()=>[cell]};
  const check={checked,parentElement:{textContent:'時間割コマ情報'},click:()=>clicks++};
  const document={body:{innerText:'2026年6月'},getElementById:id=>id==='check21'?check:table};
  const value=vm.runInNewContext(script,{document,location:{protocol:'https:',hostname:host,pathname:'/campusweb/'}});
  return {value:JSON.parse(JSON.stringify(value)),clicks};
}
assert.deepEqual(run('T5001 線形代数Ⅰ\n3限\n教室：共通21').value.rows,[{code:'T5001',date:'2026-06-15',period:3,room:'共通21'}]);
assert.deepEqual(run('').value,{ready:true,rows:[],events:[]});
assert.equal(run('',{checked:false}).clicks,1);
assert.equal(run('',{host:'example.com'}).value.ready,false);
for(const text of ['T5001\n3限\n教室：未定','T5001 T5008\n3限\n教室：共通21','T5001\n教室：共通21','T5001\n3限\n教室：共通21\n教室：共通22'])
  assert.deepEqual(run(text).value.rows,[]);
console.log('8 classroom extraction checks passed');

for(const code of ['P0502','PG101','LA0001','AN2208','S1001','R1001','T1018-A']) {
 assert.deepEqual(run(code+' 講義\n3限\n教室：共通21').value.rows,[{code,date:'2026-06-15',period:3,room:'共通21'}]);
}
assert.equal(run('AN2208 講義\n3限\n教室：A101').value.rows.length,1);
console.log('8 cross-faculty classroom extraction checks passed');

assert.deepEqual(run('1限:共生とコミュニケーション【3Q】表現行動と心の健康@共通教育棟２号館２１番教室,遠隔講義').value.rows,
 [{code:'',name:'共生とコミュニケーション【3Q】表現行動と心の健康',date:'2026-06-15',period:1,room:'共通教育棟2号館21番教室'}]);
assert.equal(run('4限:データサイエンス・AI入門【後期】@遠隔講義').value.rows.length,0);
assert.equal(run('4限:データサイエンス・AI入門【後期】@遠隔講義').value.events.length,1);
assert.equal(run('2限:身体活動【後期】チームスポーツ:屋内ボールゲーム@水戸：第1アリーナ（大体育館）').value.rows[0].room,'水戸:第1アリーナ(大体育館)');
assert.equal(run('2限:Course@共通21,共通22').value.rows.length,0);
assert.equal(run('2限:Course@教室未定').value.rows.length,0);
console.log('6 live calendar name/venue checks passed');

{
 let clicks=0;
 const link={textContent:'スケジュール管理',click:()=>clicks++};
 const document={getElementById:()=>null,querySelectorAll:()=>[link]};
 const context={document,location:{protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/'}};
 for(let i=0;i<50;i++) vm.runInNewContext(script,context);
 assert.equal(clicks,1);
 console.log('Repeated calendar navigation is suppressed');
}
