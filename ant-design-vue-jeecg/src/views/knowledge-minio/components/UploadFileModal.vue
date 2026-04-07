<template>
  <a-modal
    :visible="visible"
    title="上传文件"
    :confirmLoading="uploading"
    @ok="handleSubmit"
    @cancel="handleCancel"
    centered
    width="560px"
    :destroyOnClose="true"
  >
    <a-form layout="vertical" class="upload-form">
      <a-form-item label="存储桶" required>
        <a-select
          v-model="formState.bucketName"
          placeholder="请选择存储桶"
          :loading="bucketsLoading"
          not-found-content="暂无存储桶，请先同步"
        >
          <a-select-option v-for="item in bucketOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="标签">
        <a-select
          v-model="formState.tags"
          mode="tags"
          placeholder="选择或输入标签，回车添加"
          :tokenSeparators="[',']"
          allow-clear
        >
          <a-select-option v-for="item in tagOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="备注">
        <a-textarea
          v-model="formState.remark"
          placeholder="请输入备注信息（可选）"
          :rows="2"
        />
      </a-form-item>

      <a-form-item label="选择文件" required>
        <a-upload-dragger
          :fileList="fileList"
          :beforeUpload="beforeUpload"
          name="file"
        >
          <p class="ant-upload-drag-icon">
            <a-icon type="inbox" :style="{ fontSize: '40px', color: '#1890ff' }" />
          </p>
          <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p class="ant-upload-hint">支持单个文件上传</p>
        </a-upload-dragger>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>
import { getBucketList, getAllTags } from '../minio.api';
import { ACCESS_TOKEN } from '@/store/mutation-types';

export default {
  name: 'UploadFileModal',
  data() {
    return {
      visible: false,
      uploading: false,
      fileList: [],
      bucketsLoading: false,
      bucketOptions: [],
      tagOptions: [],
      formState: {
        bucketName: '',
        tags: [],
        remark: '',
      },
      defaultBucket: '',
    };
  },
  methods: {
    open(bucket) {
      this.defaultBucket = bucket || '';
      this.visible = true;
      this.loadBuckets();
      this.loadTags();
    },

    async loadBuckets() {
      this.bucketsLoading = true;
      try {
        const res = await getBucketList();
        this.bucketOptions = (res.result || []).map((b) => ({
          label: b.bucketName,
          value: b.bucketName,
        }));
        if (this.defaultBucket) {
          this.formState.bucketName = this.defaultBucket;
        } else if (this.bucketOptions.length > 0) {
          this.formState.bucketName = this.bucketOptions[0].value;
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

    beforeUpload(file) {
      this.fileList = [file];
      return false;
    },

    async handleSubmit() {
      if (!this.formState.bucketName) {
        this.$message.warning('请选择存储桶');
        return;
      }
      if (this.fileList.length === 0) {
        this.$message.warning('请选择文件');
        return;
      }

      this.uploading = true;
      try {
        const formData = new FormData();
        formData.append('file', this.fileList[0]);
        formData.append('bucketName', this.formState.bucketName);
        formData.append('tags', this.formState.tags.join(','));
        formData.append('remark', this.formState.remark || '');

        const baseUrl = (window._CONFIG && window._CONFIG['domianURL']) || '';
        const token = this.$ls.get(ACCESS_TOKEN) || '';
        const response = await fetch(`${baseUrl}/minio/file/upload`, {
          method: 'POST',
          headers: { 'X-Access-Token': token },
          body: formData,
        });

        const result = await response.json();
        if (result.success) {
          this.$message.success('上传成功');
          this.visible = false;
          this.$emit('success');
        } else {
          this.$message.error(result.message || '上传失败');
        }
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'));
      } finally {
        this.uploading = false;
      }
    },

    handleCancel() {
      this.visible = false;
      this.resetForm();
    },

    resetForm() {
      this.formState.bucketName = '';
      this.formState.tags = [];
      this.formState.remark = '';
      this.fileList = [];
    },
  },
};
</script>

<style lang="less" scoped>
.upload-form {
  padding: 8px 4px;
}
</style>
