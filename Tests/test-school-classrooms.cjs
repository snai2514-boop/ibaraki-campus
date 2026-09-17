const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const script = fs.readFileSync('CampusAssistant/SchoolScripts/school-classrooms.js','utf8');
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
assert.deepEqual(run('').value,{ready:true,rows:[]});
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
