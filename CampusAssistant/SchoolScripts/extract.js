(function () {
  if(location.origin !== 'https://csweb.ibaraki.ac.jp' || !location.pathname.startsWith('/campusweb/')) return {text:'',needsLogin:true};
  const output=[], visited=[]; let authenticated=false, needsLogin=false;
  function visit(doc,depth) {
    if(depth>3 || visited.includes(doc))return; visited.push(doc);
    if(doc.querySelector('input[type=password]'))needsLogin=true;
    if([...doc.querySelectorAll('a,button')].some(n=>n.textContent.replace(/\s/g,'')==='ログアウト'))authenticated=true;
    for(const table of doc.querySelectorAll('table')) {
      if(!table.getClientRects().length)continue;
      output.push([...table.rows].slice(0,300).map(row=>[...row.cells].map(cell=>{
        const c=cell.cloneNode(true); c.querySelectorAll('input,textarea,select,script,style,table,[hidden]').forEach(n=>n.remove());
        return c.textContent.replace(/\s+/g,' ').trim().slice(0,500);
      }).join(' | ')).join('\n'));
    }
    for(const f of doc.querySelectorAll('iframe,frame'))try{if(f.contentDocument.location.origin===location.origin)visit(f.contentDocument,depth+1);}catch(_){}
  }
  visit(document,0);return {text:output.join('\n\n').slice(0,60000),authenticated,needsLogin};
})();
