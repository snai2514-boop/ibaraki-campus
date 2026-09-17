const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const script = fs.readFileSync('CampusAssistant/SchoolScripts/school-sync-navigation.js', 'utf8');
function run(target, { labels = [], allGrades = false, host = 'csweb.ibaraki.ac.jp', repeat = 1, hidden = false } = {}) {
  const clicks = [];
  const nodes = labels.map(label => ({ tagName: 'BUTTON', textContent: label, disabled: false,
    getClientRects: () => hidden ? [] : [1], click: () => clicks.push(label) }));
  const radio = { parentElement: { textContent: '過去を含めた全成績' }, click: () => clicks.push('all-grades') };
  const document = { querySelectorAll: selector => selector === 'iframe,frame' ? [] :
    selector === 'input[type=radio]' ? (allGrades ? [radio] : []) : nodes };
  let result;
  for(let i = 0; i < repeat; i++) result = vm.runInNewContext(script.replace('__TARGET__', JSON.stringify(target)), {
    document, location: { protocol: 'https:', hostname: host, pathname: '/campusweb/', origin: 'https://' + host }
  });
  return { result, clicks };
}
assert.deepEqual(run('grades', { labels: ['単位修得状況照会', '履修登録確定'] }).clicks, ['単位修得状況照会']);
assert.deepEqual(run('grades', { labels: ['画面に表示する'], allGrades: true }).clicks, ['all-grades', '画面に表示する']);
assert.equal(run('grades', { labels: ['画面に表示する'] }).result, false);
assert.deepEqual(run('Q3', { labels: ['3クォーター', '履修登録確定'] }).clicks, ['3クォーター']);
assert.deepEqual(run('Q3', { labels: ['履修登録確定', '削除', 'ログアウト'] }).clicks, []);
assert.deepEqual(run('grades', { labels: ['単位修得状況照会'], host: 'login.microsoftonline.com' }).clicks, []);
assert.deepEqual(run('profile', { labels: ['学生カルテ', '単位修得状況照会'] }).clicks, ['学生カルテ']);
assert.deepEqual(run('profile', { labels: ['学籍情報', '学生個人情報', '健康管理情報'] }).clicks, ['学籍情報']);
assert.deepEqual(run('grades', { labels: ['画面に表示する'], allGrades: true, repeat: 5 }).clicks, ['all-grades', '画面に表示する']);
assert.deepEqual(run('grades', { labels: ['単位修得状況照会'], hidden: true }).clicks, ['単位修得状況照会']);
console.log('10 school navigation checks passed');
