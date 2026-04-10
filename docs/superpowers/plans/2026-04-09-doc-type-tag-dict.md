# 文档标签数据字典化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `DocTypeTagConfig.js` 中硬编码的标签下拉选项迁移到数据字典，用 `JDictSelectTag` 组件替代原生 `a-select`

**Architecture:** 改动集中在前端：`DocTypeTagConfig.js` 的 `tagFields[].options` 替换为 `dictCode`，表单组件改用 `JDictSelectTag` 渲染下拉。SQL 初始化脚本已生成，待数据库执行。

**Tech Stack:** Vue 2, Ant Design Vue, Jeecg 数据字典组件 `JDictSelectTag`

---

## 变更文件清单

| 文件 | 改动 |
|------|------|
| `ant-design-vue-jeecg/src/views/miniofile/DocTypeTagConfig.js` | `tagFields[].options` → `dictCode` |
| `ant-design-vue-jeecg/src/views/miniofile/FileUploadModal.vue` | 引入 `JDictSelectTag`，模板替换 `a-select` |
| `ant-design-vue-jeecg/src/views/miniofile/FileEditModal.vue` | 引入 `JDictSelectTag`，模板替换 `a-select` |

---

## Task 1: 改造 DocTypeTagConfig.js

**文件:** `ant-design-vue-jeecg/src/views/miniofile/DocTypeTagConfig.js`

- [ ] **Step 1: 替换 `CLASSIFICATION_LEVELS` 为字典码常量**

```js
// 改前
export const CLASSIFICATION_LEVELS = ['公开', '内部']

// 改后
export const CLASSIFICATION_LEVEL_DICT_CODE = 'classification_level'
```

- [ ] **Step 2: 科技报告 tagFields 替换 options 为 dictCode**

```js
// 改前
{ field: 'reportTaskSource', label: '任务来源/渠道', type: 'select', options: ['国家任务', '省市任务', '自研项目', '国际合作', '其他'] },
{ field: 'reportModel', label: '型号', type: 'select', options: ['WS-15', 'WS-20', 'CJ-1000', '其他'] },
{ field: 'reportMajor', label: '专业', type: 'select', options: ['材料工程', '结构设计', '气动设计', '控制系统', '推进系统', '其他'] },
{ field: 'reportCategory', label: '类别', type: 'select', options: ['研究报告', '试验报告', '设计报告', '技术报告', '其他'] },

// 改后
{ field: 'reportTaskSource', label: '任务来源/渠道', type: 'select', dictCode: 'doc_report_task_source' },
{ field: 'reportModel', label: '型号', type: 'select', dictCode: 'doc_report_model' },
{ field: 'reportMajor', label: '专业', type: 'select', dictCode: 'doc_report_major' },
{ field: 'reportCategory', label: '类别', type: 'select', dictCode: 'doc_report_category' },
```

- [ ] **Step 3: 标准规范 tagFields 替换 options 为 dictCode**

```js
// 改前
{ field: 'stdLevel', label: '标准层次', type: 'select', options: ['国家标准', '行业标准', '企业标准', '国际标准', '其他'] },
{ field: 'stdField', label: '标准领域', type: 'select', options: ['航空', '航天', '电子', '机械', '材料', '其他'] },
{ field: 'stdCivilMilitary', label: '军民属性', type: 'select', options: ['军用', '民用', '军民两用'] },
{ field: 'stdAircraftType', label: '适用航空器类型', type: 'select', options: ['固定翼', '旋翼机', '无人机', '通用航空', '其他'] },
{ field: 'stdType', label: '标准类型', type: 'select', options: ['基础标准', '产品标准', '方法标准', '管理标准', '其他'] },

// 改后
{ field: 'stdLevel', label: '标准层次', type: 'select', dictCode: 'standard_level' },
{ field: 'stdField', label: '标准领域', type: 'select', dictCode: 'doc_std_field' },
{ field: 'stdCivilMilitary', label: '军民属性', type: 'select', dictCode: 'doc_std_civil_military' },
{ field: 'stdAircraftType', label: '适用航空器类型', type: 'select', dictCode: 'doc_std_aircraft_type' },
{ field: 'stdType', label: '标准类型', type: 'select', dictCode: 'doc_std_type' },
```

- [ ] **Step 4: 新闻资讯 tagFields 替换 options 为 dictCode**

