<template>
  <div class="cnki-page">
    <!-- 顶部导航 -->
    <div class="cnki-topbar">
      <div class="topbar-left">
        <a-breadcrumb>
          <a-breadcrumb-item><a @click="goBack">文件库</a></a-breadcrumb-item>
          <a-breadcrumb-item>{{ folderName }}</a-breadcrumb-item>
        </a-breadcrumb>
      </div>
      <div class="topbar-right">
        <a-button type="primary" icon="upload" @click="openUploadModal(false)">上传文件</a-button>
        <a-button icon="folder-add" style="margin-left:8px" @click="openUploadModal(true)">压缩包上传</a-button>
      </div>
    </div>

    <!-- 文档分类 Tab + 搜索 + 排序 -->
    <div class="cnki-toolbar">
      <div class="toolbar-row">
        <a-tabs :activeKey="activeDocType" @change="onTabChange" class="doc-type-tabs">
          <a-tab-pane v-for="dt in DOC_TYPES" :key="dt.docTypeKey">
            <span slot="tab">{{ dt.label }}<b class="tab-count-inline">{{ typeCountMap[dt.docTypeKey] || 0 }}</b></span>
          </a-tab-pane>
          <a-tab-pane key="知识产权">
            <span slot="tab">知识产权<b class="tab-count-inline">{{ ipTotal }}</b></span>
          </a-tab-pane>
        </a-tabs>
      </div>
      <!-- 知识产权子分类 -->
      <div class="ip-subtype-bar" v-if="activeDocType === '知识产权'">
        <a-radio-group v-model="activeIpSubType" buttonStyle="solid" size="small" @change="() => onIpSubTypeChange(activeIpSubType)">
          <a-radio-button v-for="ip in IP_SUB_TYPES" :key="ip.docTypeKey" :value="ip.docTypeKey">{{ ip.label }}</a-radio-button>
        </a-radio-group>
      </div>
      <!-- 搜索 + 排序 -->
      <div class="search-sort-row">
        <a-input-search
          placeholder="输入文件名、作者、摘要等关键词检索"
          v-model="searchText"
          @search="loadFiles"
          allowClear
          style="width:360px"
        />
        <div class="sort-group">
          <span class="sort-label">排序：</span>
          <a-radio-group v-model="sortBy" buttonStyle="solid" size="small" @change="loadFiles">
            <a-radio-button value="time_desc">最新上传</a-radio-button>
            <a-radio-button value="time_asc">最早上传</a-radio-button>
            <a-radio-button value="name_asc">名称</a-radio-button>
            <a-radio-button value="size_desc">大小</a-radio-button>
          </a-radio-group>
        </div>
      </div>
    </div>

    <!-- 主体：左侧标签筛选 + 右侧列表 -->
    <div class="cnki-body">
      <!-- 左侧筛选 -->
      <div class="cnki-sidebar">
        <!-- 技术体系树形筛选 -->
        <div class="sidebar-section" style="margin-bottom:12px" v-if="techSystemTree.length > 0">
          <div class="sidebar-title">技术体系</div>
          <div class="tech-tree-wrap">
            <div
              v-for="node in techSystemTree" :key="node.id"
              class="tech-tree-node"
            >
              <div class="tech-tree-parent" @click="toggleTechExpand(node.id)">
                <a-icon :type="expandedTechNodes[node.id] ? 'caret-down' : 'caret-right'" class="tech-expand-icon" v-if="node.children && node.children.length > 0" />
                <span class="tech-expand-placeholder" v-else></span>
                <span style="font-weight:500">{{ node.name }}</span>
              </div>
              <div v-if="expandedTechNodes[node.id] && node.children" class="tech-tree-children">
                <div
                  v-for="child in node.children" :key="child.id"
                  class="tech-tree-child"
                  :class="{ active: activeTechSystem.includes(child.id) }"
                  @click="toggleTechFilter(child.id)"
                >
                  <a-icon :type="activeTechSystem.includes(child.id) ? 'check-square' : 'border'" class="tech-check-icon" :style="{ color: activeTechSystem.includes(child.id) ? '#1890ff' : '#bbb' }" />
                  {{ child.name }}
                  <span class="tech-count">{{ getTechSystemCount(child.id) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 原有标签筛选 -->
        <div class="sidebar-section" v-if="currentTagFields.length > 0">
          <div class="sidebar-title">分类筛选</div>
          <div v-for="tf in currentTagFields" :key="tf.field" class="filter-group">
            <div class="filter-group-label">{{ tf.label }}</div>
            <div class="filter-tags">
              <a-tag
                v-for="item in (tagStatsMap[tf.field] || [])" :key="item.tagValue"
                :color="activeFilters[tf.field] === item.tagValue ? 'blue' : ''"
                class="filter-tag-item"
                @click="toggleFilter(tf.field, item.tagValue)"
              >
                {{ item.tagValue }}
                <span class="filter-tag-count">{{ item.count }}</span>
              </a-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧文献列表 -->
      <div class="cnki-main">
        <!-- 活跃筛选条件 -->
        <div class="active-filters" v-if="Object.keys(activeFilters).length > 0 || activeTechSystem.length > 0">
          <span class="filters-label">当前筛选：</span>
          <a-tag v-for="tsId in activeTechSystem" :key="'ts_'+tsId" color="blue" closable @close="removeTechFilter(tsId)">
            技术体系：{{ techSystemNameMap[tsId] || tsId }}
          </a-tag>
          <a-tag v-for="(val, key) in activeFilters" :key="key" color="blue" closable @close="clearFilter(key)">
            {{ getFilterLabel(key) }}：{{ val }}
          </a-tag>
          <a class="clear-all" @click="clearAllFilters">清除全部</a>
        </div>

        <!-- 统计信息 -->
        <div class="result-info" v-if="!loadingFiles">
          共找到 <b>{{ totalFiles }}</b> 条结果
          <span v-if="searchText">，关键词 "<em>{{ searchText }}</em>"</span>
        </div>

        <!-- 文献列表 -->
        <a-spin :spinning="loadingFiles">
          <div class="cnki-list" v-if="fileList.length > 0">
            <div v-for="(file, index) in fileList" :key="file.id" class="cnki-item">
              <!-- 序号 -->
              <div class="item-index">
                <span class="index-num">{{ (current - 1) * pageSize + index + 1 }}</span>
              </div>

              <!-- 主要内容区 -->
              <div class="item-body">
                <!-- 标题行 -->
                <div class="item-title-row">
                  <a-icon :type="getFileIcon(file.fileName)" class="item-file-icon" />
                  <span class="item-title" :title="file.fileName" @click="downloadFile(file.id, file.fileName)">{{ file.fileName }}</span>
                  <span v-if="file.classificationLevel" class="item-level-tag">{{ file.classificationLevel }}</span>
                </div>

                <!-- 作者/来源/日期 -->
                <div class="item-source-row">
                  <template v-if="file.author">
                    <span class="source-author">{{ file.author }}</span>
                  </template>
                  <template v-if="file.docSource">
                    <span class="source-sep">|</span>
                    <span class="source-journal">{{ file.docSource }}</span>
                  </template>
                  <template v-if="file.techSystem">
                    <span class="source-sep">|</span>
                    <span class="source-tech">{{ techSystemNameMap[file.techSystem] || file.techSystem }}</span>
                  </template>
                  <template v-if="file.year">
                    <span class="source-sep">|</span>
                    <span class="source-year">{{ file.year }}年</span>
                  </template>
                  <template v-if="file.createTime">
                    <span class="source-sep">|</span>
                    <span class="source-date">{{ file.createTime.substring(0, 10) }}</span>
                  </template>
                </div>

                <!-- 摘要 -->
                <div class="item-abstract" v-if="file.summary" @click="toggleAbstract(file)">
                  <span class="abstract-label">摘要：</span>
                  <span :class="['abstract-text', { collapsed: !file._expanded }]">{{ file.summary }}</span>
                  <span class="abstract-toggle">{{ file._expanded ? '收起' : '展开' }}</span>
                </div>

                <!-- 标签/关键词 -->
                <div class="item-keywords" v-if="getFileTags(file).length > 0">
                  <span class="keywords-label">关键词：</span>
                  <a-tag
                    v-for="tag in getFileTags(file)" :key="tag"
                    class="keyword-tag"
                    @click="searchByKeyword(tag)"
                  >{{ tag }}</a-tag>
                </div>

                <!-- 文档类型标签 -->
                <div class="item-tags-row">
                  <span class="doc-type-tag">{{ file.docType }}</span>
                </div>
              </div>

              <!-- 右侧操作区 -->
              <div class="item-actions">
                <div class="action-btn" @click="downloadFile(file.id, file.fileName)">
                  <a-icon type="download" />
                  <span>下载</span>
                </div>
                <div class="action-btn" @click="openEditModal(file)">
                  <a-icon type="edit" />
                  <span>编辑</span>
                </div>
                <a-popconfirm title="确定删除此文件？" @confirm="deleteFile(file.id)">
                  <div class="action-btn action-btn-danger">
                    <a-icon type="delete" />
                    <span>删除</span>
                  </div>
                </a-popconfirm>
              </div>
            </div>
          </div>

          <div class="cnki-empty" v-else-if="!loadingFiles">
            <a-empty description="暂无符合条件的文献">
              <a-button type="primary" @click="openUploadModal(false)" style="margin-top:8px">上传文件</a-button>
            </a-empty>
          </div>
        </a-spin>

        <!-- 分页 -->
        <div class="cnki-pagination" v-if="totalFiles > 0">
          <a-pagination
            v-model="current"
            :total="totalFiles"
            :pageSize="pageSize"
            :showQuickJumper="true"
            showSizeChanger
            :pageSizeOptions="['10','20','50']"
            @change="onPageChange"
            @showSizeChange="onSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 弹窗组件 -->
    <file-upload-modal
      ref="uploadModal"
      :folderId="folderId"
      :docType="currentDocTypeForQuery"
      @success="onUploadSuccess"
    />
    <file-edit-modal
      ref="editModal"
      :tagFields="currentTagFields"
      @success="onEditSuccess"
    />
  </div>
</template>

<script>
import { getAction, deleteAction } from '@/api/manage'
import { DOC_TYPES, IP_SUB_TYPES, getFileIcon } from './DocTypeTagConfig'
import { ACCESS_TOKEN } from '@/store/mutation-types'
import Vue from 'vue'
import FileUploadModal from './FileUploadModal'
import FileEditModal from './FileEditModal'

export default {
  name: 'MinioFileList',
  components: { FileUploadModal, FileEditModal },
  data() {
    return {
      folderId: this.$route.params.folderId,
      folderName: '',
      DOC_TYPES,
      IP_SUB_TYPES,
      activeDocType: '科技报告',
      activeIpSubType: '知识产权-专利标签',
      typeCountMap: {},
      ipTotal: 0,
      activeFilters: {},
      tagStatsMap: {},
      techSystemTree: [],
      expandedTechNodes: {},
      activeTechSystem: [],
      techSystemStatsMap: {},
      techSystemNameMap: {},
      fileList: [],
      loadingFiles: false,
      searchText: '',
      sortBy: 'time_desc',
      current: 1,
      pageSize: 10,
      totalFiles: 0,
    }
  },
  computed: {
    currentTagFields() {
      if (this.activeDocType === '知识产权') {
        const f = IP_SUB_TYPES.find(ip => ip.docTypeKey === this.activeIpSubType)
        return f ? f.tagFields : []
      }
      const f = DOC_TYPES.find(dt => dt.docTypeKey === this.activeDocType)
      return f ? f.tagFields : []
    },
    currentDocTypeForQuery() {
      return this.activeDocType === '知识产权' ? this.activeIpSubType : this.activeDocType
    },
  },
  created() {
    this.loadFolderName()
    this.loadTypeCounts()
    this.loadTagStats()
    this.loadTechSystemTree()
    this.loadTechSystemStats()
    this.loadFiles()
  },
  methods: {
    getFileIcon,
    goBack() { this.$router.push('/miniofile/folder') },
    getFileTags(file) {
      let tags = []
      this.currentTagFields.forEach(tf => { if (file[tf.field]) tags.push(file[tf.field]) })
      return tags
    },
    toggleAbstract(file) {
      this.$set(file, '_expanded', !file._expanded)
    },
    searchByKeyword(tag) {
      this.searchText = tag
      this.current = 1
      this.loadFiles()
    },
    loadFolderName() {
      getAction('/minio/folder/queryById', { id: this.folderId }).then(res => {
        if (res.success && res.result) this.folderName = res.result.folderName
      })
    },
    loadTypeCounts() {
      getAction('/minio/file/countByType', { folderId: this.folderId }).then(res => {
        if (res.success && res.result) {
          this.typeCountMap = {}
          this.ipTotal = 0
          res.result.forEach(item => {
            this.$set(this.typeCountMap, item.docType, item.count)
            if (item.docType && item.docType.startsWith('知识产权')) this.ipTotal += item.count
          })
        }
      })
    },
    loadTagStats() {
      this.tagStatsMap = {}
      this.currentTagFields.forEach(tf => {
        getAction('/minio/file/tagStats', { folderId: this.folderId, docType: this.currentDocTypeForQuery, tagName: tf.field }).then(res => {
          if (res.success && res.result) this.$set(this.tagStatsMap, tf.field, res.result)
        })
      })
    },
    loadFiles() {
      this.loadingFiles = true
      let params = {
        folderId: this.folderId,
        docType: this.currentDocTypeForQuery,
        pageNo: this.current,
        pageSize: this.pageSize,
        fileName: this.searchText || undefined,
        sortBy: this.sortBy,
      }
      let keys = Object.keys(this.activeFilters)
      if (keys.length > 0) { params.tagField = keys[0]; params.tagValue = this.activeFilters[keys[0]] }
      if (this.activeTechSystem.length > 0) { params.techSystem = this.activeTechSystem.join(',') }
      getAction('/minio/file/list', params).then(res => {
        if (res.success && res.result) {
          this.fileList = (res.result.records || []).map(f => ({ ...f, _expanded: false }))
          this.totalFiles = res.result.total || 0
        }
      }).finally(() => { this.loadingFiles = false })
    },
    onTabChange(key) {
      this.activeDocType = key
      this.activeFilters = {}
      this.activeTechSystem = []
      this.current = 1
      if (key === '知识产权') this.activeIpSubType = IP_SUB_TYPES[0].docTypeKey
      this.loadTagStats()
      this.loadTechSystemStats()
      this.loadFiles()
    },
    onIpSubTypeChange(key) {
      this.activeIpSubType = key
      this.activeFilters = {}
      this.activeTechSystem = []
      this.current = 1
      this.loadTagStats()
      this.loadTechSystemStats()
      this.loadFiles()
    },
    toggleFilter(field, value) {
      if (this.activeFilters[field] === value) this.$delete(this.activeFilters, field)
      else this.$set(this.activeFilters, field, value)
      this.current = 1
      this.loadFiles()
    },
    clearFilter(key) { this.$delete(this.activeFilters, key); this.current = 1; this.loadFiles() },
    getFilterLabel(field) {
      let all = [...DOC_TYPES, ...IP_SUB_TYPES]
      for (let dt of all) { let f = dt.tagFields.find(tf => tf.field === field); if (f) return f.label }
      return field
    },
    onPageChange(page) { this.current = page; this.loadFiles() },
    onSizeChange(_current, size) { this.current = 1; this.pageSize = size; this.loadFiles() },
    downloadFile(id, fileName) {
      let baseUrl = window._CONFIG && window._CONFIG['domianURL'] ? window._CONFIG['domianURL'] : ''
      let token = Vue.ls.get(ACCESS_TOKEN) || ''
      fetch(baseUrl + '/minio/file/download/' + id, { headers: { 'X-Access-Token': token } })
        .then(res => {
          if (!res.ok) return res.json().then(data => { throw new Error(data.message || '下载失败') })
          return res.blob()
        })
        .then(blob => {
          let a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = fileName || 'download'
          document.body.appendChild(a)
          a.click()
          document.body.removeChild(a)
          URL.revokeObjectURL(a.href)
        })
        .catch(err => { this.$message.error(err.message) })
    },
    deleteFile(id) {
      deleteAction('/minio/file/delete', { id }).then(res => {
        if (res.success) { this.$message.success('删除成功'); this.loadFiles(); this.loadTypeCounts(); this.loadTagStats(); this.loadTechSystemStats() }
        else this.$message.error(res.message)
      })
    },
    openUploadModal(isZip) {
      this.$refs.uploadModal.show(isZip)
    },
    onUploadSuccess() {
      this.loadFiles(); this.loadTypeCounts(); this.loadTagStats(); this.loadTechSystemStats()
    },
    openEditModal(file) {
      this.$refs.editModal.show(file)
    },
    onEditSuccess() {
      this.loadFiles(); this.loadTagStats(); this.loadTechSystemStats()
    },
    loadTechSystemTree() {
      getAction('/sys/category/loadTreeRoot', { pcode: 'C01', async: false }).then(res => {
        if (res.success && res.result) {
          this.techSystemTree = this.buildTechTree(res.result)
        }
      })
    },
    buildTechTree(nodes) {
      if (!nodes) return []
      return nodes.map(n => {
        let id = n.key || n.id
        let name = n.title || n.name
        this.techSystemNameMap[id] = name
        return {
          id,
          name,
          children: n.children ? this.buildTechTree(n.children) : [],
        }
      })
    },
    removeTechFilter(id) {
      let idx = this.activeTechSystem.indexOf(id)
      if (idx >= 0) this.activeTechSystem.splice(idx, 1)
      this.current = 1
      this.loadFiles()
    },
    toggleTechExpand(id) {
      this.$set(this.expandedTechNodes, id, !this.expandedTechNodes[id])
    },
    toggleTechFilter(id) {
      let idx = this.activeTechSystem.indexOf(id)
      if (idx >= 0) this.activeTechSystem.splice(idx, 1)
      else this.activeTechSystem.push(id)
      this.current = 1
      this.loadFiles()
    },
    getTechSystemCount(id) {
      let stat = this.techSystemStatsMap[id]
      return stat ? stat : ''
    },
    loadTechSystemStats() {
      this.techSystemStatsMap = {}
      getAction('/minio/file/list', {
        folderId: this.folderId,
        docType: this.currentDocTypeForQuery,
        pageNo: 1,
        pageSize: 10000,
      }).then(res => {
        if (res.success && res.result && res.result.records) {
          let map = {}
          res.result.records.forEach(f => {
            if (f.techSystem) {
              f.techSystem.split(',').filter(s => s).forEach(id => {
                map[id] = (map[id] || 0) + 1
              })
            }
          })
          this.techSystemStatsMap = map
        }
      })
    },
    clearAllFilters() {
      this.activeFilters = {}
      this.activeTechSystem = []
      this.current = 1
      this.loadFiles()
    },
  },
}
</script>

<style scoped>
/* ========== 整体布局 ========== */
.cnki-page {
  padding: 0;
  background: #f0f2f5;
  min-height: 100vh;
}

/* ========== 顶部导航 ========== */
.cnki-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}
.topbar-left {
  font-size: 14px;
}
.topbar-right {
  display: flex;
  align-items: center;
}

/* ========== 工具栏（Tab + 搜索 + 排序） ========== */
.cnki-toolbar {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 24px;
}
.toolbar-row {
  border-bottom: 1px solid #f0f0f0;
}
.doc-type-tabs >>> .ant-tabs-bar {
  margin-bottom: 0;
  border-bottom: none;
}
.doc-type-tabs >>> .ant-tabs-nav .ant-tabs-tab {
  font-size: 14px;
  padding: 10px 4px;
}
.tab-count-inline {
  font-weight: normal;
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}
/* 知识产权子分类 */
.ip-subtype-bar {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
/* 搜索 + 排序行 */
.search-sort-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}
.sort-group {
  display: flex;
  align-items: center;
  gap: 6px;
}
.sort-label {
  font-size: 13px;
  color: #666;
}

/* ========== 主体区域 ========== */
.cnki-body {
  display: flex;
  gap: 0;
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
}

/* ========== 左侧筛选栏 ========== */
.cnki-sidebar {
  width: 200px;
  flex-shrink: 0;
  margin-right: 16px;
}
.sidebar-section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 2px;
}
.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
  padding: 10px 14px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}
