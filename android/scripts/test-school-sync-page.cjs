const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app/src/main/java/com/tyust/course/IbarakiPortalActivity.kt', 'utf8');
const script = source.match(/web\.evaluateJavascript\("""([\s\S]*?)"""\.trimIndent\(\)/)[1];
function read(bodyText, labels, password = false) {
  const controls = labels.map(textContent => ({ tagName: 'BUTTON', textContent }));
  const document = {
    body: { textContent: bodyText },
    querySelector: () => password ? {} : null,
    querySelectorAll: selector => selector === 'table' || selector === 'table.rishu-koma-inner' || selector === 'iframe,frame' ? [] : controls
  };
  return vm.runInNewContext(script, { document, location: {
    protocol: 'https:', hostname: 'csweb.ibaraki.ac.jp', pathname: '/campusweb/'
  }});
}
const prompt = '本人連絡先 変更する情報を入力し、変更ボタンをクリックしてください。';
assert.equal(read(prompt, ['変更', '変更なし', 'ログアウト']).contactConfirmation, true);
assert.equal(read(prompt, ['変更', 'ログアウト']).contactConfirmation, false);
assert.equal(read('通常の課表 本人連絡先', ['変更なし']).contactConfirmation, false);
assert.equal(read(prompt, ['変更なし', 'ログアウト']).authenticated, true);
assert.equal(read('', ['ログイン'], true).needsLogin, true);
assert.equal(read(prompt + ' confidential', ['変更なし']).text, '');
console.log('6 school page detection checks passed');

{
 const table={innerText:'T5009\n確率・統計【情報】\nTeacher Name\n2.0単位\n追加登録',getClientRects:()=>[{}]};
 const doc={body:{textContent:'課表'},querySelector:()=>null,querySelectorAll:q=>q==='table.rishu-koma-inner'?[table]:[]};
 const result=vm.runInNewContext(script,{document:doc,location:{protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/'}});
 assert.deepEqual(JSON.parse(JSON.stringify(result.timetableNames)),[{code:'T5009',name:'確率・統計【情報】'}]);
 table.innerText='未登録';
 assert.equal(vm.runInNewContext(script,{document:doc,location:{protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/'}}).timetableNames.length,0);
 console.log('Exact timetable title lines preserved without instructor/action text');
}

{
 const cell=(textContent,rowSpan=1,colSpan=1)=>({textContent,rowSpan,colSpan,cloneNode(){return {textContent,querySelectorAll:()=>[]}}});
 const header=['No.','科目大区分','科目名','単位数','修得年度','修得学期','評語','合否'];
 const table={getClientRects:()=>[{}],rows:[{cells:header.map(v=>cell(v))},
  {cells:[cell('1'),cell('専攻科目',2),cell('研究 A'),cell('2'),cell('2026'),cell('前期'),cell('A'),cell('合')]},
  {cells:['2','研究 B','2','2026','前期','A','合'].map(v=>cell(v))}]};
 const doc={body:{textContent:'成績'},querySelector:()=>null,querySelectorAll:q=>q==='table'?[table]:[]};
 const result=vm.runInNewContext(script,{document:doc,location:{protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/'}});
 assert.ok(result.text.includes('2 | 専攻科目 | 研究 B | 2 | 2026 | 前期 | A | 合'));
 table.rows[1].cells[1].colSpan=2;
 assert.ok(vm.runInNewContext(script,{document:doc,location:{protocol:'https:',hostname:'csweb.ibaraki.ac.jp',pathname:'/campusweb/'}}).text.includes('[unsupported grade colspan]'));
 console.log('Graduate category rowspan expansion and unsupported colspan checks passed');
}
