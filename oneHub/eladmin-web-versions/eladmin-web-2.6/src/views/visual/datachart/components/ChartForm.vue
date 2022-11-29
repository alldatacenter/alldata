<template>
  <el-dialog title="图表" width="50%" :visible.sync="dialogVisible" :show-close="false" :close-on-click-modal="false">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="图表名称" prop="chartName">
        <el-input v-model="form.chartName" placeholder="请输入图表名称" />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addDataChart, updateDataChart } from '@/api/visual/datachart'

export default {
  name: 'ChartForm',
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
        chartName: undefined
      },
      rules: {
        chartName: [
          { required: true, message: '图表名称不能为空', trigger: 'blur' }
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
            updateDataChart(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleChartFormFinished')
              }
            }).catch(error => {
              this.$message.error(error.msg || '保存失败')
            })
          } else {
            addDataChart(this.form).then(response => {
              if (response.success) {
                this.$message.success('保存成功')
                this.dialogVisible = false
                this.$emit('handleChartFormFinished')
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
