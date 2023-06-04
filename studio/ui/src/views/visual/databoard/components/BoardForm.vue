<template>
  <el-dialog title="看板" width="50%" :visible.sync="dialogVisible" :show-close="false" :close-on-click-modal="false">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="看板名称" prop="boardName">
        <el-input v-model="form.boardName" placeholder="请输入看板名称" />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addDataBoard, updateDataBoard } from '@/api/visual/databoard'

export default {
  name: 'BoardForm',
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
      form: {
        boardName: undefined
      },
      rules: {
        boardName: [
          { required: true, message: '看板名称不能为空', trigger: 'blur' }
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
    this.form = this.data
  },
  methods: {
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id) {
            updateDataBoard(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleBoardFormFinished')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          } else {
            addDataBoard(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleBoardFormFinished')
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
