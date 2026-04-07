# MinIO 文件管理系统 - 前端实现计划

> **前置条件：** 后端 Task 1-7 已完成（见 `2026-04-03-minio-file-management.md`）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现前端文件夹列表页和文件管理页，包含标签页切换、左侧标签筛选树、右侧卡片列表、上传/编辑弹窗。

**Tech Stack:** Vue 2, Ant Design Vue 1.x, JeecgListMixin, Axios

---

## Task 8: MinioFolderList.vue 文件夹列表页

**Files:**
- Create: `ant-design-vue-jeecg/src/views/miniofile/MinioFolderList.vue`

**参考模式：** 使用 `JeecgListMixin` 实现标准 CRUD 列表。参考项目中任意已有的列表页（如 `views/system/UserList.vue`）。

- [ ] **Step 1: 创建 MinioFolderList.vue**

标准 CRUD 列表页，点击行可跳转到对应的文件管理页。复用 `JeecgListMixin` 的 `loadData`、`handleAdd`、`handleEdit`、`handleDelete`、`handleTableChange` 等方法。

```vue
<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline" @keyup.enter.native="searchQuery">
        <a-row :gutter="24">
          <a-col :md="8" :sm="12">
            <a-form-item label="文件夹名称">
              <a-input placeholder="请输入文件夹名称" v-model="queryParam.folderName"></a-input>
            </a-form-item>
          </a-col>
          <a-col :md="8" :sm="12">
            <span style="float: left; overflow: hidden" class="table-page-search-submitButtons">
              <a-button type="primary" @click="searchQuery" icon="search">查询</a-button>
              <a-button type="primary" @click="searchReset" icon="reload" style="margin-left: 8px">重置</a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <!-- 操作按钮区域 -->
    <div class="table-operator">
      <a-button @click="handleAdd" type="primary" icon="plus">新建文件夹</a-button>
    </div>

    <!-- 表格区域 -->
    <a-table
      ref="table"
      size="middle"
      bordered
      rowKey="id"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="ipagination"
      :loading="loading"
      @change="handleTableChange"
    >
      <span slot="action" slot-scope="text, record">
        <a @click="goToFileList(record)">进入</a>
        <a-divider type="vertical" />
        <a @click="handleEdit(record)">编辑</a>
        <a-divider type="vertical" />
        <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(record.id)">
          <a style="color: #ff4d4f">删除</a>
        </a-popconfirm>
      </span>
    </a-table>

    <!-- 新建/编辑弹窗 -->
    <a-modal :title="title" :visible="visible" @ok="handleOk" @cancel="handleCancel" width="500px">
      <a-form-model ref="form" :model="model" :rules="validatorRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="文件夹名称" prop="folderName">
          <a-input v-model="model.folderName" placeholder="请输入文件夹名称（英文/数字/横线）" />
        </a-form-model-item>
        <a-form-model-item label="文件夹描述" prop="description">
          <a-textarea v-model="model.description" placeholder="请输入文件夹描述" :rows="3" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-card>
</template>

<script>
import JeecgListMixin from '@/mixins/JeecgListMixin'
import { minioFolderAdd, minioFolderEdit, minioFolderDelete } from '@/api/api'

export default {
  name: 'MinioFolderList',
  mixins: [JeecgListMixin],
  data() {
    return {
      description: '文件夹管理',
      // 表头
      columns: [
        { title: '#', dataIndex: '', key: 'rowIndex', width: 60, align: 'center', customRender: (t, r, index) => (this.ipagination.current - 1) * this.ipagination.pageSize + index + 1 },
        { title: '文件夹名称', dataIndex: 'folderName', width: 250 },
        { title: '描述', dataIndex: 'description', ellipsis: true },
        { title: '创建人', dataIndex: 'createBy', width: 120 },
        { title: '创建时间', dataIndex: 'createTime', width: 170 },
        { title: '操作', dataIndex: 'action', width: 200, align: 'center', scopedSlots: { customRender: 'action' } },
      ],
      url: {
        list: '/minio/folder/list',
        delete: '/minio/folder/delete',
      },
      // 弹窗
      visible: false,
      title: '',
      model: {},
      validatorRules: {
        folderName: [{ required: true, message: '请输入文件夹名称', trigger: 'blur' }],
      },
    }
  },
  methods: {
    /** 跳转到文件管理页 */
    goToFileList(record) {
      this.$router.push({ path: '/miniofile/file/' + record.id })
    },
    /** 新建文件夹 */
    handleAdd() {
      this.model = {}
      this.title = '新建文件夹'
      this.visible = true
    },
    /** 编辑文件夹 */
    handleEdit(record) {
      this.model = { ...record }
      this.title = '编辑文件夹'
      this.visible = true
    },
    /** 弹窗确认 */
    handleOk() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const api = this.model.id ? minioFolderEdit : minioFolderAdd
        api(this.model).then(res => {
          if (res.success) {
            this.$message.success(res.message)
            this.visible = false
            this.loadData()
          } else {
            this.$message.error(res.message)
          }
        })
      })
    },
    /** 弹窗取消 */
    handleCancel() {
      this.visible = false
    },
    /** 删除（覆盖 mixin 默认行为，使用自定义 API） */
    handleDelete(id) {
      minioFolderDelete({ id }).then(res => {
        if (res.success) {
          this.$message.success(res.message)
          this.loadData()
        } else {
          this.$message.error(res.message)
        }
      })
    },
  },
}
</script>
```

