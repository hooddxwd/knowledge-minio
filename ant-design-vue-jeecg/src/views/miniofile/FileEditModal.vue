<template>
  <a-modal title="编辑文件" :visible="visible" @ok="doEdit" @cancel="visible = false" width="650px">
    <a-form-model :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
      <a-form-model-item label="文档名称"><a-input v-model="form.fileName" /></a-form-model-item>
      <a-form-model-item label="文档密级" required>
        <a-select v-model="form.classificationLevel" placeholder="请选择密级">
          <a-select-option v-for="level in CLASSIFICATION_LEVELS" :key="level" :value="level">{{ level }}</a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item label="作者"><a-input v-model="form.author" /></a-form-model-item>
      <a-form-model-item label="技术体系"><a-input v-model="form.techSystem" /></a-form-model-item>
      <a-form-model-item label="文档来源"><a-input v-model="form.docSource" /></a-form-model-item>
      <a-form-model-item label="年度"><a-input v-model="form.year" /></a-form-model-item>
      <a-form-model-item label="摘要"><a-textarea v-model="form.summary" :rows="2" /></a-form-model-item>
      <a-form-model-item v-for="tf in tagFields" :key="tf.field" :label="tf.label">
        <a-select v-if="tf.type === 'select'" v-model="form[tf.field]" allowClear>
          <a-select-option v-for="opt in tf.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
        </a-select>
        <a-input v-else v-model="form[tf.field]" />
      </a-form-model-item>
    </a-form-model>
  </a-modal>
</template>

<script>
import { putAction } from '@/api/manage'
import { CLASSIFICATION_LEVELS } from './DocTypeTagConfig'

export default {
  name: 'FileEditModal',
  props: {
    tagFields: { type: Array, default: () => [] },
  },
  data() {
    return {
      CLASSIFICATION_LEVELS,
      visible: false,
      form: {},
    }
  },
  methods: {
    show(file) {
      this.form = Object.assign({}, file)
      this.visible = true
    },
    doEdit() {
      putAction('/minio/file/edit', this.form).then(res => {
        if (res.success) { this.$message.success('编辑成功'); this.visible = false; this.$emit('success') }
        else this.$message.error(res.message)
      })
    },
  },
}
</script>