```js
// 改前
{ field: 'newsType', label: '新闻类型', type: 'select', options: ['行业动态', '科研进展', '政策法规', '会议报道', '其他'] },
{ field: 'newsMajor', label: '新闻专业', type: 'select', options: ['气动', '结构', '材料', '控制', '推进', '其他'] },

// 改后
{ field: 'newsType', label: '新闻类型', type: 'select', dictCode: 'doc_news_type' },
{ field: 'newsMajor', label: '新闻专业', type: 'select', dictCode: 'doc_news_major' },
```

- [ ] **Step 5: 规章制度 tagFields 替换 options 为 dictCode**

```js
// 改前
{ field: 'ruleDomain', label: '业务域', type: 'select', options: ['科研管理', '人力资源', '财务管理', '质量管理', '其他'] },
{ field: 'ruleLevel', label: '制度等级', type: 'select', options: ['所级', '部门级', '国家级', '行业级', '其他'] },
{ field: 'ruleDept', label: '主责部门', type: 'select', options: ['科研部', '技术部', '质量部', '综合管理部', '其他'] },

// 改后
{ field: 'ruleDomain', label: '业务域', type: 'select', dictCode: 'doc_rule_domain' },
{ field: 'ruleLevel', label: '制度等级', type: 'select', dictCode: 'doc_rule_level' },
{ field: 'ruleDept', label: '主责部门', type: 'select', dictCode: 'doc_rule_dept' },
```

- [ ] **Step 6: 期刊文献 tagFields 替换 options 为 dictCode**

```js
// 改前
{ field: 'journalDb', label: '来源数据库', type: 'select', options: ['CNKI', '万方', '维普', 'Web of Science', '其他'] },
{ field: 'journalSubject', label: '学科', type: 'select', options: ['航空宇航科学与技术', '材料科学与工程', '机械工程', '动力工程', '其他'] },
{ field: 'journalLevel', label: '论文级别', type: 'select', options: ['SCI', 'EI', '核心期刊', '会议论文', '其他'] },
{ field: 'journalIndustry', label: '行业分类', type: 'select', options: ['航空运输', '航天器制造', '航空设备制造', '其他'] },

// 改后
{ field: 'journalDb', label: '来源数据库', type: 'select', dictCode: 'doc_journal_db' },
{ field: 'journalSubject', label: '学科', type: 'select', dictCode: 'doc_journal_subject' },
{ field: 'journalLevel', label: '论文级别', type: 'select', dictCode: 'doc_journal_level' },
{ field: 'journalIndustry', label: '行业分类', type: 'select', dictCode: 'doc_journal_industry' },
```

- [ ] **Step 7: 提交**

```bash
git add ant-design-vue-jeecg/src/views/miniofile/DocTypeTagConfig.js
git commit -m "refactor(miniofile): tagFields options -> dictCode"
```

---

## Task 2: 改造 FileUploadModal.vue

**文件:** `ant-design-vue-jeecg/src/views/miniofile/FileUploadModal.vue`

- [ ] **Step 1: 引入 JDictSelectTag，替换导入**

```js
// 改前
import { ACCESS_TOKEN } from '@/store/mutation-types'
import Vue from 'vue'
import { DOC_TYPES, IP_SUB_TYPES, CLASSIFICATION_LEVELS } from './DocTypeTagConfig'

// 改后
import { ACCESS_TOKEN } from '@/store/mutation-types'
import Vue from 'vue'
import { DOC_TYPES, IP_SUB_TYPES, CLASSIFICATION_LEVEL_DICT_CODE } from './DocTypeTagConfig'
import JDictSelectTag from '@/components/dict/JDictSelectTag'

export default {
  name: 'FileUploadModal',
  components: { JDictSelectTag },
```

- [ ] **Step 2: data 中替换 CLASSIFICATION_LEVELS 为字典码**

```js
// 改前
CLASSIFICATION_LEVELS,

// 改后
CLASSIFICATION_LEVEL_DICT_CODE,
```

- [ ] **Step 3: 模板中密级下拉替换为 JDictSelectTag**

```html
<!-- 改前 -->
<a-form-model-item label="文档密级" required>
  <a-select v-model="form.classificationLevel" placeholder="请选择密级">
    <a-select-option v-for="level in CLASSIFICATION_LEVELS" :key="level" :value="level">{{ level }}</a-select-option>
  </a-select>
</a-form-model-item>

<!-- 改后 -->
<a-form-model-item label="文档密级" required>
  <JDictSelectTag v-model="form.classificationLevel" dictCode="classification_level" placeholder="请选择" allowClear />
</a-form-model-item>
```

- [ ] **Step 4: 模板中 tagFields 遍历替换 select 类型为 JDictSelectTag**

