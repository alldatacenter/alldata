<template>
  <el-dialog title="流程分类" width="50%" :visible.sync="dialogVisible">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入分类名称" />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addCategory, updateCategory } from '@/api/workflow/definition'

export default {
  name: 'FlowCategory',
  props: {
    visible: {
      type: Boolean,
      default: function() {
        return false
      }
    },
    data: {
      type: Object,
      default: function() {
        return {}
      }
    }
  },
  data() {
    return {
      form: {},
      rules: {
        name: [
          { required: true, message: '分类名称不能为空', trigger: 'blur' }
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
  created() {
    console.log(this.data)
    this.form = Object.assign({}, this.data)
  },
  methods: {
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id) {
            updateCategory(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleFlowCategoryFinished')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          } else {
            addCategory(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleFlowCategoryFinished')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
