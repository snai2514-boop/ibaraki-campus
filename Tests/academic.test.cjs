const {test}=require('node:test'),a=require('node:assert/strict');
require('../CampusAssistant/Web/academic-data.js');require('../CampusAssistant/Web/calendar-data.js');
const A=require('../CampusAssistant/Web/academic.js'),D=CampusAcademicData,C=CampusCalendarData;
const scope={faculty:'人文社会科学部',department:'法律経済学科',cohort:2026,program:''};
const grade={student:'DEMO0000',rows:[]};
const table=(q,lessons)=>({student:'DEMO0000',year:2026,quarter:q,lessons});
const lesson=(code,name,credits=2)=>({code,name,description:code+' '+name+' '+credits+'単位',credits,day:1,period:1});
test('all 198 Android curriculum scopes preserve valid unique requirement trees',()=>{
 a.equal(D.curricula.length,198);for(const r of D.curricula){const ids=new Set();function check(n){a.ok(!ids.has(n.id));ids.add(n.id);a.ok(n.required>=0);n.children.forEach(check);}r.requirements.forEach(check);if(r.requirements.length)a.equal(r.requirements[0].required,124);}
});
test('humanities graduate gaps use selected curriculum and actual categories',()=>{const g=[{name:'大学入門ゼミ',credits:2,passed:true,category:'基盤教育科目'},{name:'演習',credits:3,passed:true,category:'学科専門科目'},{name:'未認識',credits:2,passed:true,category:''},{name:'不合格',credits:4,passed:false,category:'学科専門科目'}],p=A.progress(scope,g,D);a.equal(p.nodes[0].earned,5);a.equal(p.nodes[0].remaining,119);a.equal(p.unassigned,2);a.equal(p.nodes[0].children.find(n=>n.label==='学科専門科目').remaining,47);});
test('no inference from student year or number; only requirement year selects rules',()=>{a.equal(A.rule({...scope,cohort:null},D),undefined);a.equal(A.rule({...scope,cohort:2023},D),undefined);a.equal(A.scope({affiliation:'人文社会科学部 法律経済学科',year:'1',student:'20260000'},D).cohort,undefined);});
test('biology merged standard and advanced credits counted once',()=>{const s={faculty:'理学部',department:'理学科',cohort:2026,program:'生物科学コース'},p=A.progress(s,[{name:'任意',credits:2,passed:true,category:'標準科目'},{name:'任意2',credits:3,passed:true,category:'発展科目'}],D),n=p.nodes[0].children.find(n=>n.label==='専門標準・発展科目');a.equal(n.required,46);a.equal(n.earned,5);});
test('outside requirement credits never fill graduation gaps',()=>{const p=A.progress(scope,[{name:'大学入門ゼミ',credits:2,passed:true,category:'卒業要件外'}],D);a.equal(p.nodes[0].earned,0);a.equal(p.unassigned,0);});
test('semester forecast deduplicates meetings and two quarters, counts at ending quarter',()=>{const l=lesson('ZZ1','試験科目【後期】');const snapshots={q3:table(3,[l,{...l,day:2}]),q4:table(4,[l])},f=A.forecast(grade,snapshots,2026,[3,4],C,scope.faculty,D);a.equal(f.added,2);a.equal(f.courses.length,1);a.equal(A.forecast(grade,snapshots,2026,[3],C,scope.faculty,D).added,0);});
test('forecast excludes failed results and already awarded credits',()=>{const snapshots={q3:table(3,[lesson('ZZ1','已修【3Q】'),lesson('ZZ2','不合格【3Q】'),lesson('ZZ3','在修【3Q】')])},g={...grade,rows:[{name:'已修',credits:2,year:'2026',term:'3クォーター',passed:true},{name:'不合格',credits:2,year:'2026',term:'3クォーター',passed:false}]},f=A.forecast(g,snapshots,2026,[3,4],C,scope.faculty,D);a.equal(f.completed,2);a.equal(f.failed,2);a.equal(f.added,2);a.equal(f.total,4);a.deepEqual(f.missing,[4]);});
test('ambiguous duration, reused names and another account remain uncounted',()=>{const l=lesson('ZZ1','未知');const f=A.forecast(grade,{q3:table(3,[l]),other:{...table(4,[lesson('ZZ2','别人的课【4Q】')]),student:'OTHER000'}},2026,[3,4],C,scope.faculty,D);a.equal(f.added,0);a.deepEqual(f.missing,[4]);a.equal(f.courses[0].state,'待核对，不计预览');});
test('science fallback requires exact faculty, name, credits, day and period',()=>{const l={name:'アルゴリズム論',credits:2,day:4,period:2};a.equal(A.science(l,3,'理学部',D).term,'後期');a.equal(A.science(l,3,'工学部',D),null);a.equal(A.science({...l,period:3},3,'理学部',D),null);});
test('same code offered in each semester remains separate',()=>{const f=A.forecast(grade,{q1:table(1,[lesson('ZZ1','再利用【1Q】')]),q3:table(3,[lesson('ZZ1','再利用【3Q】')])},2026,[1,2,3,4],C,scope.faculty,D);a.equal(f.added,4);});
test('ICS escapes injection, preserves Tokyo times, folds Unicode by bytes',()=>{const text=A.ics([{id:'x',name:'授業\nEND:VEVENT,;'+('日本語'.repeat(40)),date:'2026-09-21',period:1}]);a.ok(text.includes('DTSTART;TZID=Asia/Tokyo:20260921T084000'));a.ok(text.includes('\\nEND:VEVENT\\,\\;'));for(const line of text.split('\r\n'))a.ok(Buffer.byteLength(line)<=75);});

