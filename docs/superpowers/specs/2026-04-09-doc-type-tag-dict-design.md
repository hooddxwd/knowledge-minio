# 文档标签数据字典化设计方案

## 背景

当前 `DocTypeTagConfig.js` 中标签字段的下拉选项写死在代码里，不便于业务人员维护。将这些选项迁移到低代码平台的数据字典中，支持在线配置。

## 迁移范围

| 字段 | 字典码 | 说明 |
|------|--------|------|
| 密级 | `classification_level` | 从 `CLASSIFICATION_LEVELS` 迁移 |
| 科技报告-任务来源/渠道 | `doc_report_task_source` | |
| 科技报告-型号 | `doc_report_model` | |
| 科技报告-专业 | `doc_report_major` | |
| 科技报告-类别 | `doc_report_category` | |
| 标准规范-标准层次 | `standard_level` | |
| 标准规范-标准领域 | `doc_std_field` | |
| 标准规范-军民属性 | `doc_std_civil_military` | |
| 标准规范-适用航空器类型 | `doc_std_aircraft_type` | |
| 标准规范-标准类型 | `doc_std_type` | |
| 新闻资讯-新闻类型 | `doc_news_type` | |
| 新闻资讯-新闻专业 | `doc_news_major` | |
| 规章制度-业务域 | `doc_rule_domain` | |
| 规章制度-制度等级 | `doc_rule_level` | |
| 规章制度-主责部门 | `doc_rule_dept` | |
| 期刊文献-来源数据库 | `doc_journal_db` | |
| 期刊文献-学科 | `doc_journal_subject` | |
| 期刊文献-论文级别 | `doc_journal_level` | |
| 期刊文献-行业分类 | `doc_journal_industry` | |

**不迁移**：IP子类型（专利标签、科技成果、软件著作权），行政公文（`type: 'input'` 的纯输入字段）。

## 数据流

```
DocTypeTagConfig.js (dictCode 映射)
    ↓
FileUploadModal.vue / FileEditModal.vue
    → JDictSelectTag :dictCode="tf.dictCode"
    → 通过 ajaxGetDictItems(dictCode) 从后端获取选项
    → 数据字典管理后台在线配置各字典项
```

## 实施方案

### 1. DocTypeTagConfig.js 改造

- 移除 `tagFields[].options`，替换为 `dictCode`
- `type: 'select'` → 用 `JDictSelectTag`，`type: 'input'` → 保持 `a-input`
- `CLASSIFICATION_LEVELS` 改为导出 `CLASSIFICATION_LEVEL_DICT_CODE = 'classification_level'`
- `getTagFieldsByDocType` 返回值结构不变，新增 `dictCode` 字段

### 2. FileUploadModal.vue 改造

- 引入 `JDictSelectTag` 组件
- 模板 select 类型字段：`<JDictSelectTag :dictCode="tf.dictCode" v-model="form[tf.field]" placeholder="请选择" allowClear />`
- 移除 `DOC_TYPES`/`IP_SUB_TYPES`/`CLASSIFICATION_LEVELS` 的遍历渲染，改用 `JDictSelectTag`

### 3. FileEditModal.vue 改造

- 同 FileUploadModal.vue，引入 `JDictSelectTag`，替换模板中的 `<a-select>`（密级和 tagFields）

### 4. MinioFileList.vue

- 侧边栏筛选标签来自后端统计聚合数据，不受影响
- 文件列表中标签展示结构不变

### 5. 数据字典初始化（系统管理 → 字典管理）

- `classification_level`：公开、内部
- `doc_report_task_source`：国家任务、省市任务、自研项目、国际合作、其他
- `doc_report_model`：WS-15、WS-20、CJ-1000、其他
- `doc_report_major`：材料工程、结构设计、气动设计、控制系统、推进系统、其他
- `doc_report_category`：研究报告、试验报告、设计报告、技术报告、其他
- `standard_level`：国家标准、行业标准、企业标准、国际标准、其他
- `doc_std_field`：航空、航天、电子、机械、材料、其他
- `doc_std_civil_military`：军用、民用、军民两用
- `doc_std_aircraft_type`：固定翼、旋翼机、无人机、通用航空、其他
- `doc_std_type`：基础标准、产品标准、方法标准、管理标准、其他
- `doc_news_type`：行业动态、科研进展、政策法规、会议报道、其他
- `doc_news_major`：气动、结构、材料、控制、推进、其他
- `doc_rule_domain`：科研管理、人力资源、财务管理、质量管理、其他
- `doc_rule_level`：所级、部门级、国家级、行业级、其他
- `doc_rule_dept`：科研部、技术部、质量部、综合管理部、其他
- `doc_journal_db`：CNKI、万方、维普、Web of Science、其他
- `doc_journal_subject`：航空宇航科学与技术、材料科学与工程、机械工程、动力工程、其他
- `doc_journal_level`：SCI、EI、核心期刊、会议论文、其他
- `doc_journal_industry`：航空运输、航天器制造、航空设备制造、其他
