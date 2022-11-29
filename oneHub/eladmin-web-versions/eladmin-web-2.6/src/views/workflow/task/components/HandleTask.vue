<template>
  <el-dialog title="任务审核" width="50%" :visible.sync="dialogVisible">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="意审核见" prop="message">
        <el-input
          v-model="form.message"
          :autosize="{ minRows: 2, maxRows: 3}"
          type="textarea"
          placeholder="请输入审核意见"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="doComplete(true)">同意</el-button>
      <el-button type="primary" @click="doComplete(false)">不同意</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { executeTask } from '@/api/workflow/task'

export default {
  name: 'HandleTask',
  props: {
    visible: {
      type: Boolean,
      default: function() {
        return false
      }
    },
    task: {
      type: Object,
      default: function() {
        return {}
      }
    }
  },
  data() {
    return {
      form: { message: '' },
      rules: {
        message: [
          { required: true, message: '审核意见不能为空', trigger: 'blur' }
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
    doComplete(approved) {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const data = {
            action: this.task.isDelegation ? 'resolve' : 'complete',
            processInstanceId: this.task.processInstanceId,
            taskId: this.task.id,
            userId: '',
            message: this.form.message,
            variables: { approved: approved }
          }
          executeTask(data).then(response => {
            if (response.success) {
              this.$message.success('任务审核成功')
              this.dialogVisible = false
              this.$emit('handleTaskFinished')
            }
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