.filter-group {
  padding: 10px 14px;
  border-bottom: 1px solid #f5f5f5;
}
.filter-group:last-child {
  border-bottom: none;
}
.filter-group-label {
  font-size: 12px;
  font-weight: 500;
  color: #666;
  margin-bottom: 6px;
}
.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.filter-tag-item {
  font-size: 12px;
  cursor: pointer;
  border-radius: 2px;
}
.filter-tag-count {
  font-size: 10px;
  color: #999;
  margin-left: 2px;
}

/* ========== 技术体系树 ========== */
.tech-tree-wrap {
  padding: 6px 0;
}
.tech-tree-node {
}
.tech-tree-parent {
  display: flex;
  align-items: center;
  padding: 5px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #333;
  transition: background 0.15s;
}
.tech-tree-parent:hover {
  background: #e6f7ff;
}
.tech-tree-parent.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}
.tech-expand-icon {
  font-size: 10px;
  color: #999;
  margin-right: 4px;
  width: 14px;
}
.tech-expand-placeholder {
  display: inline-block;
  width: 14px;
}
.tech-check-icon {
  font-size: 13px;
  margin-right: 4px;
}
.tech-tree-children {
  padding-left: 28px;
}
.tech-tree-child {
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  color: #555;
  transition: background 0.15s;
}
.tech-tree-child:hover {
  background: #e6f7ff;
}
.tech-tree-child.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}
.tech-count {
  font-size: 10px;
  color: #999;
  margin-left: 4px;
}

