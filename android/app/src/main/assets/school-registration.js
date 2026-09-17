(function () {
  'use strict';
  var request = __REQUEST__;
  if (location.protocol !== 'https:' || location.hostname !== 'csweb.ibaraki.ac.jp' || !location.pathname.startsWith('/campusweb/')) return {ready:false};
  var docs = [];
  function visit(d, depth) {
    if (depth > 3 || docs.indexOf(d) >= 0) return;
    docs.push(d);
    d.querySelectorAll('iframe,frame').forEach(function(f) { try { if(f.contentDocument.location.origin === location.origin) visit(f.contentDocument, depth+1); } catch (_) {} });
  }
  visit(document, 0);
  function text(n) { return (n.innerText || n.textContent || '').replace(/\s+/g,' ').trim(); }
  function label(n) { return (n.tagName === 'INPUT' ? n.value : text(n)).replace(/\s+/g,''); }
  function visible(n) { return n.getClientRects().length > 0 && !n.disabled; }
  function controls(d) { return Array.from(d.querySelectorAll('button,input[type=submit],input[type=button],a')).filter(visible); }
  var tableText=docs.flatMap(function(doc){return Array.from(doc.querySelectorAll('table')).filter(function(table){return !table.getClientRects || table.getClientRects().length;}).map(function(table){
    return Array.from(table.rows || table.querySelectorAll('tr')).slice(0,300).map(function(row){return Array.from(row.cells).map(function(cell){
      var clone=cell.cloneNode?cell.cloneNode(true):cell;
      if(clone!==cell) clone.querySelectorAll('input,textarea,select,script,style,table,[hidden]').forEach(function(n){n.remove();});
      return text(clone).slice(0,500);
    }).join(' | ');}).join('\n');
  });}).join('\n\n').slice(0,60000);
  function navigate() {
    if(request.action!=='read' || request.navigate===false) return false;
    for(var doc of docs) {
      var link=Array.from(doc.querySelectorAll('button,input[type=submit],input[type=button],a')).find(function(n){return !n.disabled && label(n)==='履修登録・登録状況照会';});
      if(link) {link.click(); return true;}
    }
    return false;
  }
  var identities = Array.from(tableText.matchAll(/学生番号\s*\|\s*([A-Za-z0-9]+)/g)).map(function(m){return m[1];});
  if(!identities.length) identities=docs.map(function(d) { var m=text(d.body).match(/学生番号\s*[:：|]?\s*([A-Za-z0-9]+)/); return m && m[1]; }).filter(Boolean);
  var student = identities[0];
  // The dashboard may omit the student number. Read-only menu navigation is allowed;
  // reading a snapshot or submitting still requires the live number on the target page.
  if (!student) return navigate()?{ready:false,navigated:true}:{ready:false, message:'无法确认学校账户，请打开履修状况页面后重新读取'};
  if (identities.some(function(s) {return s!==student;})) return {ready:false, message:'无法确认学校账户'};
  if (request.student && student !== request.student) return {ready:false, message:'学校账户已变化，请重新读取'};
  for (var d of docs) {
    var body = text(d.body);
    if (!/履修登録|登録状況照会/.test(body)) continue;
    var year = (body.match(/(20\d{2})\s*年度/) || [])[1];
    var term = (body.match(/(?:第\s*)?[1-4]\s*クォーター|[前後]期/) || [])[0];
    var scope = year && term ? year + '/' + term.replace(/\s+/g,'') : '';
    var closed = /履修登録期間外|登録期間外|登録できる期間ではありません/.test(body);
    var buttons = controls(d).filter(function(n) { return /^(登録|履修登録|登録する|登録内容を確認する)$/.test(label(n)); });
    var rows = [], targets = {};
    for (var table of Array.from(d.querySelectorAll('table'))) {
      var trs = Array.from(table.querySelectorAll('tr'));
      var header = trs.find(function(tr) {return /科目名|授業名/.test(text(tr)) && /科目コード|授業コード|時間割コード/.test(text(tr));});
      if (!header) continue;
      var names = Array.from(header.cells).map(text);
      function col(re) {return names.findIndex(function(v) {return re.test(v);});}
      var ci=col(/科目コード|授業コード|時間割コード/), ni=col(/科目名|授業名/), ti=col(/曜日.*時限|曜日.*時講|曜時/), di=col(/^曜日$/), pi=col(/^時限$|^時講$/), si=col(/状態|状況/), cr=col(/単位/);
      for (var tr of trs.slice(trs.indexOf(header)+1)) {
        var cells=Array.from(tr.cells).map(text);
        if(cells.length!==names.length) continue;
        var code=cells[ci], name=cells[ni];
        if(!/^[A-Za-z0-9_-]{3,30}$/.test(code) || !name) continue;
        var boxes=Array.from(tr.querySelectorAll('input[type=checkbox]')).filter(visible);
        var schedule=ti>=0?cells[ti]:[di>=0?cells[di]:'',pi>=0?cells[pi]:''].join(' ');
        var status=si>=0?cells[si]:'';
        var available=!!scope && !closed && buttons.length===1 && boxes.length===1 && !boxes[0].checked && !/登録済|履修中|不可|取消|削除/.test(status);
        var id=scope+'/'+code;
        if(targets[id]) return {ready:false,message:'课程代码重复，不能安全识别'};
        rows.push({id:id,code:code,name:name,schedule:schedule,credits:cr>=0?cells[cr]:'',status:status,available:available});
        targets[id]={box:boxes[0], row:tr};
      }
    }
    if (!rows.length && !closed) continue;
    var snapshot={ready:true,student:student,scope:scope,closed:closed,rows:rows,message:closed?'学校当前不在登记期间':(!scope?'学年或学期未确认，仅显示履修状况':rows.some(function(r){return r.available;})?'学校页面存在可登记课程':'未发现可确认的登记入口')};
    snapshot.signature=JSON.stringify([student,scope,closed,rows]);
    if(request.action==='result' || request.action==='confirm') {
      var ids=request.ids || [], matching=rows.filter(function(r){return ids.includes(r.id);});
      if(request.scope!==scope || !ids.length || new Set(ids).size!==ids.length) return {ready:false,message:'登记学期未通过核对，请检查学校结果'};
      if(matching.length===ids.length && matching.every(function(r){return /登録済|履修中/.test(r.status);})) return Object.assign(snapshot,{result:'success'});
      var confirms=controls(d).filter(function(n){return label(n)==='確定';});
      if(rows.length===ids.length && matching.length===ids.length && /履修登録.*確認|登録内容.*確認/.test(body) && confirms.length===1) {
        if(request.action==='confirm') {
          if(request.signature!==snapshot.signature) return {ready:false,message:'学校确认内容已变化，已停止提交'};
          var finalForm=confirms[0].form;
          if(!finalForm) return {ready:false,message:'学校确认表单未适配'};
          var finalAction=new URL(finalForm.action || location.href,location.href);
          if(finalAction.origin!==location.origin || !finalAction.pathname.startsWith('/campusweb/') || finalForm.__campusConfirmed) return {ready:false,message:'学校确认请求未通过校验'};
          finalForm.__campusConfirmed=true; confirms[0].click();
          return {sent:true,finalStage:true};
        }
        return Object.assign(snapshot,{result:'confirm'});
      }
      return Object.assign(snapshot,{result:'unconfirmed'});
    }
    if(request.action === 'submit') {
      // Re-read and compare the exact live account, term, course details and availability after confirmation.
      if(!request.ids || !request.ids.length || request.signature!==snapshot.signature || request.ids.length!==new Set(request.ids).size) return {ready:false,message:'课程或开放状态已变化，请重新读取后确认'};
      var chosen=rows.filter(function(r){return request.ids.indexOf(r.id)>=0;});
      if(chosen.length!==request.ids.length || chosen.some(function(r){return !r.available;})) return {ready:false,message:'所选课程已不可登记'};
      var form=buttons[0].form;
      if(!form || chosen.some(function(r){return targets[r.id].box.form!==form;}) || Array.from(form.querySelectorAll('input[type=checkbox]')).some(function(b){return b.checked;})) return {ready:false,message:'学校表单包含其他选择，未提交，请在学校页面核对'};
      var action=new URL(form.action || location.href, location.href);
      if(action.origin!==location.origin || !action.pathname.startsWith('/campusweb/')) return {ready:false,message:'学校提交地址未通过校验，未提交'};
      if(form.__campusSubmitted) return {ready:false,message:'请求已经发送，请查询结果，勿重复提交'};
      chosen.forEach(function(r){targets[r.id].box.checked=true;});
      form.__campusSubmitted=true;
      buttons[0].click();
      return {sent:true,message:'已发送登记请求；请核对学校返回结果。需要学校确认时请在下方完成。'};
    }
    return snapshot;
  }
  if(tableText.includes('年度・学期 |') && tableText.includes('月曜日 | 火曜日')) return {ready:false,tableText:tableText,message:'已读取履修课表，正在核对登记选项'};
  if(navigate()) return {ready:false,navigated:true};
  return {ready:false,message:'尚未读取到已适配的履修状况表，请打开履修登録・登録状況照会'};
})();
