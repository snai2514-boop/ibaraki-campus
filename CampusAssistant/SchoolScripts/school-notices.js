(function () {
  var requested = __DETAIL__;
  var docs = [];
  function collect(doc, depth) {
    if (depth > 3) return;
    docs.push(doc);
    doc.querySelectorAll('iframe,frame').forEach(function(f) { try { if(f.contentDocument) collect(f.contentDocument, depth + 1); } catch(e) {} });
  }
  collect(document, 0);
  function text(el) { return (el.innerText || '').trim(); }
  for (var doc of docs) {
    var article = doc.querySelector('#main-func-body');
    if (!article) continue;
    var content = text(article);
    if (requested && content.includes('[履修・成績]') && content.includes('掲載日時／')) {
      return {ready:true, body:content.slice(0,100000)};
    }
    for (var table of article.querySelectorAll('table')) {
      var trs = Array.from(table.rows);
      if (!trs.length) continue;
      var headers = Array.from(trs[0].cells).map(text);
      if (headers[0] !== 'ジャンル' || headers[1] !== '表題' || !headers.includes('掲載日時')) continue;
      var rows = [];
      for (var tr of trs.slice(1)) {
        var cells = Array.from(tr.cells);
        if (!cells.length || text(cells[0]) !== '履修・成績') continue;
        var link = cells[1].querySelector('a[href]');
        if (!link) continue;
        var url = new URL(link.href, doc.baseURI);
        var id = url.searchParams.get('seqNo');
        if (!/^\d+$/.test(id || '') || url.origin !== 'https://csweb.ibaraki.ac.jp') continue;
        if (requested === id) { link.click(); return {ready:false}; }
        rows.push({id:id,title:text(link),published:text(cells[headers.indexOf('掲載日時')]),period:text(cells[headers.indexOf('掲示期間')])});
      }
      // Do not mislabel a paginated partial list as complete.
      var total = /全部で\s*(\d+)件/.exec(content);
      return {ready:true,rows:rows,complete:!!total && Number(total[1]) === rows.length};
    }
  }
  for (var label of ['履修・成績', '掲示板']) {
    for (var doc of docs) {
      var link = Array.from(doc.querySelectorAll('a')).sort(function(a,b){return Number(!!b.getClientRects().length)-Number(!!a.getClientRects().length);}).find(function(a) { return text(a).replace(/\s+/g,'') === label && !/^javascript:/i.test(a.getAttribute('href') || '') && !!a.getAttribute('href'); });
      if (link) { link.click(); return {ready:false,phase:label}; }
    }
  }
  for (var doc of docs) {
    var menu = doc.querySelector('#menu-link-mt-kj');
    if (menu && menu.getClientRects().length) { menu.click(); return {ready:false}; }
  }
  return {ready:false,phase:"no-match",articles:docs.map(function(d){return !!d.querySelector('#main-func-body');})};
})();