/* ========== 右侧主内容 ========== */
.cnki-main {
  flex: 1;
  min-width: 0;
}

/* 活跃筛选 */
.active-filters {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 2px;
  padding: 8px 14px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.filters-label {
  font-size: 12px;
  color: #666;
}
.clear-all {
  font-size: 12px;
  color: #999;
  cursor: pointer;
}
.clear-all:hover {
  color: #1890ff;
}

/* 统计信息 */
.result-info {
  font-size: 13px;
  color: #666;
  padding: 8px 4px;
  margin-bottom: 8px;
}
.result-info b {
  color: #1890ff;
}
.result-info em {
  color: #1890ff;
  font-style: normal;
}

/* ========== 文献列表 ========== */
.cnki-list {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 2px;
  overflow: hidden;
}
.cnki-item {
  display: flex;
  padding: 16px 18px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
  position: relative;
}
.cnki-item:last-child {
  border-bottom: none;
}
.cnki-item:hover {
  background: #e6f7ff;
}

/* 序号 */
.item-index {
  width: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}
.index-num {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  border-radius: 2px;
}
.cnki-item:hover .index-num {
  background: #1890ff;
  color: #fff;
}

/* 内容区 */
.item-body {
  flex: 1;
  min-width: 0;
  padding: 0 14px;
}

/* 标题行 */
.item-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.item-file-icon {
  font-size: 18px;
  color: #1890ff;
  flex-shrink: 0;
}
.item-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  cursor: pointer;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s;
}
.item-title:hover {
  color: #1890ff;
  text-decoration: underline;
}
.item-level-tag {
  flex-shrink: 0;
  font-size: 11px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  border-radius: 2px;
  background: #fff7e6;
  color: #d48806;
  border: 1px solid #ffe58f;
}

