<template>
  <a-modal title="编辑文件" :visible="visible" @ok="doEdit" @cancel="visible = false" width="650px">
    <a-form-model :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
      <a-form-model-item label="文档名称"><a-input v-model="form.fileName" /></a-form-model-item>
      <a-form-model-item label="文档密级" required>
        <JDictSelectTag v-model="form.classificationLevel" dictCode="classification_level" placeholder="请选择" allowClear />
      </a-form-model-item>
      <a-form-model-item label="作者"><a-input v-model="form.author" /></a-form-model-item>
      <a-form-model-item label="技术体系"><a-input v-model="form.techSystem" /></a-form-model-item>
      <a-form-model-item label="文档来源"><a-input v-model="form.docSource" /></a-form-model-item>
      <a-form-model-item label="年度"><a-input v-model="form.year" /></a-form-model-item>
      <a-form-model-item label="摘要"><a-textarea v-model="form.summary" :rows="2" /></a-form-model-item>
      <a-form-model-item v-for="tf in tagFields" :key="tf.field" :label="tf.label">
        <JDictSelectTag v-if="tf.type === 'select'" v-model="form[tf.field]" :dictCode="tf.dictCode" placeholder="请选择" allowClear />
        <a-input v-else v-model="form[tf.field]" />
      </a-form-model-item>
    </a-form-model>
  </a-modal>
</template>

<script>
import { putAction } from '@/api/manage'
import JDictSelectTag from '@/components/dict/JDictSelectTag'

export default {
  name: 'FileEditModal',
  components: { JDictSelectTag },
  props: {
    tagFields: { type: Array, default: () => [] },
  },
  data() {
    return {
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
