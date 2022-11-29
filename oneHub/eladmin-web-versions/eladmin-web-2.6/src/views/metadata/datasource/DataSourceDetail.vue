<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <!-- zrx add-->
        <el-button v-hasPerm="['metadata:datasource:sync']" v-if="form.isSync === '0'" size="mini" icon="el-icon-coin" round @click="handleSync">元数据同步</el-button>
        <el-button v-hasPerm="['metadata:datasource:sync']" v-if="form.isSync === '2'" size="mini" icon="el-icon-coin" round @click="handleSync">元数据更新</el-button>
        <el-button v-hasPerm="['metadata:datasource:word']" v-if="form.isSync === '2'" size="mini" icon="el-icon-coin" round @click="handleWord">数据库文档</el-button>
        <el-button v-hasPerm="['metadata:datasource:sync']" v-if="form.isSync === '3'" size="mini" icon="el-icon-coin" round @click="handleSync">元数据再次同步</el-button>
        <el-button v-hasPerm="['metadata:datasource:connect']" size="mini" icon="el-icon-coin" round @click="handleCheckConnection">连通性检测</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-steps :active="active" finish-status="success" align-center>
        <el-step title="数据源信息" />
        <el-step title="连接信息" />
      </el-steps>
      <el-form v-if="active == 1" ref="form" :model="form" label-width="80px" disabled>
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
      <el-form v-if="active == 2" ref="form2" :model="form2" label-width="80px" disabled>
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
      </el-form>
      <el-button v-if="active == 1" style="margin-top: 12px;" @click="handleNextStep">下一步</el-button>
      <el-button v-if="active == 2" style="margin-top: 12px;" @click="handleLastStep">上一步</el-button>
    </div>
  </el-card>
</template>

<script>
import { getDataSource, checkConnection, sync, word } from '@/api/metadata/datasource'

export default {
  name: 'DataSourceDetail',
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
      title: '数据源详情',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      active: 1,
      // 表单参数
      form: {},
      form2: {},
      // 状态数据字典
      statusOptions: [],
      // 数据源类型数据字典
      dbTypeOptions: []
    }
  },
  created() {
    console.log('id:' + this.data.id)
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
  mounted() {
    this.getDataSource(this.data.id)
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    getDataSource: function(id) {
      getDataSource(id).then(response => {
        if (response.success) {
          this.form = response.data
          this.form2 = this.form.dbSchema
        }
      })
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
      checkConnection(this.form).then(response => {
        if (response.success) {
          this.$message.success('连接成功')
        }
      })
    },
    /** 元数据同步 */
    handleSync() {
      sync(this.data.id).then(response => {
        if (response.success) {
          this.$set(this.form, 'isSync', '1')
          this.$message.success('元数据正在后台同步中，同步完成后刷新缓存即可到元数据管理中查看结果')
        }
      })
    },
    /** 数据库文档 */
    handleWord() {
      word(this.data.id).then(response => {
        const blob = new Blob([response])
        const fileName = '数据库设计文档.doc'
        if ('download' in document.createElement('a')) {
          // 非IE下载
          const elink = document.createElement('a')
          elink.download = fileName
          elink.style.display = 'none'
          elink.href = URL.createObjectURL(blob)
          document.body.appendChild(elink)
          elink.click()
          URL.revokeObjectURL(elink.href)
          // 释放URL 对象
          document.body.removeChild(elink)
        } else {
          // IE10+下载
          navigator.msSaveBlob(blob, fileName)
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
