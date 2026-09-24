(function () {
  'use strict';
  var request = __REQUEST__;
  if (location.protocol !== 'https:' || location.hostname !== 'csweb.ibaraki.ac.jp' ||
      !location.pathname.startsWith('/campusweb/')) return {kind:'error',message:'学校地址未通过核对'};
  var key='campus-native-registration-context';
  function text(n) {return (n && (n.innerText || n.textContent) || '').replace(/\s+/g,' ').trim();}
  function label(n) {return (n.tagName==='INPUT'?n.value:text(n)).replace(/\s+/g,'');}
  function visible(n) {return !n.disabled && n.getClientRects().length>0;}
  function controls() {return Array.from(document.querySelectorAll('a,button,input[type=button],input[type=submit]')).filter(visible);}
  function fail(message) {return {kind:'error',message:message};}
  function navigate(node) {
    if(document.__campusNativeNavigating) return {kind:'loading'};
    document.__campusNativeNavigating=true; node.click(); return {kind:'navigated'};
  }
  var body=text(document.body), cs=controls();
  var identities=Array.from(body.matchAll(/学生番号\s*[:：|]?\s*([A-Za-z0-9]+)/g)).map(function(m){return m[1];});
  if(identities.some(function(s){return s!==request.student;})) {
    sessionStorage.removeItem(key); return fail('学校账户已变化，请重新登录后读取');
  }
  if(document.querySelector('input[type=password]')) {sessionStorage.removeItem(key);return fail('学校登录已过期，请重新登录');}
  var term=body.match(/年度・学期\s*\|?\s*(20\d{2})年度\s*([1-4])クォーター/);
  var grid=document.querySelector('table.rishu-koma');
  if(grid && term && identities.length) {
    var scope=term[1]+'-Q'+term[2], slots=[], targets={};
    var closed=/履修登録期間外|登録期間外|登録できる期間ではありません/.test(body);
    cs.forEach(function(n) {
      var handler=n.getAttribute('onclick') || '';
      var m=handler.match(/^\s*return\s+SelectCallA\(['"]([1-6])['"],\s*['"]([1-5])['"]\)\s*;?\s*$/);
      var intensive=/^\s*return\s+OtherSelectCall\(\)\s*;?\s*$/.test(handler);
      if((m && /^(未登録|追加登録)$/.test(label(n))) || (intensive && label(n)==='集中講義を登録')) {
        var slot=m?m[1]+'-'+m[2]:'intensive';
        if(!targets[slot]) {targets[slot]=n;slots.push({key:slot,label:m?'月火水木金土'[Number(m[1])-1]+'曜 '+m[2]+'限':'集中授课'});}
      }
    });
    var registered=[];
    document.querySelectorAll('table.rishu-koma-inner').forEach(function(t) {
      var m=text(t).match(/^([A-Za-z0-9_-]{3,30})\s/);
      if(m) registered.push(m[1]);
    });
    document.querySelectorAll('table.rishu-etc tr').forEach(function(tr) {
      var c=Array.from(tr.cells).map(text);
      if(c.length>=4 && /^[A-Za-z0-9_-]{3,30}$/.test(c[2])) registered.push(c[2]);
    });
    var context={student:request.student,scope:scope,year:term[1],time:Date.now(),slot:'',registered:registered};
    sessionStorage.setItem(key,JSON.stringify(context));
    if(request.scope && request.scope!==scope) return fail('学校学期已变化，请刷新课程');
    if(request.action==='open') {
      if(closed || !targets[request.slot]) return fail('该时间格目前没有开放登记入口，请刷新');
      context.slot=request.slot;sessionStorage.setItem(key,JSON.stringify(context));
      return navigate(targets[request.slot]);
    }
    return {kind:'grid',student:request.student,scope:scope,slots:closed?[]:slots,registered:registered,closed:closed};
  }
  var context;
  try {context=JSON.parse(sessionStorage.getItem(key) || 'null');} catch(_) {}
  var list=document.querySelector('form#rishuReferSelectForm');
  if(list && /履修登録する科目を選択|集中講義/.test(body)) {
    if(!context || context.student!==request.student || Date.now()-context.time>10*60*1000 ||
        (request.scope && request.scope!==context.scope)) return fail('选课会话需要重新核对，请刷新');
    var heading=body.match(/([月火水木金土])曜日\s*([1-6])限で履修登録する科目を選択/);
    var slot=heading?String('月火水木金土'.indexOf(heading[1])+1)+'-'+heading[2]:'intensive';
    if(slot!==context.slot || (request.slot && request.slot!==slot)) return fail('学校返回的时间格不一致，请刷新');
    if(request.action==='back') {
      var backs=cs.filter(function(n){return label(n)==='履修登録画面に戻る';});
      return backs.length===1?navigate(backs[0]):fail('未能返回课表，请刷新');
    }
    var rows=[], buttons={}, malformed=false;
    Array.from(list.querySelectorAll('table')).forEach(function(t) {
      var trs=Array.from(t.rows), header=trs.find(function(tr){return /時間割コード/.test(text(tr)) && /科目/.test(text(tr));});
      if(!header) return;
      var names=Array.from(header.cells).map(text);
      function col(re){return names.findIndex(function(v){return re.test(v);});}
      var ci=col(/^時間割コード$/),ni=col(/^科目$|科目名/),si=col(/曜日.*時限/),ti=col(/^担当/),mi=col(/遠隔授業/),cr=col(/単位/),fi=col(/^開講学部$/);
      trs.slice(trs.indexOf(header)+1).forEach(function(tr) {
        var cells=Array.from(tr.cells).map(text);
        if(cells.length!==names.length) {if(!/該当するデータはありません|登録されていません/.test(text(tr))) malformed=true;return;}
        if(ci<0 || ni<0 || !/^[A-Za-z0-9_-]{3,30}$/.test(cells[ci]) || !cells[ni]) {malformed=true;return;}
        var bs=Array.from(tr.querySelectorAll('input[type=button],input[type=submit],button')).filter(function(b){return visible(b) && label(b)==='登録';});
        if(bs.length!==1) return;
        var b=bs[0], m=(b.getAttribute('onclick')||'').match(/^\s*rishuInsert\(['"](20\d{2})['"],\s*['"]([A-Za-z0-9_-]+)['"],\s*['"]([A-Za-z0-9_-]+)['"]\);?\s*$/);
        if(!m || m[1]!==context.year || m[3]!==cells[ci] || b.form!==list) {malformed=true;return;}
        var action=new URL(list.action || location.href,location.href);
        if(action.origin!==location.origin || !action.pathname.startsWith('/campusweb/')) {malformed=true;return;}
        var id=context.scope+'/'+m[2]+'/'+cells[ci];
        if(buttons[id]) {malformed=true;return;}
        var row={id:id,code:cells[ci],name:cells[ni],schedule:si>=0?cells[si]:'',credits:cr>=0?cells[cr]:'',
          teacher:ti>=0?cells[ti]:'',remote:mi>=0?cells[mi]:'',faculty:fi>=0?cells[fi]:'',status:'学校提供登记入口',available:context.registered.indexOf(cells[ci])<0,slotKey:slot};
        row.signature=JSON.stringify([context.student,context.scope,m.slice(1),row]);
        rows.push(row);buttons[id]=b;
      });
    });
    if(malformed) return fail('学校课程列表格式发生变化，请刷新后重试');
    if(request.action==='submit') {
      var chosen=rows.filter(function(r){return r.id===request.id;});
      if(chosen.length!==1 || !chosen[0].available || chosen[0].signature!==request.signature) return fail('课程信息或开放状态已变化，请重新选择');
      if(document.__campusNativeSubmitted) return fail('请求已发送，请先核对结果');
      document.__campusNativeSubmitted=true; buttons[request.id].click();
      return {kind:'sent'};
    }
    return {kind:'list',student:context.student,scope:context.scope,slot:slot,rows:rows};
  }
  if(request.action==='start') {
    sessionStorage.removeItem(key);
    var menus=Array.from(document.querySelectorAll('a')).filter(function(n){return label(n)==='履修登録・登録状況照会';});
    // The school can repeat this read-only entry in its menu, history and ranking.
    // Prefer the observed registration flow rather than requiring one visible label.
    var flowMenus=menus.filter(function(n) {
      try {
        var u=new URL(n.getAttribute('href') || '',location.href);
        return u.origin===location.origin && u.pathname==='/campusweb/campussquare.do' && u.searchParams.get('_flowId')==='RSW0001000-flow';
      } catch(_) {return false;}
    });
    if(flowMenus.length) return navigate(flowMenus[0]);
    if(menus.length===1) return navigate(menus[0]);
  }
  return {kind:'other'};
})();
