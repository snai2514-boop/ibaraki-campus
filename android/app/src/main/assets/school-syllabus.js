(function(){
 if(location.origin!=='https://csweb.ibaraki.ac.jp'||!location.pathname.startsWith('/campusweb/'))return {};
 const request=__REQUEST__, clean=n=>(n?.textContent||'').replace(/\s+/g,' ').trim();
 const basic=document.querySelector('#tabs-1'), details=document.querySelector('#tabs-2'), plan=document.querySelector('#tabs-3');
 function field(root,label){const r=[...(root?.querySelectorAll('tr')||[])].find(r=>clean(r.cells[0]).replace(/\s/g,'').startsWith(label));return r?clean(r.cells[1]):'';}
 if(basic&&details&&plan){
   const code=field(basic,'時間割コード');
   if(code!==request.code)return {error:true};
   const rows=[...plan.querySelectorAll('tr')].filter(r=>r.cells.length>=5&&/^\d+$/.test(clean(r.cells[0]))).map(r=>({number:clean(r.cells[0]),when:clean(r.cells[1]),subject:clean(r.cells[2]),content:clean(r.cells[3]),notes:clean(r.cells[4])}));
   return {ready:true,code,title:field(basic,'開講科目名'),delivery:field(details,'オンライン授業'),grading:field(details,'成績の評価方法'),textbook:field(details,'教科書'),notes:field(details,'履修上の注意'),rows};
 }
 const text=clean(document.querySelector('#main-func-body'));
 const period=text.match(/(\d{4})年度\s*([1-4])クォーター/);
 if(period&&Number(period[1])!==request.year)return {error:true};
 if(period&&Number(period[2])===request.quarter){
   const link=[...document.querySelectorAll('a[onclick]')].find(a=>{const m=a.getAttribute('onclick').match(/syRefer\('([0-9]{4})','([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)','([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)'/);return m&&Number(m[1])===request.year&&m[3]===request.code;});
   if(!link)return {error:true};
   const m=link.getAttribute('onclick').match(/syRefer\('([0-9]{4})','([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)','([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)'/);
   location.href='/campusweb/campussquare.do?_flowId=SBW3701300-flow&isOpenWindow=1&_eventId=syllabus&nendo='+m[1]+'&jikanwarishozokucd='+m[2]+'&jikanwaricd='+m[3];
   return {};
 }
 return {navigate:true};
})();
