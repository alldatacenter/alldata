<template>
  <el-dialog title="审核人选择" width="50%" :visible.sync="dialogVisible">
    <el-transfer
      v-model="value"
      :data="data"
      :props="{
        key: 'id',
        label: 'username'
      }"
      @left-check-change="leftCheckChange($event)"
      @change="change($event)"
    >
      <span slot-scope="{ option }">{{ option.username }}({{ option.nickname }})</span>
    </el-transfer>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submit">确认</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { getAuditUsers } from '@/api/system/user'
import { executeTask } from '@/api/workflow/task'

export default {
  name: 'HandleUser',
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
    },
    action: {
      type: String,
      default: function() {
        return ''
      }
    }
  },
  data() {
    return {
      data: [],
      value: []
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
    this.getList()
  },
  methods: {
    getList() {
      getAuditUsers().then(response => {
        if (response.success) {
          this.data = response.data
          console.log(this.data)
        }
      })
    },
    leftCheckChange(e) {
      if (e.length === 0) {
        this.data.forEach((item, index) => {
          delete item['disabled']
        })
      }
      if (e.length === 1) {
        this.data.forEach((item, index) => {
          if (e[0] !== item.id) {
            item['disabled'] = true
          }
        })
      }
    },
    change(e) {
      if (this.value.length === 0) {
        this.data.forEach((item, index) => {
          delete item['disabled']
        })
      }
    },
    submit() {
      if (this.value.length === 0) {
        this.$message.warning('请先选择审核人员')
        return false
      }
      this.$confirm('确认选择该审核人员, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const data = {
          action: this.action,
          processInstanceId: this.task.processInstanceId,
          taskId: this.task.id,
          userId: this.value[0],
          message: ''
        }
        executeTask(data).then(response => {
          if (response.success) {
            this.$message.success('审核人员设置成功')
            this.dialogVisible = false
            this.$emit('handleTaskUserFinished')
          }
        })
      }).catch(() => {
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-transfer ::v-deep .el-transfer-panel__header {
  display: none;
}
</style>
