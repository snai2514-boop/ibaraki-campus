const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('CampusAssistant/SchoolScripts/school-syllabus.js','utf8');
function navigate(code, offered=code) {
 const request={year:2026,quarter:2,code};
 const location={origin:'https://csweb.ibaraki.ac.jp',pathname:'/campusweb/',href:''};
 const document={querySelector:s=>s==='#main-func-body'?{textContent:'2026年度 2クォーター'}:null,
 querySelectorAll:()=>[{getAttribute:()=>`syRefer('2026','05','${offered}')`}]};
 const result=vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify(request)),{document,location});
 return {result,location};
}
for(const code of ['T1018-A','T1066-H32B','P0502','PG101','LA0001','AN2208','R2001','S1001']) {
 assert.ok(navigate(code).location.href.endsWith('jikanwaricd='+code));
}
assert.equal(navigate('T1018-A','T1018-B').result.error,true);
assert.equal(navigate('../secret').result.error,true);
console.log('10 cross-faculty syllabus navigation checks passed');

function fields(values) {return {querySelectorAll:()=>Object.entries(values).map(([label,value])=>({cells:[{textContent:label},{textContent:value}]}))};}
const delivery='オンライン授業（リアルタイム配信型） ／on-line course (real time)';
const basic=fields({'時間割コード':'KB9999','開講科目名':'表現行動と心の健康'});
const details=fields({'オンライン授業／対面授業／on-line course/face-to-face course/blended course':delivery,'成績の評価方法':'レポート 50%'});
const plan={querySelectorAll:()=>[]};
const document={querySelector:s=>({'#tabs-1':basic,'#tabs-2':details,'#tabs-3':plan}[s]),querySelectorAll:()=>[]};
const location={origin:'https://csweb.ibaraki.ac.jp',pathname:'/campusweb/'};
const result=vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify({code:'KB9999'})),{document,location});
assert.equal(result.delivery,delivery);
assert.ok(!result.delivery.includes('blended'));
assert.equal(vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify({code:'OTHER'})),{document,location}).error,true);
console.log('3 syllabus delivery-value checks passed');
