<template>
  <div class="minio-file-manager">
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <a-icon type="cloud-server" :style="{ fontSize: '20px', color: '#1890ff' }" />
        <span class="page-title">MinIO 文件管理</span>
      </div>
      <div class="header-right">
        <a-select
          v-model="currentBucket"
          placeholder="选择存储桶"
          style="width: 200px"
          :loading="bucketsLoading"
          allow-clear
          @change="handleBucketChange"
        >
          <a-select-option v-for="item in bucketOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
        <a-button size="small" @click="handleSyncBuckets" :loading="syncing">
          <a-icon type="sync" /> 同步Bucket
        </a-button>
        <a-button type="primary" size="small" @click="handleUpload" :disabled="!currentBucket">
          <a-icon type="cloud-upload" /> 上传文件
        </a-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-input
        v-model="searchParams.fileName"
        placeholder="搜索文件名..."
        allow-clear
        style="width: 200px"
        @pressEnter="handleSearch"
      >
        <a-icon slot="prefix" type="search" />
      </a-input>
      <a-select
        v-model="searchParams.tag"
        placeholder="按标签筛选"
        style="width: 180px"
        allow-clear
        @change="handleSearch"
      >
        <a-select-option v-for="item in tagOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </a-select-option>
      </a-select>
      <a-select
        v-model="searchParams.fileType"
        placeholder="文件类型"
        style="width: 120px"
        allow-clear
        @change="handleSearch"
      >
        <a-select-option value="pdf">PDF</a-select-option>
        <a-select-option value="docx">Word</a-select-option>
        <a-select-option value="xlsx">Excel</a-select-option>
        <a-select-option value="jpg">图片</a-select-option>
        <a-select-option value="txt">文本</a-select-option>
        <a-select-option value="zip">ZIP</a-select-option>
      </a-select>
      <a-button type="link" size="small" @click="resetSearch">重置</a-button>
    </div>

    <!-- 文件表格 -->
    <div class="table-wrapper">
      <a-table
        :columns="columns"
        :dataSource="tableData"
        :loading="tableLoading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
        :rowSelection="{ selectedRowKeys, onChange: onSelectChange }"
        size="middle"
      >
        <!-- 文件名 -->
        <span slot="fileName" slot-scope="text, record">
          <span class="file-name-cell">
            <a-icon :type="getFileIcon(record.fileType)" :style="{ fontSize: '16px', color: getFileColor(record.fileType), flexShrink: 0 }" />
            <span class="file-name-text" :title="record.fileName">{{ record.fileName }}</span>
          </span>
        </span>

        <!-- 文件大小 -->
        <span slot="fileSize" slot-scope="text, record">
          {{ formatSize(record.fileSize) }}
        </span>

        <!-- 标签 -->
        <span slot="tags" slot-scope="text, record">
          <template v-if="record.tags">
            <a-tag v-for="tag in record.tags.split(',')" :key="tag" color="blue" style="margin-bottom: 2px">
              {{ tag.trim() }}
            </a-tag>
          </template>
          <span v-else style="color: #ccc">-</span>
        </span>

        <!-- 操作 -->
        <span slot="action" slot-scope="text, record">
          <a-space>
            <a-button type="link" size="small" @click="handleDownload(record)">
              <a-icon type="download" /> 下载
            </a-button>
            <a-button type="link" size="small" @click="handleEditTags(record)">
              <a-icon type="tags" /> 标签
            </a-button>
            <a-popconfirm title="确定删除此文件？" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>
                <a-icon type="delete" /> 删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </span>
      </a-table>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="selectedRowKeys.length > 0" class="batch-bar">
      <span>已选择 {{ selectedRowKeys.length }} 项</span>
      <a-popconfirm title="确定批量删除选中文件？" @confirm="handleBatchDelete">
        <a-button type="primary" danger size="small">批量删除</a-button>
      </a-popconfirm>
      <a-button size="small" @click="selectedRowKeys = []">取消选择</a-button>
    </div>

    <!-- 上传弹窗 -->
    <upload-file-modal ref="uploadModalRef" @success="handleUploadSuccess" />

    <!-- 编辑标签弹窗 -->
    <a-modal
      v-model="editTagVisible"
      title="编辑标签"
      @ok="submitEditTags"
      :confirmLoading="editTagSaving"
      centered
      width="460px"
    >
      <a-form layout="vertical">
        <a-form-item label="文件名">
          <a-input :value="editingFile && editingFile.fileName" disabled />
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model="editTagForm.tags"
            mode="tags"
            placeholder="选择或输入标签"
            :tokenSeparators="[',']"
            allow-clear
          >
            <a-select-option v-for="item in tagOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model="editTagForm.remark" :rows="2" placeholder="备注信息" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { getFileList, deleteFile, deleteBatch, editFile, getAllTags, getBucketList, syncBuckets } from './minio.api';
