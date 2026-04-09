<template>
  <a-modal :title="isZipUpload ? '压缩包上传' : '上传文件'" :visible="visible" @cancel="handleCancel" :footer="null" width="650px" :maskClosable="false">
    <a-form-model :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
      <a-form-model-item label="文档类型">
        <a-select v-model="form.docType" placeholder="请选择文档类型" @change="onDocTypeChange" allowClear>
          <a-select-opt-group label="文档分类">
            <a-select-option v-for="dt in DOC_TYPES" :key="dt.docTypeKey" :value="dt.docTypeKey">{{ dt.label }}</a-select-option>
          </a-select-opt-group>
          <a-select-opt-group label="知识产权">
            <a-select-option v-for="ip in IP_SUB_TYPES" :key="ip.docTypeKey" :value="ip.docTypeKey">{{ ip.label }}</a-select-option>
          </a-select-opt-group>
        </a-select>
      </a-form-model-item>
      <a-form-model-item label="文档密级" required>
        <JDictSelectTag v-model="form.classificationLevel" dictCode="classification_level" placeholder="请选择" allowClear />
      </a-form-model-item>
      <a-form-model-item label="选择文件">
        <a-upload :fileList="fileList" :beforeUpload="beforeUpload" :remove="() => { fileList = [] }" :accept="isZipUpload ? '.zip' : ''">
          <a-button icon="upload">{{ isZipUpload ? '选择ZIP文件' : '选择文件' }}</a-button>
        </a-upload>
      </a-form-model-item>
      <a-form-model-item label="作者"><a-input v-model="form.author" /></a-form-model-item>
      <a-form-model-item label="技术体系"><a-input v-model="form.techSystem" /></a-form-model-item>
      <a-form-model-item label="文档来源"><a-input v-model="form.docSource" /></a-form-model-item>
      <a-form-model-item label="年度"><a-input v-model="form.year" placeholder="如 2024" /></a-form-model-item>
      <a-form-model-item label="摘要"><a-textarea v-model="form.summary" :rows="2" /></a-form-model-item>
      <a-form-model-item v-for="tf in localTagFields" :key="tf.field" :label="tf.label">
        <JDictSelectTag v-if="tf.type === 'select'" v-model="form[tf.field]" :dictCode="tf.dictCode" placeholder="请选择" allowClear />
        <a-input v-else v-model="form[tf.field]" :placeholder="'请输入' + tf.label" />
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ offset: 5, span: 17 }">
        <a-button type="primary" :loading="uploading" @click="doUpload">{{ uploading ? '上传中...' : '确认上传' }}</a-button>
      </a-form-model-item>
    </a-form-model>
  </a-modal>
</template>

<script>
import { ACCESS_TOKEN } from '@/store/mutation-types'
import Vue from 'vue'
import { DOC_TYPES, IP_SUB_TYPES } from './DocTypeTagConfig'
import JDictSelectTag from '@/components/dict/JDictSelectTag'

export default {
  name: 'FileUploadModal',
  components: { JDictSelectTag },
  props: {
    folderId: { type: String, required: true },
    docType: { type: String, default: '' },
  },
  data() {
    return {
      DOC_TYPES,
      IP_SUB_TYPES,
      visible: false,
      isZipUpload: false,
      fileList: [],
      form: {},
      uploading: false,
      localTagFields: [],
    }
  },
  methods: {
    show(isZip) {
      this.isZipUpload = !!isZip
      this.fileList = []
      this.form = { docType: this.docType, classificationLevel: '公开' }
      this.localTagFields = this.getTagFieldsByDocType(this.form.docType)
      this.localTagFields.forEach(tf => { this.$set(this.form, tf.field, undefined) })
      this.visible = true
    },
    getTagFieldsByDocType(docType) {
      if (!docType) return []
      if (docType.startsWith('知识产权')) {
        let found = IP_SUB_TYPES.find(ip => ip.docTypeKey === docType)
        return found ? found.tagFields : []
      }
      let found = DOC_TYPES.find(dt => dt.docTypeKey === docType)
      return found ? found.tagFields : []
    },
    onDocTypeChange() {
      // 切换文档类型时，重置标签字段
      this.localTagFields = this.getTagFieldsByDocType(this.form.docType)
      this.localTagFields.forEach(tf => { this.$set(this.form, tf.field, undefined) })
    },
    handleCancel() { this.visible = false },
    beforeUpload(file) { this.fileList = [file]; return false },
    doUpload() {
      if (this.fileList.length === 0) { this.$message.warning('请选择文件'); return }
      if (!this.form.classificationLevel) { this.$message.warning('请选择文档密级'); return }
      this.uploading = true
      let formData = new FormData()
      formData.append('file', this.fileList[0])
      formData.append('folderId', this.folderId)
      Object.keys(this.form).forEach(key => {
        if (this.form[key] !== undefined && this.form[key] !== null && this.form[key] !== '') formData.append(key, this.form[key])
      })
      let baseUrl = window._CONFIG && window._CONFIG['domianURL'] ? window._CONFIG['domianURL'] : ''
      let token = Vue.ls.get(ACCESS_TOKEN) || ''
      let url = baseUrl + (this.isZipUpload ? '/minio/file/uploadZip' : '/minio/file/upload')
      fetch(url, { method: 'POST', headers: { 'X-Access-Token': token }, body: formData })
        .then(res => res.json())
        .then(data => {
          if (data.success) { this.$message.success(data.message); this.visible = false; this.$emit('success') }
          else this.$message.error(data.message)
        }).catch(() => { this.$message.error('上传失败') })
        .finally(() => { this.uploading = false })
    },
  },
}
</script>
