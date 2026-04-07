<template>
  <a-card :bordered="false">
    <!-- 顶部栏 -->
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px">
      <div>
        <a-input-search placeholder="搜索文件夹" v-model="queryParam.displayName" style="width:280px" @search="searchQuery" allowClear />
      </div>
      <a-button type="primary" icon="plus" @click="handleAdd">新建文件夹</a-button>
    </div>

    <!-- 文件夹卡片网格 -->
    <a-spin :spinning="loading">
      <div v-if="dataSource.length > 0" class="folder-grid">
        <div v-for="folder in dataSource" :key="folder.id" class="folder-card" @click="goToFileList(folder)">
          <div class="folder-icon">
            <a-icon type="folder" theme="filled" style="font-size:42px;color:#faad14" />
          </div>
          <div class="folder-info">
            <div class="folder-name">{{ folder.displayName || folder.folderName }}</div>
            <div class="folder-bucket" v-if="folder.displayName && folder.displayName !== folder.folderName">{{ folder.folderName }}</div>
            <div class="folder-desc" v-if="folder.description">{{ folder.description }}</div>
            <div class="folder-meta">
              <span v-if="folder.createBy">{{ folder.createBy }}</span>
              <span v-if="folder.createTime">{{ folder.createTime.substring(0,10) }}</span>
            </div>
          </div>
          <div class="folder-actions" @click.stop>
            <a-tooltip title="编辑"><a-icon type="edit" @click="handleEdit(folder)" /></a-tooltip>
            <a-popconfirm title="确定删除此文件夹？" @confirm="handleDelete(folder.id)">
              <a-tooltip title="删除"><a-icon type="delete" style="color:#ff4d4f" /></a-tooltip>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无文件夹，点击右上角新建" />
    </a-spin>

    <!-- 分页 -->
    <div v-if="ipagination.total > 0" style="text-align:center;margin-top:20px">
      <a-pagination v-model="ipagination.current" :total="ipagination.total" :pageSize="ipagination.pageSize" showTotal="{total} => 共 ${total} 个文件夹" showSizeChanger @change="onPageChange" @showSizeChange="onSizeChange" />
    </div>

    <!-- 弹窗 -->
    <a-modal :title="modalTitle" :visible="visible" @ok="handleOk" @cancel="visible=false" width="500px">
      <a-form-model ref="form" :model="model" :rules="rules" :label-col="{span:5}" :wrapper-col="{span:16}">
        <a-form-model-item label="文件夹名称" prop="folderName">
          <a-input v-model="model.folderName" placeholder="英文/数字/短横线（创建后不可改）" :disabled="!!model.id" />
        </a-form-model-item>
        <a-form-model-item label="显示名称" prop="displayName">
          <a-input v-model="model.displayName" placeholder="文件夹显示名（支持中文）" />
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="model.description" placeholder="请输入描述" :rows="3" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-card>
</template>

<script>
import { getAction, postAction, putAction, deleteAction } from '@/api/manage'

export default {
  name: 'MinioFolderList',
  data() {
    const validateFolderName = (_rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入文件夹名称'))
        return
      }
      if (!/^[a-z0-9][a-z0-9\-]{1,61}[a-z0-9]$/.test(value)) {
        callback(new Error('3-63位小写字母、数字、短横线，不能以短横线开头或结尾'))
        return
      }
      callback()
    }
    return {
      dataSource: [],
      loading: false,
      ipagination: { current: 1, pageSize: 12, total: 0 },
      queryParam: {},
      visible: false,
      modalTitle: '',
      model: {},
      rules: {
        folderName: [{ required: true, validator: validateFolderName, trigger: 'blur' }],
        displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
      },
    }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      let params = Object.assign({}, this.queryParam, { pageNo: this.ipagination.current, pageSize: this.ipagination.pageSize })
      getAction('/minio/folder/list', params).then(res => {
        if (res.success) { this.dataSource = res.result.records || []; this.ipagination.total = res.result.total || 0 }
      }).finally(() => { this.loading = false })
    },
    onPageChange(page) { this.ipagination.current = page; this.loadData() },
    onSizeChange(_current, size) { this.ipagination.current = 1; this.ipagination.pageSize = size; this.loadData() },
    searchQuery() { this.ipagination.current = 1; this.loadData() },
    searchReset() { this.queryParam = {}; this.ipagination.current = 1; this.loadData() },
    goToFileList(record) { this.$router.push({ path: '/miniofile/file/' + record.id }) },
    handleAdd() { this.model = {}; this.modalTitle = '新建文件夹'; this.visible = true; this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() }) },
    handleEdit(record) { this.model = Object.assign({}, record); this.modalTitle = '编辑文件夹'; this.visible = true; this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() }) },
    handleOk() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        let api = this.model.id ? putAction : postAction
        api('/minio/folder/' + (this.model.id ? 'edit' : 'add'), this.model).then(res => {
          if (res.success) { this.$message.success(res.message); this.visible = false; this.loadData() }
          else this.$message.error(res.message)
        })
      })
    },
    handleDelete(id) {
      deleteAction('/minio/folder/delete', { id }).then(res => {
        if (res.success) { this.$message.success('删除成功'); this.loadData() }
        else this.$message.error(res.message)
      })
    },
  },
}
</script>

<style scoped>
.folder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.folder-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s;
  background: #fff;
}
.folder-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-color: #faad14;
}
.folder-card:hover .folder-actions {
  opacity: 1;
}
.folder-icon {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fffbe6;
  border-radius: 12px;
}
.folder-info {
  flex: 1;
  min-width: 0;
}
.folder-name {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.folder-card:hover .folder-name {
  color: #1890ff;
}
.folder-bucket {
  font-size: 12px;
  color: #bfbfbf;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.folder-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.folder-meta {
  font-size: 12px;
  color: #bfbfbf;
  display: flex;
  gap: 12px;
}
.folder-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  gap: 8px;
  font-size: 16px;
}
.folder-actions .anticon {
  cursor: pointer;
  color: #8c8c8c;
  transition: color 0.2s;
}
.folder-actions .anticon:hover {
  color: #1890ff;
}
</style>
