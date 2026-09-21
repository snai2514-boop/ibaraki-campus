(function () {
  if (location.protocol !== 'https:' || location.hostname !== 'csweb.ibaraki.ac.jp' || !location.pathname.startsWith('/campusweb/')) return false;
  var target = __TARGET__, docs = [];
  function visit(doc, depth) {
    if (depth > 3) return;
    docs.push(doc);
    doc.querySelectorAll('iframe,frame').forEach(function (frame) {
      try { if (frame.contentDocument.location.origin === location.origin) visit(frame.contentDocument, depth + 1); } catch (_) {}
    });
  }
  visit(document, 0);
  function text(node) { return (node.tagName === 'INPUT' ? node.value : node.textContent).replace(/\s+/g, '').trim(); }
  function clickExact(label, allowCollapsed) {
    for (var doc of docs) {
      var nodes = Array.from(doc.querySelectorAll('a,button,input[type=button],input[type=submit],label'));
      var node = nodes.find(function (n) { return (allowCollapsed || n.getClientRects().length) && !n.disabled && text(n) === label; });
      if (node) { node.click(); return true; }
    }
    return false;
  }
  if (target === 'profile') {
    return clickExact('学籍情報', true) || clickExact('学生カルテ', true);
  }
  if (target === 'grades') {
    // Restrict to observed read-only query controls. Never submit enrollment forms.
    for (var doc of docs) {
      var display = Array.from(doc.querySelectorAll('input[type=submit],input[type=button],button,a')).find(function (n) { return text(n) === '画面に表示する'; });
      if (display) {
        var all = Array.from(doc.querySelectorAll('input[type=radio]')).find(function (n) {
          return (n.parentElement.textContent || '').replace(/\s+/g, '').includes('過去を含めた全成績');
        });
        if (!all) return false;
        // Do not repeatedly resubmit the same query while the school is responding.
        if (display.__campusGradesSubmitted) return false;
        display.__campusGradesSubmitted = true;
        all.click(); display.click(); return true;
      }
    }
    return clickExact('単位修得状況照会', true);
  }
  if (/^Q[1-4]$/.test(target)) {
    if (clickExact(target.substring(1) + 'クォーター')) return true;
    // The school's navigation menu may be collapsed after returning from grades.
    // Match the same exact read-only module label used by the visible menu.
    return clickExact('履修登録・登録状況照会', true);
  }
  return false;
})();
