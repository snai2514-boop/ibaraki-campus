(function(){
  if(location.origin!=='https://csweb.ibaraki.ac.jp'||!location.pathname.startsWith('/campusweb/'))return {ready:true,rows:[]};
  const request=__REQUEST__, key=JSON.stringify(request);
  const docs=[];
  function visit(doc,depth){if(depth>3 || docs.includes(doc))return;docs.push(doc);doc.querySelectorAll('iframe,frame').forEach(f=>{try{if(f.contentDocument.location.origin===location.origin)visit(f.contentDocument,depth+1);}catch(_){}});}
  visit(document,0);
  const links=request.links || docs.flatMap(doc=>[...doc.querySelectorAll('a[onclick]')]).map(a=>a.getAttribute('onclick').match(/syRefer\(\s*['"]([0-9]{4})['"]\s*,\s*['"]([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)['"]\s*,\s*['"]([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)['"]/)).filter(m=>m&&Number(m[1])===request.year&&request.codes.includes(m[3])).map(m=>[m[0],m[1],m[2],m[3]]);
  const unique=[...new Map(links.filter(m=>/^20\d{2}$/.test(m[1])&&/^[A-Za-z0-9-]+$/.test(m[2])&&/^[A-Za-z0-9-]+$/.test(m[3])).map(m=>[m[1]+'/'+m[3],m])).values()];
  if(request.capture)return {links:unique};
  if(window.__campusModes?.key===key)return window.__campusModes;
  const state=window.__campusModes={key,ready:false,rows:[]};
  const clean=n=>(n?.textContent||'').replace(/\s+/g,' ').trim();
  function field(root,label){const row=[...(root?.querySelectorAll('tr')||[])].find(r=>clean(r.cells[0]).replace(/\s/g,'').startsWith(label));return row?clean(row.cells[1]):'';}
  async function worker(){
    while(unique.length){const m=unique.shift(), controller=new AbortController(), timer=setTimeout(()=>controller.abort(),8000);
      try{
        const url='/campusweb/campussquare.do?_flowId=SBW3701300-flow&isOpenWindow=1&_eventId=syllabus&nendo='+m[1]+'&jikanwarishozokucd='+m[2]+'&jikanwaricd='+m[3];
        const response=await fetch(url,{credentials:'same-origin',signal:controller.signal});
        if(!response.ok || !response.url.startsWith(location.origin+'/campusweb/'))continue;
        const doc=new DOMParser().parseFromString(await response.text(),'text/html'), basic=doc.querySelector('#tabs-1'), details=doc.querySelector('#tabs-2');
        if(field(basic,'時間割コード')!==m[3] || !details)continue;
        state.rows.push({year:Number(m[1]),code:m[3],delivery:field(details,'オンライン授業'),notes:field(details,'履修上の注意')});
      }catch(_){}finally{clearTimeout(timer);}
    }
  }
  Promise.all([worker(),worker()]).finally(()=>{state.ready=true;});
  return state;
})();
