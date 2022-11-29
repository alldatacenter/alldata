<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['metadata:datasource:add']" v-if="active == 2" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-steps :active="active" finish-status="success" align-center>
        <el-step title="数据源信息" />
        <el-step title="连接信息" />
      </el-steps>
      <el-form v-if="active == 1" ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="数据源类型" prop="dbType">
          <el-select v-model="form.dbType">
            <el-option
              v-for="item in dbTypeOptions"
              :key="item.id"
              :label="item.itemValue"
              :value="item.itemText"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="请输入数据源名称" />
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
      <el-form v-if="active == 2" ref="form2" :model="form2" :rules="rules2" label-width="80px">
        <el-form-item label="主机" prop="host">
          <el-input v-model="form2.host" placeholder="请输入主机" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input v-model="form2.port" placeholder="请输入端口" />
        </el-form-item>
        <el-form-item v-if="form.dbType === '3' || form.dbType === '4'" label="服务名" prop="sid">
          <el-input v-model="form2.sid" placeholder="请输入服务名" />
        </el-form-item>
        <el-form-item v-if="form.dbType !== '3' && form.dbType !== '4'" label="数据库" prop="dbName">
          <el-input v-model="form2.dbName" placeholder="请输入数据库" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form2.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form2.password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button v-hasPerm="['metadata:datasource:connect']" size="mini" type="primary" @click="handleCheckConnection">连通性检测</el-button>
        </el-form-item>
      </el-form>
      <el-button v-if="active == 1" style="margin-top: 12px;" @click="handleNextStep">下一步</el-button>
      <el-button v-if="active == 2" style="margin-top: 12px;" @click="handleLastStep">上一步</el-button>
    </div>
  </el-card>
</template>

<script>
import { addDataSource, checkConnection } from '@/api/metadata/datasource'

export default {
  name: 'DataSourceAdd',
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
      title: '数据源新增',
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
      active: 1,
      // 表单参数
      form: {
        id: undefined,
        dbType: undefined,
        themeId: undefined,
        sourceName: undefined,
        dbSchema: {},
        status: '1',
        remark: undefined
      },
      // 表单校验
      rules: {
        dbType: [
          { required: true, message: '数据源类型不能为空', trigger: 'change' }
        ],
        sourceName: [
          { required: true, message: '数据源名称不能为空', trigger: 'blur' }
        ]
      },
      form2: {
        host: undefined,
        port: undefined,
        dbName: undefined,
        username: undefined,
        password: undefined,
        sid: undefined
      },
      rules2: {
        host: [
          { required: true, message: '主机不能为空', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '端口不能为空', trigger: 'blur' }
        ],
        sid: [
          { required: true, message: '服务名不能为空', trigger: 'blur' }
        ],
        dbName: [
          { required: true, message: '数据库不能为空', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '用户名不能为空', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '密码不能为空', trigger: 'blur' }
        ]
      },
      // 状态数据字典
      statusOptions: [],
      // 数据源类型数据字典
      dbTypeOptions: []
    }
  },
  created() {
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDicts('data_db_type').then(response => {
      if (response.success) {
        this.dbTypeOptions = response.data
      }
    })
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 步骤条下一步 */
    handleNextStep() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.active++
        }
      })
    },
    /** 步骤条上一步 */
    handleLastStep() {
      this.active--
    },
    /** 检测数据库连通性 */
    handleCheckConnection() {
      this.$refs['form2'].validate(valid => {
        if (valid) {
          this.form.dbSchema = this.form2
          checkConnection(this.form).then(response => {
            if (response.success) {
              this.$message.success('连接成功')
            }
          })
        }
      })
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form2'].validate(valid => {
        if (valid) {
          this.form.dbSchema = this.form2
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          addDataSource(this.form).then(response => {
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
