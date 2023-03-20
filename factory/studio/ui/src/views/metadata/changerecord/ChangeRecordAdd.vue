<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['metadata:changerecord:add']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="源数据表主键" prop="objectId">
          <el-input v-model="form.objectId" placeholder="请输入源数据表主键" />
        </el-form-item>
        <el-form-item label="源数据表的字段名" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="请输入源数据表的字段名" />
        </el-form-item>
        <el-form-item label="原来的值" prop="fieldOldValue">
          <el-input v-model="form.fieldOldValue" placeholder="请输入原来的值" />
        </el-form-item>
        <el-form-item label="最新的值" prop="fieldNewValue">
          <el-input v-model="form.fieldNewValue" placeholder="请输入最新的值" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="请输入版本号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{ dict.itemValue }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
    </div>
  </el-card>
</template>

<script>
import { addChangeRecord } from '@/api/metadata/changerecord'

export default {
  name: 'ChangeRecordAdd',
  props: {
    data: {
      type: Object,
      default: function() {
        return {}
      }
    }
  },
  data() {
    return {
      title: '变更记录新增',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 保存按钮
      loadingOptions: {
        loading: false,
        loadingText: '保存',
        isDisabled: false
      },
      // 表单参数
      form: {
        status: '1'
      },
      // 表单校验
      rules: {
        objectId: [
          { required: true, message: '源数据表主键不能为空', trigger: 'blur' }
        ],
        fieldName: [
          { required: true, message: '数据表的字段名不能为空', trigger: 'blur' }
        ],
        fieldOldValue: [
          { required: true, message: '原来的值不能为空', trigger: 'blur' }
        ],
        fieldNewValue: [
          { required: true, message: '最新的值不能为空', trigger: 'blur' }
        ],
        version: [
          { required: true, message: '版本号不能为空', trigger: 'blur' }
        ]
      },
      // 状态数据字典
      statusOptions: []
    }
  },
  created() {
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          addChangeRecord(this.form).then(response => {
            if (response.success) {
              this.$message.success('保存成功')
              setTimeout(() => {
                // 2秒后跳转列表页
                this.$emit('showCard', this.showOptions)
              }, 2000)
            } else {
              this.$message.error('保存失败')
              this.loadingOptions.loading = false
              this.loadingOptions.loadingText = '保存'
              this.loadingOptions.isDisabled = false
            }
          }).catch(() => {
            this.loadingOptions.loading = false
            this.loadingOptions.loadingText = '保存'
            this.loadingOptions.isDisabled = false
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 230px);
  overflow-y: auto;
}
</style>
