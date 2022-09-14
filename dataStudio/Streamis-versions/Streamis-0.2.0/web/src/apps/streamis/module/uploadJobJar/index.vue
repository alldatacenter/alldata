<template>
  <div>
    <Modal
      :title="$t('message.streamis.jobListTableColumns.upload')"
      v-model="visible"
      @on-cancel="cancel"
      footer-hide
      width="800"
    >
      <Form
        ref="uploadForm"
        :model="uploadForm"
        :rules="ruleValidate"
        :label-width="80"
      >
        <FormItem
          :label="$t('message.streamis.jobListTableColumns.jobName')"
          prop="jobName"
        >
          <Input v-model="uploadForm.jobName"></Input>
        </FormItem>
        <FormItem
          :label="$t('message.streamis.uploadJar.entrypointClass')"
          prop="entrypointClass"
        >
          <Input v-model="uploadForm.entrypointClass"></Input>
        </FormItem>
        <FormItem :label="$t('message.streamis.uploadJar.label')" prop="label">
          <Input v-model="uploadForm.label"></Input>
        </FormItem>
        <FormItem
          :label="$t('message.streamis.uploadJar.entrypointMainArgs')"
          prop="entrypointMainArgs"
        >
          <Input v-model="uploadForm.entrypointMainArgs"></Input>
        </FormItem>
        <FormItem
          :label="$t('message.streamis.uploadJar.parallelism')"
          prop="parallelism"
        >
          <Input v-model="uploadForm.parallelism"></Input>
        </FormItem>
        <FormItem>
          <Upload :before-upload="handleUpload" action="">
            <Button icon="ios-cloud-upload-outline">{{
              $t('message.streamis.uploadJar.selectJar')
            }}</Button>
          </Upload>
          <div v-if="!!file">
            {{ $t('message.streamis.uploadJar.choosedJar') }}: {{ file.name }}
          </div>
        </FormItem>
        <FormItem>
          <Button type="primary" @click="handleSubmit()" :loading="loading">{{
            $t('message.streamis.uploadJar.upload')
          }}</Button>
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>
<script>
import api from '@/common/service/api'
export default {
  props: {
    visible: Boolean
  },
  data() {
    return {
      uploadForm: {
        jobName: '',
        entrypointClass: '',
        label: '',
        entrypointMainArgs: '',
        parallelism: ''
      },
      file: '',
      ruleValidate: {
        jobName: [
          {
            required: true,
            message: this.$t('message.streamis.uploadJar.jobNameEmpty'),
            trigger: 'blur'
          }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleSubmit() {
      this.$refs['uploadForm'].validate(valid => {
        if (!this.file) {
          this.$Message.error(this.$t('message.streamis.uploadJar.jarEmpty'))
          return
        }
        if (valid) {
          const formData = new FormData()
          formData.append('file', this.file)
          formData.append('workspaceId', 1)
          formData.append('projectId', 1)
          Object.keys(this.uploadForm).forEach(key => {
            const value = this.uploadForm[key]
            if (value) {
              formData.append(key, value)
            }
          })
          this.loading = true
          api
            .fetch('streamis/streamJobManager/config/upload', formData)
            .then(res => {
              this.loading = false
              this.$t('message.streamis.operationSuccess')
              this.cancel();
              this.$emit("jarUploadSuccess");
              console.log(res)
            })
            .catch(e => {
              console.log(e)
              this.loading = false;
            })
        }
      })
    },
    handleUpload(file) {
      const type = file.type
      if (type !== 'application/java-archive') {
        this.$Message.error(this.$t('message.streamis.uploadJar.jarError'))
        this.file = null
      } else {
        this.file = file
      }
      console.log(file)
      return false
    },
    cancel() {
      this.file = ''
      this.$refs['uploadForm'].resetFields()
      this.$emit('jarModalCancel')
    }
  }
}
</script>
<style lang="scss" scoped></style>