```html
<!-- 改前 -->
<a-form-model-item v-for="tf in localTagFields" :key="tf.field" :label="tf.label">
  <a-select v-if="tf.type === 'select'" v-model="form[tf.field]" placeholder="请选择" allowClear>
    <a-select-option v-for="opt in tf.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
  </a-select>
  <a-input v-else v-model="form[tf.field]" :placeholder="'请输入' + tf.label" />
</a-form-model-item>

<!-- 改后 -->
<a-form-model-item v-for="tf in localTagFields" :key="tf.field" :label="tf.label">
  <JDictSelectTag v-if="tf.type === 'select'" v-model="form[tf.field]" :dictCode="tf.dictCode" placeholder="请选择" allowClear />
  <a-input v-else v-model="form[tf.field]" :placeholder="'请输入' + tf.label" />
</a-form-model-item>
```

- [ ] **Step 5: show 方法中默认密级改为字典值（不变，因为字典值和原值相同）**

```js
// form.classificationLevel = '公开' 保持不变
```

- [ ] **Step 6: 提交**

```bash
git add ant-design-vue-jeecg/src/views/miniofile/FileUploadModal.vue
git commit -m "feat(miniofile): use JDictSelectTag for tag fields and classification level"
```

---

## Task 3: 改造 FileEditModal.vue

**文件:** `ant-design-vue-jeecg/src/views/miniofile/FileEditModal.vue`

- [ ] **Step 1: 引入 JDictSelectTag，替换导入**

```js
// 改前
import { putAction } from '@/api/manage'
import { CLASSIFICATION_LEVELS } from './DocTypeTagConfig'

// 改后
import { putAction } from '@/api/manage'
import JDictSelectTag from '@/components/dict/JDictSelectTag'

export default {
  name: 'FileEditModal',
  components: { JDictSelectTag },
```

- [ ] **Step 2: data 中移除 CLASSIFICATION_LEVELS**

```js
// 改前
CLASSIFICATION_LEVELS,
visible: false,

// 改后
visible: false,
```

- [ ] **Step 3: 模板中密级下拉替换为 JDictSelectTag**

```html
<!-- 改前 -->
<a-form-model-item label="文档密级" required>
  <a-select v-model="form.classificationLevel" placeholder="请选择密级">
    <a-select-option v-for="level in CLASSIFICATION_LEVELS" :key="level" :value="level">{{ level }}</a-select-option>
  </a-select>
</a-form-model-item>

<!-- 改后 -->
<a-form-model-item label="文档密级" required>
  <JDictSelectTag v-model="form.classificationLevel" dictCode="classification_level" placeholder="请选择" allowClear />
</a-form-model-item>
```

- [ ] **Step 4: 模板中 tagFields 遍历替换 select 类型为 JDictSelectTag**

```html
<!-- 改前 -->
<a-form-model-item v-for="tf in tagFields" :key="tf.field" :label="tf.label">
  <a-select v-if="tf.type === 'select'" v-model="form[tf.field]" allowClear>
    <a-select-option v-for="opt in tf.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
  </a-select>
  <a-input v-else v-model="form[tf.field]" />
</a-form-model-item>

<!-- 改后 -->
<a-form-model-item v-for="tf in tagFields" :key="tf.field" :label="tf.label">
  <JDictSelectTag v-if="tf.type === 'select'" v-model="form[tf.field]" :dictCode="tf.dictCode" placeholder="请选择" allowClear />
  <a-input v-else v-model="form[tf.field]" />
</a-form-model-item>
```

- [ ] **Step 5: 提交**

```bash
git add ant-design-vue-jeecg/src/views/miniofile/FileEditModal.vue
git commit -m "feat(miniofile): use JDictSelectTag in FileEditModal"
```

---

## Task 4: 数据库初始化（手动）

- [ ] **Step 1: 执行 SQL 脚本**

在 MySQL 客户端执行以下脚本：
```
jeecg-boot/db/增量SQL/文档标签数据字典初始化.sql
```

或在 Navicat/DBeaver 中导入该文件。

---

## 验证步骤

1. **启动后端和前端**，进入文件上传页面
2. 选择任一文档类型（如"科技报告"），确认下拉选项正常加载
3. 选择"标准规范"，确认标准层次等下拉选项正常加载
4. 编辑已有文件，确认编辑弹窗中密级和标签字段下拉正常
5. 确认行政公文的纯输入字段（来文单位、主办单位等）不受影响
6. 确认知识产权子类型的输入字段不受影响
