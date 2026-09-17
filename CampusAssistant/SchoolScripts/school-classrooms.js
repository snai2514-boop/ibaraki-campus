(function () {
  if (location.protocol !== 'https:' || location.hostname !== 'csweb.ibaraki.ac.jp' || !location.pathname.startsWith('/campusweb/')) return {ready:false};
  var table = document.getElementById('schedule-calender');
  if (!table) {
    var link = Array.from(document.querySelectorAll('a')).find(function(n){return n.textContent.trim()==='スケジュール管理';});
    if(link) link.click();
    return {ready:false};
  }
  var check = document.getElementById('check21');
  if (!check || !check.parentElement.textContent.includes('時間割コマ情報')) return {ready:false};
  if (!check.checked) { check.click(); return {ready:false}; }
  var heading = document.body.innerText.match(/(\d{4})年\s*(\d{1,2})月/);
  if (!heading) return {ready:false};
  var year=Number(heading[1]), month=Number(heading[2]), rows=[];
  table.querySelectorAll('td').forEach(function(cell){
    var head=cell.querySelector('.cal-head-number'), content=cell.querySelector('.cal-content');
    if(!head || !content) return;
    var day=parseInt(head.textContent,10), y=year, m=month;
    if(head.classList.contains('cal-head-oth')) { m += day>15 ? -1 : 1; if(m===0){m=12;y--;} if(m===13){m=1;y++;} }
    if(!(day>=1 && day<=31)) return;
    var date=y+'-'+String(m).padStart(2,'0')+'-'+String(day).padStart(2,'0');
    Array.from(content.children).forEach(function(node){
      var clone=node.cloneNode(true);
      clone.querySelectorAll('script,style,input,textarea,select,[hidden]').forEach(function(n){n.remove();});
      clone.querySelectorAll('br').forEach(function(n){n.replaceWith('\n');});
      var text=clone.textContent+'\n'+Array.from(node.querySelectorAll('[title]')).map(function(n){return n.title;}).join('\n')+'\n'+(node.title||'');
      var codes=Array.from(new Set(text.match(/\b(?:[A-Z]{1,3}[0-9]{4,6}|PG[0-9]{3})(?:-[A-Za-z0-9]+)*\b/g)||[]));
      var periods=Array.from(text.matchAll(/(?:第\s*)?([1-5])\s*(?:限|講時)/g)).map(function(m){return Number(m[1]);});
      var rooms=Array.from(text.matchAll(/(?:教室|場所|講義室)\s*[:：]\s*([^\n|]+)|((?:共通|共通教育)[^\n|]{0,30}?(?:\d{1,3}番?教室|\d{1,3}))|\b([A-Z][0-9]-[0-9A-Za-z]{2,5})\b/g)).map(function(m){return (m[1]||m[2]||m[3]).trim();});
      rooms=Array.from(new Set(rooms)); periods=Array.from(new Set(periods));
      // Ambiguous/missing identifiers and notices never overwrite a classroom.
      if(codes.length===1 && periods.length===1 && rooms.length===1 && rooms[0].length<=80 && !/未定|調整|変更|オンライン|遠隔/.test(rooms[0]))
        rows.push({code:codes[0],date:date,period:periods[0],room:rooms[0]});
    });
  });
  return {ready:true,rows:rows};
})();
