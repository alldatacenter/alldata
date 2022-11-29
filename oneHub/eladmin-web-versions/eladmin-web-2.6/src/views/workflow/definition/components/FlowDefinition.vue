<template>
  <el-dialog title="流程定义" width="50%" :visible.sync="dialogVisible">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="模板名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入模板名称" />
      </el-form-item>
      <el-form-item label="模板文件">
        <el-upload
          ref="upload"
          class="upload-demo"
          action=""
          :limit="1"
          :on-change="handleChange"
          :on-remove="handleRemove"
          :auto-upload="false"
          accept="text/xml"
        >
          <el-button slot="trigger" size="small" type="primary">选取文件</el-button>
          <div slot="tip" class="el-upload__tip">只能上传.bpmn20.xml结尾的文件</div>
        </el-upload>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { deployDefinition } from '@/api/workflow/definition'

export default {
  name: 'FlowDefinition',
  props: {
    visible: {
      type: Boolean,
      default: function() {
        return false
      }
    },
    category: {
      type: String,
      default: function() {
        return ''
      }
    }
  },
  data() {
    return {
      form: {},
      rules: {
        name: [
          { required: true, message: '模板名称不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  methods: {
    handleRemove(file) {
      this.form.file = undefined
    },
    handleChange(file) {
      this.form.file = file.raw
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const formData = new FormData()
          formData.append('category', this.category)
          formData.append('name', this.form.name)
          formData.append('file', this.form.file)
          deployDefinition(formData).then(response => {
            if (response.success) {
              this.$message.success('部署成功')
              this.dialogVisible = false
              this.$emit('handleFlowDefinitionFinished')
            }
          }).catch(error => {
            this.$message.error(error.msg || '部署失败')
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