test("Android and iOS course classification produce identical reference results",()=>{const cases=require("./classification-parity.json");a.ok(cases.length>300);for(const x of cases){const c=A.classify(x.scope,x.course,D);a.deepEqual({id:c.id,label:c.label,excluded:c.excluded},x.expected,JSON.stringify(x));}});

test("manual GPA counts failed grades, excludes recognized credit and rejects duplicates",()=>{const rows=[{id:"a",name:"a",credits:2,kind:"SCORED",score:85},{id:"b",name:"b",credits:2,kind:"SCORED",score:59},{id:"c",name:"c",credits:2,kind:"RECOGNIZED"}];a.equal(A.manualGpa(rows),1.5);a.throws(()=>A.manualGpa([...rows,rows[0]]));a.equal(A.manualGpa([]),null);});


test('optional credit rows retain credits and forecast without zero ratios or completion bars',()=>{
 const vm=require('node:vm'),fs=require('node:fs');
 const context=vm.createContext({CampusAcademic:A,CampusAcademicData:D,h:s=>String(s)});
 vm.runInContext(fs.readFileSync(require('node:path').join(__dirname,'../CampusAssistant/Web/academic-ui.js'),'utf8'),context);
 const optional={label:'Optional',earned:2,required:0,remaining:0,added:1,projectedRemaining:0,children:[]};
 const html=context.renderCreditTree([optional,{...optional,earned:0,added:0}]);
 a.match(html,/已修 2 学分/);a.match(html,/已修 0 学分/);a.match(html,/预计新增 \+1/);
 a.doesNotMatch(html,/<progress|\/ 0|还需/);
 const parent=context.renderCreditTree([{...optional,required:3,remaining:1,projectedRemaining:0,children:[optional]}]);
 a.match(parent,/2 \/ 3/);a.equal((parent.match(/<progress/g)||[]).length,1);a.match(parent,/还需 1 学分/);
});

test('scoped awarded-credit checks preserve missing data and reject failed or excluded grades',()=>{
 const s={faculty:'教育学部',department:'養護教諭養成課程',cohort:2026,program:''},g={name:'日本国憲法',category:'基盤教育科目',credits:2,passed:true};
 const check=rows=>A.constraintChecks(s,rows,D).find(x=>x.id==='constitution');
 a.equal(check(null).status,'尚未同步');a.equal(check([g]).status,'已满足学分数');a.equal(check([{...g,passed:false}]).status,'已保存成绩尚不足');a.equal(check([{...g,category:'卒業要件外'}]).earned,0);
 a.ok(!A.constraintChecks({faculty:'工学部',department:'情報工学科',cohort:2026,program:''},[g],D).some(x=>x.id==='constitution'));
});

test('unclassified awards are distinguished from a simple saved-credit deficit',()=>{const r=A.constraintChecks(scope,[{name:'未知通识课程',category:'基盤教育科目',credits:2,passed:true}],D);a.equal(r[0].status,'待核对分类');a.match(r[0].note,/另有 2 学分/);a.equal(A.constraintChecks(scope,[],D)[0].status,'已保存成绩尚不足');});