- [ ] **Step 2: 验证页面可访问**

启动前端 dev server，访问 `/miniofile/folder`，确认文件夹列表页正常渲染。

- [ ] **Step 3: Commit**

```bash
git add ant-design-vue-jeecg/src/views/miniofile/MinioFolderList.vue
git commit -m "feat(minio-frontend): add MinioFolderList page with CRUD operations"
```

---

## Task 9: MinioFileList.vue 文件管理页

**Files:**
- Create: `ant-design-vue-jeecg/src/views/miniofile/MinioFileList.vue`

这是最复杂的前端组件，包含以下功能模块：
1. 顶部标签页栏（7 大类 + 上传按钮）
2. 左侧标签筛选面板
3. 右侧卡片式文件列表
4. 上传弹窗（单文件 / 压缩包）
5. 编辑弹窗

由于组件较大，分 3 个 Step 逐步构建：先搭建布局骨架和标签页，再完成筛选和卡片列表，最后添加上传/编辑弹窗。

- [ ] **Step 1: 创建 MinioFileList.vue — 布局骨架 + 标签页 + 数据加载**

```vue
<template>
  <a-card :bordered="false" class="minio-file-page">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <a-button type="link" icon="arrow-left" @click="goBack" class="back-btn">返回文件夹列表</a>
      <span class="folder-name">{{ folderName }}</span>
    </div>

    <!-- 标签页栏 -->
    <div class="tab-bar">
      <a-tabs :activeKey="activeDocType" @change="onTabChange" size="small">
        <!-- 6 个常规类型 -->
        <a-tab-pane v-for="dt in DOC_TYPES" :key="dt.docTypeKey" :tab="getTabLabel(dt)" />
        <!-- 知识产权（含子标签） -->
        <a-tab-pane key="知识产权" tab="知识产权">
          <div slot="tab" style="display:flex;align-items:center;gap:4px">
            <span>知识产权</span>
            <a-badge :count="ipTotal" :number-style="{ backgroundColor: '#999' }" v-if="ipTotal > 0" />
          </div>
        </a-tab-pane>
      </a-tabs>
      <!-- 知识产权子标签 -->
      <div class="sub-tabs" v-if="activeDocType === '知识产权'">
        <a-radio-group v-model="activeIpSubType" buttonStyle="solid" size="small" @change="onIpSubTypeChange">
          <a-radio-button v-for="ip in IP_SUB_TYPES" :key="ip.docTypeKey" :value="ip.docTypeKey">
            {{ ip.label }}
          </a-radio-button>
        </a-radio-group>
      </div>
      <!-- 上传按钮 -->
      <div class="upload-buttons">
        <a-button type="primary" icon="upload" size="small" @click="openUploadModal(false)">上传文件</a-button>
        <a-button icon="folder-add" size="small" style="margin-left:8px" @click="openUploadModal(true)">压缩包上传</a-button>
      </div>
    </div>

    <!-- 主体区域：左侧筛选 + 右侧列表 -->
    <div class="main-content">
      <!-- 左侧标签筛选面板 -->
      <div class="filter-panel" v-if="currentTagFields.length > 0">
        <div class="filter-group" v-for="tf in currentTagFields" :key="tf.field">
          <div class="filter-title">{{ tf.label }}</div>
          <div class="filter-items">
            <a-tag
              :color="activeFilters[tf.field] === item ? 'blue' : ''"
              v-for="item in (tagStatsMap[tf.field] || [])"
              :key="item.tagValue"
              style="cursor:pointer;margin-bottom:4px"
              @click="toggleFilter(tf.field, item.tagValue)"
            >
              {{ item.tagValue }} ({{ item.count }})
            </a-tag>
          </div>
        </div>
      </div>

      <!-- 右侧文件列表 -->
      <div class="file-list-area">
        <!-- 活跃筛选标签 -->
        <div class="active-filters" v-if="hasActiveFilters">
          <span>筛选：</span>
          <a-tag v-for="(val, key) in activeFilters" :key="key" color="blue" closable @close="clearFilter(key)">
            {{ getFilterLabel(key) }}: {{ val }}
          </a-tag>
          <a style="margin-left:8px" @click="clearAllFilters">清除全部</a>
        </div>

        <!-- 搜索栏 -->
        <div class="search-bar">
          <a-input-search placeholder="搜索文件名" v-model="searchText" style="width:300px" @search="loadFiles" allowClear />
        </div>

        <!-- 卡片列表 -->
        <a-spin :spinning="loadingFiles">
          <div class="file-cards" v-if="fileList.length > 0">
            <div class="file-card" v-for="file in fileList" :key="file.id">
              <div class="card-header">
                <a-icon :type="getFileIcon(file.fileName)" class="file-icon" />
                <span class="doc-type-tag">{{ file.docType }}</span>
                <span class="level-tag">{{ file.classificationLevel }}</span>
                <span class="year-tag" v-if="file.year">{{ file.year }}年度</span>
              </div>
              <div class="card-title">{{ file.fileName }}</div>
              <div class="card-meta">
                <span v-if="file.author">{{ file.author }}</span>
                <span v-if="file.techSystem">{{ file.techSystem }}</span>
                <span v-if="file.docSource">{{ file.docSource }}</span>
                <span v-if="file.createTime">{{ file.createTime.substring(0, 10) }}</span>
              </div>
              <div class="card-summary" v-if="file.summary">{{ file.summary }}</div>
              <!-- 标签 -->
              <div class="card-tags">
                <a-tag v-for="tag in getFileTags(file)" :key="tag" size="small">{{ tag }}</a-tag>
              </div>
              <!-- 操作 -->
              <div class="card-actions">
                <a @click="downloadFile(file.id)" title="下载"><a-icon type="download" /></a>
                <a-divider type="vertical" />
                <a @click="openEditModal(file)" title="编辑"><a-icon type="edit" /></a>
                <a-divider type="vertical" />
                <a-popconfirm title="确定删除？" @confirm="deleteFile(file.id)">
                  <a style="color:#ff4d4f" title="删除"><a-icon type="delete" /></a>
                </a-popconfirm>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无文件" />
        </a-spin>

        <!-- 分页 -->
        <div class="pagination-area" v-if="totalFiles > 0">
          <a-pagination
            v-model="current"
            :total="totalFiles"
            :pageSize="pageSize"
            showLessItems
            @change="onPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 上传弹窗 -->
    <a-modal :title="isZipUpload ? '压缩包上传' : '上传文件'" :visible="uploadVisible" @cancel="uploadVisible = false" :footer="null" width="650px" :maskClosable="false">
      <a-form-model ref="uploadForm" :model="uploadForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item label="文档类型" required>
          <a-input v-model="uploadForm.docType" disabled />
        </a-form-model-item>
        <a-form-model-item label="选择文件" required>
          <a-upload
            :fileList="uploadFileList"
            :beforeUpload="beforeUpload"
            :remove="() => { uploadFileList = [] }"
            :accept="isZipUpload ? '.zip' : ''"
          >
            <a-button icon="upload">{{ isZipUpload ? '选择ZIP文件' : '选择文件' }}</a-button>
          </a-upload>
        </a-form-model-item>
        <a-form-model-item label="文档名称">
          <a-input v-model="uploadForm.fileName" placeholder="留空则使用文件原名" />
        </a-form-model-item>
        <a-form-model-item label="作者">
          <a-input v-model="uploadForm.author" />
        </a-form-model-item>
        <a-form-model-item label="技术体系">
          <a-input v-model="uploadForm.techSystem" />
        </a-form-model-item>
        <a-form-model-item label="文档来源">
          <a-input v-model="uploadForm.docSource" />
        </a-form-model-item>
        <a-form-model-item label="年度">
          <a-input v-model="uploadForm.year" placeholder="如 2024" />
        </a-form-model-item>
        <a-form-model-item label="摘要">
          <a-textarea v-model="uploadForm.summary" :rows="2" />
        </a-form-model-item>
        <!-- 动态标签字段 -->
        <a-form-model-item v-for="tf in currentTagFields" :key="tf.field" :label="tf.label">
          <a-select v-if="tf.type === 'select'" v-model="uploadForm[tf.field]" placeholder="请选择" allowClear>
            <a-select-option v-for="opt in tf.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
          </a-select>
          <a-input v-else v-model="uploadForm[tf.field]" :placeholder="'请输入' + tf.label" />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ offset: 5, span: 17 }">
          <a-button type="primary" :loading="uploading" @click="doUpload">{{ uploading ? '上传中...' : '确认上传' }}</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <!-- 编辑弹窗 -->
    <a-modal title="编辑文件" :visible="editVisible" @ok="doEdit" @cancel="editVisible = false" width="650px">
      <a-form-model ref="editForm" :model="editForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item label="文档名称">
          <a-input v-model="editForm.fileName" />
        </a-form-model-item>
        <a-form-model-item label="文档类型">
          <a-input v-model="editForm.docType" disabled />
        </a-form-model-item>
        <a-form-model-item label="密级">
          <a-select v-model="editForm.classificationLevel">
            <a-select-option value="公开">公开</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="作者">
          <a-input v-model="editForm.author" />
        </a-form-model-item>
        <a-form-model-item label="技术体系">
          <a-input v-model="editForm.techSystem" />
        </a-form-model-item>
        <a-form-model-item label="文档来源">
          <a-input v-model="editForm.docSource" />
        </a-form-model-item>
        <a-form-model-item label="年度">
          <a-input v-model="editForm.year" />
        </a-form-model-item>
        <a-form-model-item label="摘要">
          <a-textarea v-model="editForm.summary" :rows="2" />
        </a-form-model-item>
        <!-- 动态标签字段 -->
        <a-form-model-item v-for="tf in currentTagFields" :key="tf.field" :label="tf.label">
          <a-select v-if="tf.type === 'select'" v-model="editForm[tf.field]" allowClear>
            <a-select-option v-for="opt in tf.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
          </a-select>
          <a-input v-else v-model="editForm[tf.field]" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-card>
</template>

<script>
import {
  minioFolderQueryById, minioFileList, minioFileEdit,
  minioFileDelete, minioFileDownload, minioFileCountByType,
  minioFileTagStats, minioFileUpload, minioFileUploadZip,
} from '@/api/api'
import { DOC_TYPES, IP_SUB_TYPES, getTagFieldsByDocType, formatFileSize, getFileIcon } from './DocTypeTagConfig'

export default {
  name: 'MinioFileList',
  data() {
    return {
      folderId: this.$route.params.folderId,
      folderName: '',
      // 标签页
      activeDocType: '科技报告',
      activeIpSubType: '知识产权-专利标签',
      typeCountMap: {}, // { docType: count }
      ipTotal: 0,
      // 筛选
      activeFilters: {},
      tagStatsMap: {}, // { field: [{ tagValue, count }] }
      // 文件列表
      fileList: [],
      loadingFiles: false,
      searchText: '',
      current: 1,
      pageSize: 12,
      totalFiles: 0,
      // 上传弹窗
      uploadVisible: false,
      isZipUpload: false,
      uploadFileList: [],
      uploadForm: {},
      uploading: false,
      // 编辑弹窗
      editVisible: false,
      editForm: {},
      // 常量
      DOC_TYPES,
      IP_SUB_TYPES,
      getFileIcon,
    }
  },
  computed: {
    /** 当前文档类型对应的标签字段列表 */
    currentTagFields() {
      if (this.activeDocType === '知识产权') {
        const found = IP_SUB_TYPES.find(ip => ip.docTypeKey === this.activeIpSubType)
        return found ? found.tagFields : []
      }
      const found = DOC_TYPES.find(dt => dt.docTypeKey === this.activeDocType)
      return found ? found.tagFields : []
    },
    /** 当前实际的 docType（知识产权时用子类型） */
    currentDocTypeForQuery() {
      return this.activeDocType === '知识产权' ? this.activeIpSubType : this.activeDocType
    },
    /** 是否有活跃筛选 */
    hasActiveFilters() {
      return Object.keys(this.activeFilters).length > 0
    },
  },
  created() {
    this.loadFolderName()
    this.loadTypeCounts()
    this.loadTagStats()
    this.loadFiles()
  },
  watch: {
    '$route.params.folderId'(val) {
      if (val) {
        this.folderId = val
        this.loadFolderName()
        this.loadTypeCounts()
        this.activeDocType = '科技报告'
        this.activeFilters = {}
        this.loadTagStats()
        this.loadFiles()
      }
    },
  },
  methods: {
    // ========== 导航 ==========
    goBack() {
      this.$router.push('/miniofile/folder')
    },

    // ========== 数据加载 ==========
    loadFolderName() {
      minioFolderQueryById({ id: this.folderId }).then(res => {
        if (res.success && res.result) {
          this.folderName = res.result.folderName
        }
      })
    },
    loadTypeCounts() {
      minioFileCountByType({ folderId: this.folderId }).then(res => {
        if (res.success && res.result) {
          this.typeCountMap = {}
          this.ipTotal = 0
          res.result.forEach(item => {
            this.typeCountMap[item.docType] = item.count
            if (item.docType && item.docType.startsWith('知识产权')) {
              this.ipTotal += item.count
            }
          })
        }
      })
    },
    loadTagStats() {
      this.tagStatsMap = {}
      if (this.currentTagFields.length === 0) return
      // 并行加载所有标签字段的统计
      const promises = this.currentTagFields.map(tf => {
        return minioFileTagStats({
          folderId: this.folderId,
          docType: this.currentDocTypeForQuery,
          tagName: tf.field,
        }).then(res => {
          if (res.success && res.result) {
            this.$set(this.tagStatsMap, tf.field, res.result)
          }
        })
      })
      Promise.all(promises)
    },
    loadFiles() {
      this.loadingFiles = true
      const params = {
        folderId: this.folderId,
        docType: this.currentDocTypeForQuery,
        pageNo: this.current,
        pageSize: this.pageSize,
        fileName: this.searchText || undefined,
      }
      // 添加标签筛选参数
      const keys = Object.keys(this.activeFilters)
      if (keys.length > 0) {
        params.tagField = keys[0]
        params.tagValue = this.activeFilters[keys[0]]
      }
      minioFileList(params).then(res => {
        if (res.success && res.result) {
          this.fileList = res.result.records || []
          this.totalFiles = res.result.total || 0
        }
      }).finally(() => {
        this.loadingFiles = false
      })
    },

    // ========== 标签页切换 ==========
    getTabLabel(dt) {
      const count = this.typeCountMap[dt.docTypeKey] || 0
      return (
        <span style="display:flex;align-items:center;gap:4px">
          <span>{dt.label}</span>
          <a-badge count={count} number-style={{ backgroundColor: '#999' }} v-if={count > 0} />
        </span>
      )
    },
    onTabChange(key) {
      this.activeDocType = key
      this.activeFilters = {}
      this.current = 1
      if (key === '知识产权') {
        this.activeIpSubType = IP_SUB_TYPES[0].docTypeKey
      }
      this.loadTagStats()
      this.loadFiles()
    },
    onIpSubTypeChange() {
      this.activeFilters = {}
      this.current = 1
      this.loadTagStats()
      this.loadFiles()
    },

    // ========== 标签筛选 ==========
    toggleFilter(field, value) {
      if (this.activeFilters[field] === value) {
        this.$delete(this.activeFilters, field)
      } else {
        this.$set(this.activeFilters, field, value)
      }
      this.current = 1
      this.loadFiles()
    },
    clearFilter(key) {
      this.$delete(this.activeFilters, key)
      this.current = 1
      this.loadFiles()
    },
    clearAllFilters() {
      this.activeFilters = {}
      this.current = 1
      this.loadFiles()
    },
    getFilterLabel(field) {
      const all = [...DOC_TYPES, ...IP_SUB_TYPES]
      for (const dt of all) {
        const found = dt.tagFields.find(tf => tf.field === field)
        if (found) return found.label
      }
      return field
    },

    // ========== 分页 ==========
    onPageChange(page) {
      this.current = page
      this.loadFiles()
    },

    // ========== 文件操作 ==========
    downloadFile(id) {
      window.open(minioFileDownload(id), '_blank')
    },
    deleteFile(id) {
      minioFileDelete({ id }).then(res => {
        if (res.success) {
          this.$message.success('删除成功')
          this.loadFiles()
          this.loadTypeCounts()
          this.loadTagStats()
        } else {
          this.$message.error(res.message)
        }
      })
    },
    /** 获取文件的非空标签值 */
    getFileTags(file) {
      const tags = []
      this.currentTagFields.forEach(tf => {
        if (file[tf.field]) tags.push(file[tf.field])
      })
      return tags
    },

    // ========== 上传弹窗 ==========
    openUploadModal(isZip) {
      this.isZipUpload = isZip
      this.uploadFileList = []
      this.uploadForm = { docType: this.currentDocTypeForQuery }
      this.currentTagFields.forEach(tf => { this.uploadForm[tf.field] = undefined })
      this.uploadVisible = true
    },
    beforeUpload(file) {
      this.uploadFileList = [file]
      return false // 阻止自动上传
    },
    doUpload() {
      if (this.uploadFileList.length === 0) {
        this.$message.warning('请选择文件')
        return
      }
      this.uploading = true
      const formData = new FormData()
      formData.append('file', this.uploadFileList[0])
      formData.append('folderId', this.folderId)
      // 附加所有表单字段
      Object.keys(this.uploadForm).forEach(key => {
        if (this.uploadForm[key] !== undefined && this.uploadForm[key] !== null && this.uploadForm[key] !== '') {
          formData.append(key, this.uploadForm[key])
        }
      })
      const api = this.isZipUpload ? minioFileUploadZip : minioFileUpload
      api(formData).then(res => {
        if (res.success) {
          this.$message.success(res.message)
          this.uploadVisible = false
          this.loadFiles()
          this.loadTypeCounts()
          this.loadTagStats()
        } else {
          this.$message.error(res.message)
        }
      }).catch(() => {
        this.$message.error('上传失败')
      }).finally(() => {
        this.uploading = false
      })
    },

    // ========== 编辑弹窗 ==========
    openEditModal(file) {
      this.editForm = { ...file }
      this.editVisible = true
    },
    doEdit() {
      minioFileEdit(this.editForm).then(res => {
        if (res.success) {
          this.$message.success('编辑成功')
          this.editVisible = false
          this.loadFiles()
          this.loadTagStats()
        } else {
          this.$message.error(res.message)
        }
      })
    },
  },
}
</script>

<style scoped>
.minio-file-page { min-height: 100%; }
.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.page-header .back-btn { padding: 0; font-size: 14px; }
.page-header .folder-name { font-size: 16px; font-weight: 500; margin-left: 8px; }
.tab-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}
.tab-bar .ant-tabs { flex: 1; min-width: 0; }
.sub-tabs { margin-bottom: 8px; }
.upload-buttons { flex-shrink: 0; }
.main-content {
  display: flex;
  gap: 16px;
}
.filter-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fafafa;
  border-radius: 4px;
  padding: 12px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
}
.filter-title { font-weight: 500; margin-bottom: 6px; font-size: 13px; color: #666; }
.filter-items { margin-bottom: 12px; }
.file-list-area { flex: 1; min-width: 0; }
.active-filters { margin-bottom: 12px; }
.search-bar { margin-bottom: 12px; }
.file-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}
.file-card {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 12px;
  transition: box-shadow 0.2s;
}
.file-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
.card-header { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.file-icon { font-size: 20px; color: #1890ff; }
.doc-type-tag { background: #e6f7ff; color: #1890ff; padding: 0 6px; border-radius: 3px; font-size: 12px; }
.level-tag { background: #f6ffed; color: #52c41a; padding: 0 6px; border-radius: 3px; font-size: 12px; }
.year-tag { background: #fff7e6; color: #fa8c16; padding: 0 6px; border-radius: 3px; font-size: 12px; }
.card-title { font-weight: 500; font-size: 14px; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { font-size: 12px; color: #999; display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.card-summary { font-size: 12px; color: #666; margin-bottom: 6px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-tags { margin-bottom: 6px; }
.card-actions { text-align: right; }
.pagination-area { text-align: center; margin-top: 16px; }
</style>
```

- [ ] **Step 2: 验证页面功能**

1. 启动前端 dev server
2. 从文件夹列表页点击某个文件夹 → 进入文件管理页
3. 验证标签页切换正常，角标数字显示正确
4. 点击知识产权标签 → 子标签切换正常
5. 左侧标签筛选面板显示对应类型的标签
6. 右侧文件卡片列表正常渲染
7. 上传弹窗根据当前 doc_type 显示对应标签字段
8. 编辑弹窗正确加载文件属性
9. 下载/删除功能正常

- [ ] **Step 3: Commit**

```bash
git add ant-design-vue-jeecg/src/views/miniofile/MinioFileList.vue
git commit -m "feat(minio-frontend): add MinioFileList page with tabs, filtering, cards and modals"
```
