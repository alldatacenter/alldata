<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" label-width="80px" disabled>
        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" />
        </el-form-item>
        <el-form-item label="调用者ip" prop="callerIp">
          <el-input v-model="form.callerIp" />
        </el-form-item>
        <el-form-item label="调用请求头" prop="callerHeader">
          <el-input v-model="form.callerHeader" />
        </el-form-item>
        <el-form-item label="调用请求参数" prop="callerParam">
          <el-input v-model="form.callerParam" />
        </el-form-item>
        <el-form-item label="调用报文" prop="callerSoap">
          <el-input v-model="form.callerSoap" />
        </el-form-item>
        <el-form-item label="调用时间" prop="callerDate">
          <el-input v-model="form.callerDate" />
        </el-form-item>
        <el-form-item label="调用耗时" prop="time">
          <el-input v-model="form.time" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{dict.itemValue}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="信息记录" prop="msg">
          <el-input v-model="form.msg" />
        </el-form-item>
      </el-form>
    </div>
  </el-card>
</template>

<script>
import { getDataServiceLog } from '@/api/market/dataservicelog'

export default {
  name: 'ServiceLogDetail',
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
      title: '服务日志详情',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showDetail: false
      },
      // 表单参数
      form: {},
      // 状态数据字典
      statusOptions: []
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_normal_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
  },
  mounted() {
    this.getLog(this.data.id)
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    getLog: function(id) {
      getDataServiceLog(id).then(response => {
        if (response.success) {
          this.form = response.data
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
