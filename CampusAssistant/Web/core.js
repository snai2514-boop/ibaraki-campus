(function(root){
 'use strict';
 const assert=(ok,message)=>{if(!ok)throw new Error(message);};
 function identity(text){const ids=[...text.matchAll(/学生番号\s*\|\s*([A-Za-z0-9]{4,24})\b/g)].map(m=>m[1]); assert(ids.length && new Set(ids).size===1,'无法确认学校账户');return ids[0];}
 function profile(text){
  const values={};for(const line of text.split('\n')){const cells=line.split(' | ').map(x=>x.trim());if(cells.length%2)continue;for(let i=0;i<cells.length;i+=2){if(!['学生氏名','学生番号','所属','学年'].includes(cells[i]))continue;assert(!values[cells[i]]||values[cells[i]]===cells[i+1],'学校账户信息冲突');values[cells[i]]=cells[i+1];}}
  assert(values['学生氏名'],'尚未读取学生资料');return {student:identity(text),name:values['学生氏名'],affiliation:values['所属']||'',year:values['学年']||''};
 }
 function parse(text){
  const lines=text.split('\n').map(s=>s.trim()), start=lines.findIndex(l=>l.startsWith('No. | 科目大区分 |'));
  if(start>=0){
   const rows=[];for(const line of lines.slice(start+1)){if(!line)break;const c=line.split(' | ');assert(c.length===11&&/^\d+$/.test(c[0]),'成绩表结构变化');const credits=Number(c[5]),score=/^\d+$/.test(c[8])?Number(c[8]):null;
    assert(credits>0&&credits<=10&&['合','否'].includes(c[10]),'成绩尚未确定');assert(score===null||(score<=100&&(score>=60)===(c[10]==='合')),'分数与合否不一致');
    rows.push({name:c[4],credits,score,grade:c[9],passed:c[10]==='合',year:c[6],term:c[7],category:c.slice(1,4).filter(Boolean).join(' / ')});
   }
   const earned=rows.filter(r=>r.passed).reduce((n,r)=>n+r.credits,0),reported=text.match(/修得単位数 \| ([0-9.]+)/),gi=lines.indexOf('年度・学期 | 学期GPA | 年間GPA | 通算GPA');
   assert(rows.length&&reported&&Math.abs(earned-Number(reported[1]))<1e-6,'明细学分与学校总数不一致');
   const raw=gi>=0?(lines[gi+1]||'').split(' | ')[3]:null,gpa=raw&&/^\d+(\.\d+)?$/.test(raw)?Number(raw):null;
   return {key:'grades',rows,earned,gpa};
  }
  const term=text.match(/年度・学期 \| (\d{4})年度 ([1-4])クォーター/);assert(term,'尚未读取到已适配课表');
  assert(lines.includes('| 月曜日 | 火曜日 | 水曜日 | 木曜日 | 金曜日 | 土曜日'),'星期结构变化');const lessons=[];
  for(let p=1;p<=6;p++){const a=lines.indexOf(p+'限'),b=lines.indexOf(p<6?(p+1)+'限':'集中講義など');assert(a>=0&&b>a,'课表节次不完整');const cells=lines.slice(a+1,b).filter(Boolean);assert(cells.length===6,'课表列数变化');cells.forEach((cell,i)=>{if(cell==='未登録')return;const m=cell.match(/^([A-Z][A-Za-z0-9-]*) (.+) ([0-9.]+)単位$/);assert(m,'存在未识别课程');lessons.push({day:i+1,period:p,code:m[1],name:m[2],credits:Number(m[3]),description:cell});});}
  const count=text.match(/件数 \| (\d+)件/);assert(text.includes('登録されていません')&&count&&Number(count[1])===lessons.length,'课程数不一致或存在集中课程');
  return {key:`timetable-${term[1]}-Q${term[2]}`,year:Number(term[1]),quarter:Number(term[2]),lessons};
 }
 function readOnlyRegistration(text,student){assert(identity(text)===student,'学校账户已变化');const t=parse(text);assert(t.lessons,'未读取到履修课表');const rows=[...new Map(t.lessons.map(l=>[l.code,{id:t.key+'/'+l.code,code:l.code,name:l.name,schedule:'',credits:String(l.credits),available:false,status:'已登记'}])).values()];return {ready:true,student,scope:t.key,rows,signature:'',message:'履修状况已读取；学校当前页面未列出可确认的登记选项。'};}
 function slots(schedule){
  const s=schedule.normalize('NFKC').replace(/曜日|曜/g,'').replace(/\s+/g,''),out=[];
  for(const m of s.matchAll(/([月火水木金土日](?:[・、,\/][月火水木金土日])*)[：:]?([1-6](?:[-~〜～][1-6]|[・、,\/][1-6])*)(?!\d)/g)){
   const days=[...m[1]].filter(x=>'月火水木金土日'.includes(x)).map(x=>'月火水木金土日'.indexOf(x)+1);let ps=[];
   for(const p of m[2].split(/[・、,\/]/)){const r=p.split(/[-~〜～]/).map(Number);if(r.length===2)for(let n=r[0];n<=r[1];n++)ps.push(n);else ps.push(r[0]);}
   days.forEach(day=>ps.forEach(period=>out.push({day,period})));
  }return out;
 }
 function selectedRows(snapshot,selected,now=Date.now()){
  assert(snapshot&&now>=snapshot.at&&now-snapshot.at<=300000,'课程状态已过期，请重新读取');assert(selected.length&&new Set(selected).size===selected.length,'请先勾选课程');const rows=snapshot.rows.filter(r=>selected.includes(r.id));assert(rows.length===selected.length&&rows.every(r=>r.available),'所选课程已不可登记');return rows;
 }
 function reconcile(before,after,selected){if(!before||before.student!==after.student||before.scope!==after.scope)return [];return selected.filter(id=>{const a=before.rows.find(r=>r.id===id),b=after.rows.find(r=>r.id===id);return a&&b&&b.available&&JSON.stringify(a)===JSON.stringify(b);});}
 function delivery(value){const v=(value||'').toLowerCase();if(/blended|hybrid|ブレンド|混合|ハイブリッド|併用/.test(v))return '混合授课';if(/対面|face-to-face/.test(v))return /オンライン|on-?line/.test(v)?'混合授课':'线下授课';if(/オンライン|on-?line/.test(v))return /オンデマンド|on.?demand/.test(v)?'线上授课（录播）':/リアルタイム|real.?time/.test(v)?'线上授课（实时）':'线上上课';return '';}
 function venue(year,code,value){if(year===2026&&['KB4012','KB2004'].includes(code))return '按每次授课通知确认';return delivery(value)||(year===2026&&['KB7003','KB9304','KB9307','KB9420','KB9422'].includes(code)?'线上上课':'教室待确认');}
 function events(snapshots,data,affiliation=''){
  const merged=new Map(),issues=[];
  for(const t of Object.values(snapshots).filter(t=>t.lessons).sort((a,b)=>(a.at||0)-(b.at||0))){for(const l of t.lessons){try{
   assert(t.year===data.year&&l.day<=5&&l.period<=5,'校历年份或节次尚未核对');const o=data.offerings[l.code],explicit=[...l.name.matchAll(/【(前期|前学期|後期|後学期|通年|[1-4]Q)】/g)].map(m=>m[1].replace('前学期','前期').replace('後学期','後期'));
   assert(new Set(explicit).size<=1&&(!explicit.length||!o||explicit[0]===o.term),'学期标记冲突');assert(!o?.irregular,'集中或隔周授课需单独核对');const term=explicit[0]||o?.term,isQ=term===t.quarter+'Q',isS=term==='通年'||term===(t.quarter<=2?'前期':'後期');assert(isQ!==isS,'授课学季尚未确认');
   const campus=o?.campus||(/水戸/.test(l.name)||l.code.startsWith('KB')||/人文社会科学部|教育学部|理学部|地域未来共創学環/.test(affiliation)?'MITO':/日立/.test(l.name)?'HITACHI':/阿見/.test(l.name)?'AMI':null);
   assert(t.quarter!==3||l.day!==5||campus,'授课校区尚未确认');let dates=data.dates[t.quarter-1][l.day-1].split(' ');
   if(t.quarter===3&&l.day===5){if(campus==='HITACHI')dates='09-25 10-02 10-09 10-16 10-23 10-30 11-13'.split(' ');if(campus==='AMI')dates='09-25 10-02 10-09 10-16 10-30 11-06 11-13'.split(' ');}
   if(isS){if(t.quarter===2&&l.day===2)dates='06-09 06-16 06-23 06-30 07-07 07-14 07-21'.split(' ');if(t.quarter===3&&l.day>=2)dates.push({2:'11-24',3:'11-18',4:'11-19',5:'11-20'}[l.day]);if(t.quarter===4&&l.day>=2)dates.pop();}
   dates.forEach(d=>{const date=(t.quarter===4&&Number(d.slice(0,2))<=3?2027:2026)+'-'+d;merged.set([date,l.period,l.code].join('/'),{...l,date,year:t.year,quarter:t.quarter});});
  }catch(e){issues.push(l.code+'：'+e.message);}}}return {events:[...merged.values()],issues:[...new Set(issues)]};
 }
 function dateString(date){return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;}
 function dayDate(s){const [y,m,d]=s.split('-').map(Number);return new Date(y,m-1,d,12);}
 function shift(s,days){const d=dayDate(s);d.setDate(d.getDate()+days);return dateString(d);}
 function monday(s){const d=dayDate(s);return shift(s,-((d.getDay()+6)%7));}
 function termOn(s){if(s<'2026-04-06'||s>='2027-04-05')return null;return s>='2026-11-23'?'2026 · Q4':s>='2026-09-21'?'2026 · Q3':s>='2026-06-01'?'2026 · Q2':'2026 · Q1';}
 function changes(old,current){if(!old||old.student!==current.student)return [];const out=[];if(current.earned>old.earned)out.push(`已修学分 ${old.earned} → ${current.earned}`);if(Number.isFinite(old.gpa)&&Number.isFinite(current.gpa)&&old.gpa!==current.gpa)out.push(`GPA ${old.gpa} → ${current.gpa}`);return out;}
 const api={identity,profile,parse,readOnlyRegistration,slots,selectedRows,reconcile,delivery,venue,events,dateString,dayDate,shift,monday,termOn,changes};root.CampusCore=api;if(typeof module!=='undefined')module.exports=api;
})(globalThis);
