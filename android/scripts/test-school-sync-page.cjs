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
    querySelectorAll: selector => selector === 'table' || selector === 'iframe,frame' ? [] : controls
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
