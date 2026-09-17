/* Bundled interface only. No remote UI code, analytics, passwords or network fetches. */
'use strict';
const C=CampusCore, D=CampusCalendarData, native=!!window.webkit?.messageHandlers?.campus&&!window.__CAMPUS_PREVIEW__;
const preview=!native&&(window.__CAMPUS_PREVIEW__||new URLSearchParams(location.search).get('preview')==='1');
const empty=()=>({version:1,profile:null,snapshots:{},modes:{},rooms:{},registration:null,inbox:[],personal:[],pending:null});
let state=empty(),page='home',date=C.dateString(new Date()),view='week',selected=[],busy=false,generation=0,status='登录学校账户后同步课程、成绩和履修状况。',sequence=0;
const pending=new Map(),$=id=>document.getElementById(id),h=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function bridge(action,payload={}){if(!native)return Promise.reject(new Error('电脑预览不连接学校；请在苹果客户端使用此功能。'));return new Promise((resolve,reject)=>{const id=String(++sequence),timer=setTimeout(()=>{pending.delete(id);reject(new Error('操作超时，保留原记录'));},['importEvidence','importTheme'].includes(action)?300000:20000);pending.set(id,{resolve,reject,timer});window.webkit.messageHandlers.campus.postMessage({id,action,payload});});}
window.nativeReply=(id,result,error)=>{const p=pending.get(id);if(!p)return;clearTimeout(p.timer);pending.delete(id);error?p.reject(new Error(error)):p.resolve(result);};
window.nativeForeground=()=>{if(state.profile&&!busy)sync();};
window.nativeBackground=()=>{generation++;busy=false;status=state.pending?'提交结果尚未确认，重新打开后请核对学校结果。':'同步已暂停，已读取的数据保留。';render();};
window.nativeLoginClosed=()=>{if(!busy)sync();};
window.nativeEvent=(message)=>{status=message;render();};
function persist(){return native?bridge('save',{state}):Promise.resolve();}
function toast(message){$('toast').textContent=message;$('toast').style.display='block';setTimeout(()=>$('toast').style.display='none',5000);}
function modal(title,body,buttons=[['知道了',()=>{}]]){const d=$('dialog');if(d.open)d.close();$('dialog-title').textContent=title;$('dialog-body').innerHTML=body;$('dialog-actions').replaceChildren();for(const [label,fn] of buttons){const b=document.createElement('button');b.textContent=label;b.onclick=()=>{d.close();Promise.resolve(fn()).catch(e=>toast(e.message));};$('dialog-actions').append(b);}d.showModal();}
function announce(message){state.inbox.unshift({at:Date.now(),message});state.inbox=state.inbox.slice(0,100);if(native&&state.alerts!==false)bridge('notify',{message}).catch(()=>{});}
function allEvents(){const r=C.events(state.snapshots,D,state.profile?.affiliation||'');return {...r,events:r.events.concat(state.personal)};}
function venue(e){if(e.personal)return '个人安排';const label=C.venue(e.year,e.code,state.modes[e.year+'/'+e.code]?.delivery),room=state.rooms?.[[e.code,e.date,e.period].join('/')];return room&&['教室待确认','线下授课'].includes(label)?room:label;}
function courseCard(e){return `<div class="course"><h3>${h(e.name)}</h3><p>${e.period?'第 '+h(e.period)+' 限 · ':''}${h(venue(e))}</p></div>`;}
function render(){
 document.querySelectorAll('nav button').forEach(b=>b.classList.toggle('active',b.dataset.page===page));
 $('title').textContent=({home:'教务助手',calendar:'日历',courses:'我的课程',grades:'学分与成绩',settings:'设置',registration:'可登录课程',notices:'通知'})[page];
 $('subtitle').textContent=state.profile?`${state.profile.name} · ${state.profile.affiliation||'茨城大学'}`:'你的课程，清晰有序。';
 const main=$('content');
 if(page==='home'){
  const grades=state.snapshots.grades,today=C.dateString(new Date()),events=allEvents().events.filter(e=>e.date===today),count=state.registration?.rows.filter(r=>r.available).length||0;
  main.innerHTML=`<div class="home-grid"><section class="card hero"><div class="eyebrow">YOUR CAMPUS, AT A GLANCE</div><h2>把时间留给学习</h2><p>${state.profile?'课程与学业动态，一处掌握。':'连接学校账户，开始你的校园日程。'}</p><button class="primary" id="sync">${state.profile?'同步学校数据':'登录学校账户'}</button><p class="small">${h(status)}</p></section><section class="card"><div class="row spread"><h2>学业概览</h2><span class="tag">学校数据</span></div><div class="stats"><div class="stat"><div class="number">${grades?h(grades.earned):'—'}</div><div class="small">已修学分</div></div><div class="stat"><div class="number">${grades?.gpa??'—'}</div><div class="small">通算 GPA</div></div></div><button id="to-grades" class="secondary">查看全部成绩 ↗</button></section></div><section class="card" style="margin-top:16px"><div class="row spread"><div><h3>可登录课程</h3><p class="small">${count?count+' 门可供选择':'读取学校登记入口，在周历中选择'}</p></div><button id="registration">查看 →</button></div></section><section class="card"><div class="row spread"><h2>今天的安排</h2><span class="small">${today}</span></div>${events.length?events.map(courseCard).join(''):'<p class="empty">今天没有已确认的课程安排</p>'}</section><button id="notices" class="secondary">通知与学业变化 · ${state.inbox.length}</button>`;
  $('sync').onclick=()=>state.profile?sync():bridge('login').catch(e=>toast(e.message));$('to-grades').onclick=()=>go('grades');$('registration').onclick=()=>{go('registration');if(native&&!busy)sync(true);};$('notices').onclick=()=>go('notices');
 }else if(page==='calendar')renderCalendar();
 else if(page==='registration')renderRegistration();
 else if(page==='grades'){
  const g=state.snapshots.grades;main.innerHTML=`<section class="card"><h2>${g?h(g.earned)+' 已修学分':'尚未读取成绩'}</h2><p>通算 GPA ${g?.gpa??'—'} · 直接采用学校显示值</p><p class="small">${g?'上次同步 '+new Date(g.at).toLocaleString():'登录学校账户后同步'}</p></section>${(g?.rows||[]).map(r=>`<section class="card"><div class="row spread"><h3>${h(r.name)}</h3><span class="tag">${r.score!==null&&r.score!==undefined?h(r.score)+' 分 · ':''}${h(r.grade)} · ${r.passed?'通过':'未通过'}</span></div><p>${r.credits} 学分 · ${h(r.year)} ${h(r.term)}</p><p class="small">${h(r.category)}</p></section>`).join('')}`;
 }else if(page==='courses'){
  const ts=Object.values(state.snapshots).filter(t=>t.lessons);main.innerHTML=ts.length?ts.map(t=>`<section class="card"><h2>${t.year} · Q${t.quarter}</h2>${t.lessons.map(l=>`<div class="course"><h3>${h(l.name)}</h3><p>星期${'一二三四五六'[l.day-1]} · 第 ${l.period} 限 · ${l.credits} 学分</p><p>${h(C.venue(t.year,l.code,state.modes[t.year+'/'+l.code]?.delivery))}</p><button class="secondary syllabus" data-year="${t.year}" data-code="${h(l.code)}">课程大纲</button></div>`).join('')}</section>`).join(''):'<section class="card empty">同步学校课表后显示课程</section>';
  document.querySelectorAll('.syllabus').forEach(b=>b.onclick=()=>syllabus(Number(b.dataset.year),b.dataset.code));
 }else if(page==='notices'){
  main.innerHTML=state.inbox.map(n=>`<section class="card"><p class="small">${new Date(n.at).toLocaleString()}</p><div class="notice">${h(n.message)}</div></section>`).join('')+'<h2>学校公告</h2>'+(state.notices?.rows||[]).map(n=>`<section class="card"><h3>${h(n.title)}</h3><p class="small">${h(n.published)}</p><button class="notice-open" data-id="${h(n.id)}">阅读公告</button></section>`).join('');
  document.querySelectorAll('.notice-open').forEach(b=>b.onclick=()=>notice(b.dataset.id));
 }else if(page==='settings'){
  main.innerHTML=`<section class="card"><h2>学校账户</h2><p>${state.profile?h(state.profile.name)+' · '+h(state.profile.student):'未登录'}</p><button id="login">打开学校登录</button><p class="small">账号密码只在学校网页输入，应用不读取或保存密码。</p></section><section class="card"><h2>同步与通知</h2><p>每次回到应用，同步资料、成绩、Q1—Q4 课表、公告和履修状况。读取失败保留原记录。</p><button id="notify">允许系统通知</button><p class="small">通知包含学分、GPA 变化和新出现的可登记课程。</p></section><section class="card"><h2>隐私</h2><p>学校会话和个人数据保存在当前设备。没有数据上传服务，也不上传至 GitHub。</p><button id="logout" class="danger">退出并清除本机学校数据</button></section><p class="small">教务助手 iOS 1.0.82 · 茨城大学专用<br>校历仅含已核对的 2026 学年度；无法确认的课程日期不会猜测。</p>`;
  $('login').onclick=()=>{if(busy)return toast('请等待当前操作完成');bridge('login').catch(e=>toast(e.message));};$('notify').onclick=()=>bridge('permission').then(()=>toast('通知设置已请求')).catch(e=>toast(e.message));$('logout').onclick=()=>modal('退出学校账户','<p>将清除本机课程、成绩、会话和个人安排。学校记录不会改变。</p>',[['取消',()=>{}],['退出并清除',async()=>{generation++;busy=false;await bridge('logout');state=empty();selected=[];status='已退出';go('home');}]]);
 }
 if(page==='grades')showAcademic();
 if(page==='calendar')enhanceCalendar();
 if(page==='settings')enhanceSettings();
 try{applyThemePack();}catch(e){applyThemePack(null);}
 localize();
}
function go(next){page=next;render();window.scrollTo(0,0);}
function renderCalendar(){
 const data=allEvents(),week=C.monday(date),days=Array.from({length:5},(_,i)=>C.shift(week,i));
 $('content').innerHTML=`<div class="row spread"><input class="date" id="date" type="date" aria-label="选择日期" value="${date}"><span class="tag">${C.termOn(date)||'未公布校历'}</span></div><div class="segments"><button id="month" class="${view==='month'?'active':''}">月</button><button id="week" class="${view==='week'?'active':''}">周</button></div><div class="calendar-controls card"><button id="prev" aria-label="上一${view==='week'?'周':'月'}">‹</button><strong>${view==='week'?week.slice(5)+' — '+days[4].slice(5):date.slice(0,7)}</strong><button id="next" aria-label="下一${view==='week'?'周':'月'}">›</button></div><div id="calendar-grid"></div><div class="row spread"><button id="today" class="secondary">今天</button><button id="add-event">＋ 个人安排</button></div>${data.issues.length?'<details class="card"><summary>有 '+data.issues.length+' 项日期待核对</summary><p>'+data.issues.map(h).join('<br>')+'</p></details>':''}<p class="small">学校校历计划；临时调课和考试安排以学校通知为准。</p>`;
 if(view==='week'){$('calendar-grid').innerHTML=grid(days,(day,p)=>{const list=data.events.filter(e=>e.date===day&&e.period===p);return list.map(e=>`<div class="block ${p%2?'':'alt'}">${h(e.name)}<span class="small">${h(venue(e))}</span></div>`).join('');});}
 else {const d=C.dayDate(date),first=new Date(d.getFullYear(),d.getMonth(),1,12),start=C.monday(C.dateString(first)),dates=Array.from({length:42},(_,i)=>C.shift(start,i));$('calendar-grid').innerHTML='<div class="month">'+'一二三四五六日'.split('').map(x=>'<div class="month-head">'+x+'</div>').join('')+dates.map(s=>{const n=data.events.filter(e=>e.date===s).length;return `<button class="month-day ${s===date?'active':''}" data-date="${s}">${Number(s.slice(8))}<div class="small">${n?n+' 门':'·'}</div></button>`;}).join('')+'</div><section class="card"><h3>'+date+'</h3>'+data.events.filter(e=>e.date===date).map(courseCard).join('')+'</section>';document.querySelectorAll('.month-day').forEach(b=>b.onclick=()=>{date=b.dataset.date;render();});}
 $('date').onchange=e=>{if(e.target.value){date=e.target.value;render();}};$('month').onclick=()=>{view='month';render();};$('week').onclick=()=>{view='week';render();};
 function move(delta){if(view==='week')date=C.shift(C.monday(date),delta*7);else {const d=C.dayDate(date);date=C.dateString(new Date(d.getFullYear(),d.getMonth()+delta,1,12));}render();}
 $('prev').onclick=()=>move(-1);$('next').onclick=()=>move(1);$('today').onclick=()=>{date=C.dateString(new Date());render();};$('add-event').onclick=()=>modal('添加个人安排',`<label>标题<input id="event-name" type="text" maxlength="100"></label><label>日期<input id="event-date" class="date" type="date" value="${date}"></label><p><label>节次 <select id="event-period">${[1,2,3,4,5].map(p=>'<option>'+p+'</option>').join('')}</select></label></p>`,[['取消',()=>{}],['保存',async()=>{const name=$('event-name').value.trim(),d=$('event-date').value;if(!name||!d)return toast('请填写标题和日期');state.personal.push({name,date:d,period:Number($('event-period').value),personal:true,id:'personal-'+Date.now()});await persist();render();}]]);
}
function grid(days,cell){return '<div class="week-wrap"><div class="week"><div></div>'+days.map((d,i)=>`<div class="day">${'一二三四五'[i]}${/^\d/.test(d)?'<div class="small">'+d.slice(5)+'</div>':''}</div>`).join('')+[1,2,3,4,5].map(p=>`<div class="period">${p}<small>${['08:40','10:35','13:10','15:05','17:00'][p-1]}</small></div>`+days.map(d=>`<div class="cell">${cell(d,p)}</div>`).join('')).join('')+'</div></div>';}
function renderRegistration(){
 const r=state.registration,rows=r?.rows.filter(r=>r.available)||[];
 $('content').innerHTML=`<div class="row spread"><button id="back" class="secondary">‹ 首页</button><button id="refresh" ${busy?'disabled':''}>${busy?'读取中…':'刷新'}</button></div><p class="small status">${h(busy?status:r?.message||'尚未读取履修状况')}</p>${grid(['月','火','水','木','金'],(day,p)=>{const list=rows.filter(r=>C.slots(r.schedule).some(s=>s.day==='月火水木金'.indexOf(day)+1&&s.period===p));return list.length>1?`<button class="multi" data-day="${day}" data-period="${p}">${list.length} 门可选<br>点击选择</button>`:list.length?`<div class="block"><label><input type="checkbox" class="select-course" data-id="${h(list[0].id)}" ${selected.includes(list[0].id)?'checked':''} ${busy?'disabled':''}><span>${h(list[0].name)}</span></label></div>`:'';})}<div id="unscheduled"></div><div class="footer-action"><span id="selected-count" class="small">已选 ${selected.length} 门</span><button id="submit" class="primary" ${busy||!selected.length||state.pending?'disabled':''}>一键登录已勾选课程</button></div>${state.pending?'<section class="card"><p>上次请求结果未确认，已停止重复提交。请先到学校页面核对实际登记结果。</p><button id="check-pending">核对学校结果</button><button id="clear-pending" class="secondary">我已核对，重新读取</button></section>':''}`;
 $('back').onclick=()=>go('home');$('refresh').onclick=()=>sync(true);document.querySelectorAll('.select-course').forEach(b=>b.onchange=()=>{toggle(b.dataset.id,b.checked);render();});
 document.querySelectorAll('.multi').forEach(b=>b.onclick=()=>choose(rows.filter(r=>C.slots(r.schedule).some(s=>s.day==='月火水木金'.indexOf(b.dataset.day)+1&&s.period===Number(b.dataset.period)))));
 const extra=rows.filter(r=>!C.slots(r.schedule).some(s=>s.day<=5&&s.period<=5));if(extra.length){$('unscheduled').innerHTML='<button id="other">其他时间 / 时间待确认 · '+extra.length+' 门</button>';$('other').onclick=()=>choose(extra);}
 $('submit').onclick=confirmSubmit;if(state.pending){$('check-pending').onclick=()=>bridge('login').catch(e=>toast(e.message));$('clear-pending').onclick=()=>modal('确认已核对结果','<p>只有确认学校实际登记结果后才解除重复提交保护。</p>',[['取消',()=>{}],['已核对',async()=>{state.pending=null;selected=[];await persist();await sync(true);}]]);}
}
function toggle(id,on){selected=on?[...new Set([...selected,id])]:selected.filter(x=>x!==id);}
function choose(rows){modal('选择课程',rows.map(r=>`<label class="choice"><input class="dialog-course" type="checkbox" data-id="${h(r.id)}" ${selected.includes(r.id)?'checked':''} ${busy?'disabled':''}><span><strong>${h(r.name)}</strong><br><span class="small">${h(r.schedule)} · ${h(r.credits)} 学分</span></span></label>`).join(''),[['完成',render]]);document.querySelectorAll('.dialog-course').forEach(b=>b.onchange=()=>toggle(b.dataset.id,b.checked));}
async function read(script,request={}){return bridge('read',{script,request});}
const delay=ms=>new Promise(r=>setTimeout(r,ms));
async function poll(task,stamp,timeout=30000){const until=Date.now()+timeout;let last='学校响应超时';while(Date.now()<until){if(stamp!==generation)throw new Error('操作已中断');try{const value=await task();if(stamp!==generation)throw new Error('操作已中断');if(value)return value;}catch(e){if(e.message.includes('账户')||e.message.includes('登录')||stamp!==generation)throw e;last=e.message;}await delay(1400);}throw new Error(last);}
async function verifyOwner(){const e=await read('extract');if(e.needsLogin)throw new Error('学校登录已失效，请重新登录');if(C.identity(e.text)!==state.profile.student)throw new Error('学校账户已变化，请退出后重新登录');return e.text;}
async function sync(registrationOnly=false){
 if(busy)return; if(!native)return toast('电脑预览不连接学校');busy=true;const stamp=++generation,results=[],links=[];status='正在读取学校数据…';render();
 try{
  await bridge('home');
  if(!state.profile||!registrationOnly){let navigated=false;const p=await poll(async()=>{const e=await read('extract');if(e.needsLogin)throw new Error('学校登录已失效，请打开学校登录');try{return C.profile(e.text);}catch(err){if(!navigated)navigated=await read('school-sync-navigation',{target:'profile'});return null;}},stamp);if(state.profile&&state.profile.student!==p.student)throw new Error('学校账户已变化，请退出后重新登录');state.profile=p;await persist();results.push('学生资料已更新');}
  const tasks=registrationOnly?['registration']:['grades','Q1','Q2','Q3','Q4','classrooms','notices','registration','delivery'];
  for(const task of tasks){if(stamp!==generation)break;status='正在同步 '+({grades:'成绩 / 学分 / GPA',notices:'学校公告',registration:'履修状况',delivery:'授课方式'}[task]||task);render();
   try{
    if(task==='delivery'){
     if(links.length){await bridge('home');const result=await poll(async()=>{const r=await read('school-course-modes',{links});return r.ready?r:null;},stamp,35000);for(const row of result.rows)state.modes[row.year+'/'+row.code]=row;results.push('授课方式已核对 '+result.rows.length+' 门');}
    }else if(task==='classrooms'){
     await bridge('home');const r=await poll(async()=>{const r=await read('school-classrooms');return r.ready?r:null;},stamp);state.rooms=state.rooms||{};const events=allEvents().events,groups=new Map();for(const row of r.rows){if(!events.some(e=>e.code===row.code&&e.date===row.date&&e.period===row.period))continue;const key=[row.code,row.date,row.period].join('/');groups.set(key,[...(groups.get(key)||[]),row.room]);}for(const [key,rooms] of groups)if(new Set(rooms).size===1)state.rooms[key]=rooms[0];results.push('当前学校日历教室已读取');
    }else if(task==='registration'){
     await bridge('home');let count=0;const r=await poll(async()=>{const r=await read('school-registration',{action:'read',student:state.profile.student,navigate:count<2});if(r.navigated)count++;if(r.ready)return r;if(r.tableText)return C.readOnlyRegistration(r.tableText,state.profile.student);return null;},stamp);
     if(r.student!==state.profile.student)throw new Error('学校账户已变化');const before=state.registration;r.at=Date.now();selected=C.reconcile(before,r,selected);state.registration=r;
     const added=r.rows.filter(x=>x.available&&!before?.rows.some(old=>old.id===x.id&&old.available));if(added.length){const msg='发现 '+added.length+' 门可登记课程：'+added.map(x=>x.name).join('、');announce(msg);if(!$('dialog').open)modal('有课程可以登录',`<p>${h(msg)}</p>`,[['稍后',()=>{}],['查看',()=>go('registration')]]);}results.push('履修状况已读取');
    }else if(task==='notices'){
     await bridge('home');const r=await poll(async()=>{const r=await read('school-notices',{});return r.ready?r:null;},stamp);state.notices=r;results.push('公告已读取 '+r.rows.length+' 条'+(r.complete?'':'（当前页）'));
    }else{
     if(task==='grades'||task==='Q1')await bridge('home');let lastNav=0;
     const t=await poll(async()=>{const e=await read('extract');if(e.needsLogin)throw new Error('学校登录已失效');let parsed;try{parsed=C.parse(e.text);}catch(_){}if(parsed&&(task==='grades'?parsed.key==='grades':parsed.quarter===Number(task.slice(1)))){if(C.identity(e.text)!==state.profile.student)throw new Error('学校账户已变化');return parsed;}if(Date.now()-lastNav>4000){await read('school-sync-navigation',{target:task});lastNav=Date.now();}return null;},stamp);
     if(t.lessons&&!t.lessons.length){results.push(task+' 学校暂无课程，保留原记录');continue;}
     t.at=Date.now();t.student=state.profile.student;if(task==='grades'){const diff=C.changes(state.snapshots.grades,t);if(diff.length)announce(diff.join('；'));}else{const capture=await read('school-course-modes',{capture:true,year:t.year,codes:t.lessons.map(l=>l.code)});links.push(...capture.links);}
     state.snapshots[t.key]=t;results.push(task+' 已更新 '+(t.rows||t.lessons).length+' 门');
    }
    if(stamp===generation)await persist();
   }catch(e){if(stamp!==generation)break;if(/账户|登录/.test(e.message))throw e;results.push(task+' 未更新：'+e.message+'；保留原记录');}
  }
  status=results.join('\n');
 }catch(e){status=e.message;toast(e.message);}finally{if(stamp===generation){busy=false;render();}}
}
function confirmSubmit(){try{const r=state.registration,rows=C.selectedRows(r,selected);if(preview)return modal('预览：二次确认',rows.map(x=>`<p>${h(x.name)} · ${h(x.schedule)}</p>`).join('')+'<p>此处仅演示所选课程，不连接学校或提交登记。</p>');const ids=rows.map(x=>x.id),signature=r.signature;modal('确认登记以下课程？',rows.map(x=>`<p>${h(x.name)}<br><span class="small">${h(x.schedule)} · ${h(x.credits)} 学分</span></p>`).join('')+'<p>确认后向学校提交，仅登记以上勾选课程。</p>',[['取消',()=>{}],['确认登记',()=>submit(ids,signature)]]);}catch(e){toast(e.message);}}
async function submit(ids,signature){
 if(busy||state.pending)return;busy=true;const stamp=++generation;render();
 try{C.selectedRows(state.registration,ids);if(state.registration.signature!==signature)throw new Error('课程状态已变化，请重新确认');state.pending={ids,scope:state.registration.scope,student:state.profile.student,at:Date.now()};await persist();const result=await read('school-registration',{action:'submit',student:state.profile.student,ids,signature});if(!result.sent){state.pending=null;await persist();throw new Error(result.message||'未提交');}
  let confirmed=false;const outcome=await poll(async()=>{const r=await read('school-registration',{action:'result',...state.pending});if(r.result==='success')return r;if(r.result==='confirm'&&!confirmed){confirmed=true;const c=await read('school-registration',{action:'confirm',...state.pending,signature:r.signature});if(!c.sent)throw new Error(c.message||'学校确认未通过');}return null;},stamp);
  state.pending=null;selected=[];state.registration={...outcome,at:Date.now()};await persist();modal('登记完成','<p>学校已明确返回所选全部课程登记成功。请重新同步正式课表。</p>');
 }catch(e){status=state.pending?'结果未确认，已停止自动重试。请到学校页面核对。':e.message;modal('登记状态',`<p>${h(status)}</p>`);}finally{if(stamp===generation){busy=false;render();}}
}
async function syllabus(year,code){if(busy)return toast('正在同步，请稍后');busy=true;const stamp=++generation;try{const term=Object.values(state.snapshots).find(t=>t.year===year&&t.lessons?.some(l=>l.code===code));if(!term)throw new Error('课程学季尚未读取');await bridge('home');let lastNav=0;const r=await poll(async()=>{const r=await read('school-syllabus',{year,code,quarter:term.quarter});if(r.error)throw new Error('未找到对应课程大纲');if(r.navigate&&Date.now()-lastNav>4000){await read('school-sync-navigation',{target:'Q'+term.quarter});lastNav=Date.now();}return r.ready?r:null;},stamp);state.modes[year+'/'+code]={delivery:r.delivery};await persist();modal(r.title||'课程大纲',`<p>${h(C.delivery(r.delivery))}</p><h3>评价方法</h3><p>${h(r.grading)}</p><h3>教材</h3><p>${h(r.textbook)}</p><h3>履修注意</h3><p>${h(r.notes)}</p>`+(r.rows||[]).map(row=>`<div class="course"><h3>第 ${h(row.number)} 次 · ${h(row.subject)}</h3><p>${h(row.content)}</p></div>`).join(''));}catch(e){toast(e.message);}finally{busy=false;}}
async function notice(id){if(busy)return toast('正在同步，请稍后');busy=true;const stamp=++generation;try{await bridge('home');const r=await poll(async()=>{const r=await read('school-notices',{detail:id});return r.ready&&r.body?r:null;},stamp);modal('学校公告','<div class="notice">'+h(r.body)+'</div>');}catch(e){toast(e.message);}finally{busy=false;}}
document.querySelectorAll('nav button').forEach(b=>b.onclick=()=>go(b.dataset.page));
$('dialog').addEventListener('close',()=>{if(page==='registration')render();});
async function start(){
 if(native){try{state=(await bridge('load'))||empty();}catch(e){status=e.message;}render();playThemeStartup();if(state.profile)sync();}
 else{if(preview){$('preview').hidden=false;state.profile={student:'DEMO0000',name:'预览同学',affiliation:'人文社会科学部 法律経済学科',curriculumYear:2026,program:''};state.snapshots.grades={student:'DEMO0000',earned:2,gpa:3.2,at:Date.now(),rows:[{name:'大学入門ゼミ',credits:2,score:85,grade:'A',passed:true,year:'2026',term:'前期',category:'基盤教育科目'}]};const names=['共生とコミュニケーション【3Q】','経済・経営【3Q】'];state.snapshots['timetable-2026-Q3']={key:'timetable-2026-Q3',student:'DEMO0000',year:2026,quarter:3,at:Date.now(),lessons:names.map((name,i)=>({code:'DEMO'+i,name,description:name,credits:2,day:5,period:i+1}))};D.offerings.DEMO0=D.offerings.DEMO1={term:'3Q',campus:'MITO',irregular:false};state.modes['2026/DEMO0']=state.modes['2026/DEMO1']={delivery:'オンライン授業（リアルタイム配信型）'};state.registration={student:'DEMO0000',scope:'demo',at:Date.now(),message:'虚构预览数据：勾选课程后查看二次确认。',rows:[['a','法学入门','月1'],['b','经济学基础','火2'],['c','统计学基础','火2'],['d','信息与社会','金3']].map(([id,name,schedule])=>({id,name,schedule,credits:'2',available:true}))};date='2026-09-21';status='电脑预览 · 示例数据，不代表真实学校记录。';}render();}
}
start();
