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