/* 来源行 */
.item-source-row {
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
  line-height: 1.6;
}
.source-author {
  color: #1890ff;
  cursor: pointer;
}
.source-author:hover {
  text-decoration: underline;
}
.source-sep {
  margin: 0 4px;
  color: #d9d9d9;
}

/* 摘要 */
.item-abstract {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
  margin-bottom: 4px;
  cursor: pointer;
}
.abstract-label {
  color: #999;
  font-weight: 500;
}
.abstract-text {
  display: inline;
}
.abstract-text.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.abstract-toggle {
  color: #1890ff;
  font-size: 12px;
  margin-left: 4px;
}
.abstract-toggle:hover {
  text-decoration: underline;
}

/* 关键词 */
.item-keywords {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.keywords-label {
  font-size: 12px;
  color: #999;
  font-weight: 500;
}
.keyword-tag {
  font-size: 12px;
  cursor: pointer;
  border-radius: 2px;
  border: none;
  background: #e6f7ff;
  color: #1890ff;
}
.keyword-tag:hover {
  background: #bae7ff;
}

/* 标签行 */
.item-tags-row {
  margin-top: 4px;
}
.doc-type-tag {
  display: inline-block;
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
  border-radius: 2px;
  background: #e6f7ff;
  color: #096dd9;
  border: 1px solid #91d5ff;
}

/* 操作区 */
.item-actions {
  width: 110px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding-top: 2px;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #1890ff;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 2px;
  transition: all 0.15s;
}
.action-btn:hover {
  background: #e6f7ff;
}
.action-btn-danger {
  color: #ff4d4f;
}
.action-btn-danger:hover {
  background: #fff1f0;
  color: #cf1322;
}

/* 空状态 */
.cnki-empty {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 2px;
  padding: 60px 20px;
}

/* 分页 */
.cnki-pagination {
  text-align: center;
  padding: 16px 0 24px;
}
</style>
