# 学分规则核对 / Credit rule review / 履修要件の確認

适用版本 1.0.82。核对日期：2026-09-18。两端共用 Android 导出的 198 个学部／学科／年度／项目规则组合，当前年度范围为 2024–2026。组合数量不是“全部毕业条件已经自动化”的证明。

## 本次修正

- `x/y` 的 y 是毕业最低学分，不能当作最多可修学分。未单列最低值的子类显示“已修 x 学分”，保留预计新增，不显示虚假的 `/0` 或完成进度条。
- 初修外国语、共生、P&A 的同一学期履修额度须合并核对，不能因子类没有独立最低学分就认定无限制。前期为 Q1+Q2、后期为 Q3+Q4。
- 基盘教育的初修外国语与人文社会科学部专业科目的第二外国语分别核对。没有将未经确认的“所有人都必须 2 学分”写进所有分类。
- 只用已合格、有效且能够识别类别的学校成绩核对最低值；不合格、预计新增、专业或毕业要件外记录不会冒充基盘必修已完成。未加载成绩显示“尚未同步”；已加载且可分类的成绩不足显示“已保存成绩尚不足”；存在未分类已修成绩才显示“待核对分类”。

## 通识规则范围

| 学部／学環 | 核对重点 | 共通教育履修案内印刷页 |
| --- | --- | --- |
| 人文社会科学部（三学科） | 多文化合计 2；专业与基盘外国语分开、不得同语言；人间文化一年级初修外语限制 | 42–44 |
| 教育学部（两課程） | 日本国憲法 2 必修；身体活動合计 2；超额转入自由履修 | 46–48 |
| 理学部 | 指定项目两类各 2 的例外；2024 身体活動修正表 | 49–51 |
| 工学部（五学科） | 2026 机械的環境と人間 1 必修；2024 身体活動修正表；信息工学详细分类 | 52–54 |
| 農学部（两学科及项目） | 通识组合、选修与自由履修转入；按对应年度专业结构 | 55–57 |
| 地域未来共創学環 | 多文化 1+2、自然社会 2+4、アントレプレナーシップⅠ／Ⅱ各 1，不能跨类互抵 | 58–60 |

来源：[SSC 当前资料](https://sites.google.com/g.ibaraki.ac.jp/ssc/guidebooks) · [往年资料](https://sites.google.com/g.ibaraki.ac.jp/ssc/ガイドブック類のバックナンバー) · [2024 案内](https://drive.google.com/file/d/1Ddr855lCNPrbGKrKBf6-9df9cCBDrVRA/view) · [2025 案内](https://drive.google.com/file/d/1ij3Y-7w_7L9hOA64TeMxksg_jEtjd_G3/view) · [2026 案内](https://drive.google.com/file/d/1U-r7ebg_4NjCMWCn3sFjuEf5htQ8q_-_/view) · [2024 修正表](https://drive.google.com/file/d/1e6qHOUlbj7_V3q94qwD9nyOnJO6C9mrs/view)。

## 仍须核对的条件

已显示英语先修、同题目重复修读、留学生与分班资格、抽签、出席、学分认定及 CAP 提醒。审批状态、指定班级、出席记录、专业主副修、实验实习、毕业研究着手与审查等没有完整的结构化学校数据时，不能自动标为通过。

CAP 是“登记”限制，不是“已取得”限制，不合格课程也不能简单从年度登记量中扣除。不同学部对集中讲义、认定学分和资格课程的除外范围不同。工学部的放宽涉及最近学期 GPA 2.75 及学校批准；教育学部涉及年度 GPA 等条件。通算 GPA 不能替代年度／学期 GPA，也不能代替批准。

专业逐项条件仍按 [人文](https://www.hum.ibaraki.ac.jp/reference/for-enrolled.html)、[教育](https://www.edu.ibaraki.ac.jp/students/zaigaku/)、[理学](https://www.sci.ibaraki.ac.jp/collegelife/curriculum/)、[工学](https://www.eng.ibaraki.ac.jp/education/class/)、[農学](https://www.agr.ibaraki.ac.jp/teaching/course/)、[地域未来](https://www.mirai.ibaraki.ac.jp/students/) 的对应年度文件核对。App 的总学分满足不是学校毕业判定。

## 扫描与验证边界

对本机保存的专业资料进行必修、上限、先修、除外、第二外国語、卒業研究、実習、認定、条件关键词索引扫描，并核对上述通识规则原文与修正表。部分 PDF 有特殊字体编码，2025 数字提取存在异常，采用页面图像复核关键数字。关键词扫描不等于逐条专业条件已经形式化，也不能证明没有遗漏；不把未确认条目记作“无限制”。

Android 测试覆盖适用学部／年度隔离、未知年度、必修成绩筛选、可选子类显示、日历季度联动、同步失败保留数据和选课解析。iOS 测试检查相同规则导出、分类结果、课表解析及勾选状态。真实学校响应和审批状态仍以学校系统为准。

## English

This release distinguishes graduation minima from enrollment caps, removes misleading zero-denominator progress, and checks reviewed common-education requirements against awarded grades. Rules are scoped by faculty and 2024–2026 cohort. Approval, placement, attendance, professional prerequisites and thesis requirements remain unresolved when supporting records are unavailable. A keyword scan is not proof that every professional rule has been automated. Do not interpret a satisfied credit total as university graduation approval.

## 日本語

卒業最低単位数と履修登録上限を区別し、最低数が独立して設定されていない区分の `/0` 表示を修正しました。確認済みの共通教育要件は学部・要件年度別に修得済み成績で確認します。許可、クラス指定、出席、専門科目の先修条件、卒業研究など、根拠データがない条件は未確認のままです。キーワード検索は専門要件すべての自動判定を保証しません。