import UploadFileModal from './components/UploadFileModal.vue';

export default {
  name: 'MinioFileManager',
  components: {
    UploadFileModal,
  },
  data() {
    return {
      // ---- Bucket相关 ----
      currentBucket: undefined,
      bucketOptions: [],
      bucketsLoading: false,
      syncing: false,

      // ---- 搜索 ----
      searchParams: {
        fileName: '',
        tag: undefined,
        fileType: undefined,
      },
      tagOptions: [],

      // ---- 表格 ----
      columns: [
        { title: '文件名', dataIndex: 'fileName', width: 280, ellipsis: true, scopedSlots: { customRender: 'fileName' } },
        { title: '存储桶', dataIndex: 'bucketName', width: 120 },
        { title: '大小', dataIndex: 'fileSize', width: 100, scopedSlots: { customRender: 'fileSize' } },
        { title: '类型', dataIndex: 'fileType', width: 80 },
        { title: '标签', dataIndex: 'tags', width: 220, scopedSlots: { customRender: 'tags' } },
        { title: '上传人', dataIndex: 'createBy', width: 100 },
        { title: '上传时间', dataIndex: 'createTime', width: 170 },
        { title: '操作', dataIndex: 'action', width: 220, fixed: 'right', scopedSlots: { customRender: 'action' } },
      ],
      tableData: [],
      tableLoading: false,
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: (total) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50'],
      },
      selectedRowKeys: [],

      // ---- 弹窗 ----
      editTagVisible: false,
      editTagSaving: false,
      editingFile: null,
      editTagForm: {
        tags: [],
        remark: '',
      },
    };
  },
  mounted() {
    this.loadBuckets();
    this.loadTags();
  },
  methods: {
    // ---- 数据加载 ----
    async loadBuckets() {
      this.bucketsLoading = true;
      try {
        const res = await getBucketList();
        this.bucketOptions = (res.result || []).map((b) => ({
          label: b.bucketName,
          value: b.bucketName,
        }));
        if (this.bucketOptions.length > 0 && !this.currentBucket) {
          this.currentBucket = this.bucketOptions[0].value;
          this.loadTableData();
        }
      } catch {
        this.bucketOptions = [];
      } finally {
        this.bucketsLoading = false;
      }
    },

    async loadTags() {
      try {
        const res = await getAllTags();
        this.tagOptions = (res.result || []).map((t) => ({ label: t, value: t }));
      } catch {
        this.tagOptions = [];
      }
    },

    async loadTableData() {
      this.tableLoading = true;
      try {
        const params = {
          pageNo: this.pagination.current,
          pageSize: this.pagination.pageSize,
          bucketName: this.currentBucket,
        };
        if (this.searchParams.fileName) params.fileName = this.searchParams.fileName;
        if (this.searchParams.tag) params.tag = this.searchParams.tag;
        if (this.searchParams.fileType) params.fileType = this.searchParams.fileType;

        const res = await getFileList(params);
        this.tableData = res.result ? res.result.records || [] : [];
        this.pagination.total = res.result ? res.result.total || 0 : 0;
      } catch {
        this.tableData = [];
      } finally {
        this.tableLoading = false;
      }
    },

    // ---- 事件处理 ----
    handleBucketChange() {
      this.pagination.current = 1;
      this.loadTableData();
    },

    handleSearch() {
      this.pagination.current = 1;
      this.loadTableData();
    },

    resetSearch() {
      this.searchParams.fileName = '';
      this.searchParams.tag = undefined;
      this.searchParams.fileType = undefined;
      this.handleSearch();
    },

    handleTableChange(pag) {
      this.pagination.current = pag.current;
      this.pagination.pageSize = pag.pageSize;
      this.loadTableData();
    },

    async handleSyncBuckets() {
      this.syncing = true;
      try {
        await syncBuckets();
        this.$message.success('Bucket同步成功');
        await this.loadBuckets();
      } catch {
        this.$message.error('同步失败');
      } finally {
        this.syncing = false;
      }
    },

    handleUpload() {
      this.$refs.uploadModalRef.open(this.currentBucket);
    },

    handleUploadSuccess() {
      this.loadTableData();
      this.loadTags();
    },

    handleDownload(record) {
      const baseUrl = (window._CONFIG && window._CONFIG['domianURL']) || '';
      const token = this.$ls.get(this.$store.getters.accessTokenKey || 'Access-Token') || '';
      const url = `${baseUrl}/minio/file/download?id=${record.id}&X-Access-Token=${token}`;
      window.open(url, '_blank');
    },

    async handleDelete(record) {
      try {
        await deleteFile({ id: record.id });
        this.$message.success('删除成功');
        this.selectedRowKeys = this.selectedRowKeys.filter((k) => k !== record.id);
        this.loadTableData();
        this.loadTags();
      } catch {
        this.$message.error('删除失败');
      }
    },

    async handleBatchDelete() {
      if (this.selectedRowKeys.length === 0) return;
      try {
        await deleteBatch({ ids: this.selectedRowKeys.join(',') });
        this.$message.success('批量删除成功');
        this.selectedRowKeys = [];
        this.loadTableData();
        this.loadTags();
      } catch {
        this.$message.error('批量删除失败');
      }
    },

    handleEditTags(record) {
      this.editingFile = record;
      this.editTagForm.tags = record.tags ? record.tags.split(',').map((t) => t.trim()) : [];
      this.editTagForm.remark = record.remark || '';
      this.editTagVisible = true;
    },

    async submitEditTags() {
      this.editTagSaving = true;
      try {
        await editFile({
          id: this.editingFile.id,
          tags: this.editTagForm.tags.join(','),
          remark: this.editTagForm.remark,
        });
        this.$message.success('更新成功');
        this.editTagVisible = false;
        this.loadTableData();
        this.loadTags();
      } catch {
        this.$message.error('更新失败');
      } finally {
        this.editTagSaving = false;
      }
    },

    onSelectChange(keys) {
      this.selectedRowKeys = keys;
    },

    // ---- 工具函数 ----
    getFileIcon(fileType) {
      const iconMap = {
        pdf: 'file-pdf',
        doc: 'file-word',
        docx: 'file-word',
        xls: 'file-excel',
        xlsx: 'file-excel',
        ppt: 'file-ppt',
        pptx: 'file-ppt',
        jpg: 'file-image',
        jpeg: 'file-image',
        png: 'file-image',
        gif: 'file-image',
        mp4: 'video-camera',
        mp3: 'sound',
        zip: 'file-zip',
        rar: 'file-zip',
        txt: 'file-text',
        md: 'file-markdown',
      };
      return iconMap[fileType || ''] || 'file';
    },

    getFileColor(fileType) {
      const colorMap = {
        pdf: '#e74c3c',
        doc: '#2979ff',
        docx: '#2979ff',
        xls: '#27ae60',
        xlsx: '#27ae60',
        ppt: '#e67e22',
        pptx: '#e67e22',
        jpg: '#8e44ad',
        jpeg: '#8e44ad',
        png: '#8e44ad',
        zip: '#7f8c8d',
        mp4: '#e74c3c',
        mp3: '#1abc9c',
        txt: '#95a5a6',
        md: '#3498db',
      };
      return colorMap[fileType || ''] || '#8c8c8c';
    },

    formatSize(bytes) {
      if (!bytes || bytes === 0) return '0 B';
      const units = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(1024));
      return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
    },
  },
};
</script>

<style lang="less" scoped>
.minio-file-manager {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.table-wrapper {
  margin-bottom: 8px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #e8f4ff;
  border-radius: 6px;
  font-size: 13px;
  color: #1890ff;
  margin-top: 8px;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
