const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('CampusAssistant/SchoolScripts/school-course-modes.js','utf8');
const location={origin:'https://csweb.ibaraki.ac.jp',pathname:'/campusweb/'};
let fetches=0;
const document={querySelectorAll:()=>[
  "syRefer('2026','05','KB9999')", "syRefer('2026','05','KB9999')",
  "syRefer('2025','05','KB9999')", "syRefer('2026','05','OTHER')"
].map(value=>({getAttribute:()=>value}))};
const captured=vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify({capture:true,year:2026,codes:['KB9999']})),{document,location,window:{},fetch(){fetches++;throw Error('capture must never fetch');}});
assert.equal(fetches,0);
assert.equal(captured.links.length,1);
assert.equal(captured.links[0][1],'2026');
assert.equal(captured.links[0][3],'KB9999');
console.log('4 syllabus capture-isolation checks passed');
const child={location:{origin:location.origin},querySelectorAll:s=>s==='a[onclick]'?[{getAttribute:()=>`syRefer("2026", "05", "KB9999")`}]:[]};
const parent={querySelectorAll:s=>s==='iframe,frame'?[{contentDocument:child}]:[]};
const framed=vm.runInNewContext(source.replace('__REQUEST__',JSON.stringify({capture:true,year:2026,codes:['KB9999']})),{document:parent,location,window:{},fetch(){throw Error('capture must never fetch');}});
assert.equal(framed.links.length,1);
assert.equal(framed.links[0][3],'KB9999');
console.log('2 framed syllabus capture checks passed');
