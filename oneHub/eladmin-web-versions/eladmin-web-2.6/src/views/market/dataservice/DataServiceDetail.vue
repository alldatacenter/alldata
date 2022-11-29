<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-row>
        <el-col :span="24">
          <el-tabs type="border-card">
            <el-tab-pane label="请求头">
              <el-table
                :data="serviceHeaderList"
                stripe
                border
                :max-height="300"
                style="width: 100%; margin: 15px 0;"
              >
                <el-table-column label="序号" width="55" align="center">
                  <template slot-scope="scope">
                    <span>{{ scope.$index +1 }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="KEY" label="键" align="center" show-overflow-tooltip />
                <el-table-column prop="VALUE" label="值" align="center" show-overflow-tooltip />
                <el-table-column prop="DESCRIPTION" label="描述" align="center" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-col>
      </el-row>
      <el-divider content-position="left">服务信息</el-divider>
      <el-form ref="form" :model="form" label-width="80px" disabled>
        <el-form-item label="服务名称">
          <el-input v-model="form.serviceName" placeholder="请输入服务名称" />
        </el-form-item>
        <el-form-item label="服务编号">
          <el-input v-model="form.serviceNo" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="form.serviceType">
            <el-option
              v-for="item in serviceTypeOptions"
              :key="item.id"
              :label="item.itemValue"
              :value="item.itemText"
            />
          </el-select>
        </el-form-item>
        <template v-if="form.serviceType === '1'">
          <el-form-item label="服务请求地址">
            <el-input v-model="form.httpService.url" placeholder="请输入服务请求地址" />
          </el-form-item>
          <el-form-item label="服务请求头">
            <el-input v-model="form.httpService.header" placeholder="请输入服务请求头，如{key:val}格式" />
          </el-form-item>
          <el-form-item label="服务请求参数">
            <el-input v-model="form.httpService.param" placeholder="请输入服务请求参数，如{key:val}格式" />
          </el-form-item>
          <el-form-item label="服务请求方式">
            <el-select v-model="form.httpService.httpMethod" placeholder="请选择请求方式">
              <el-option
                v-for="dict in httpMethodOptions"
                :key="dict.id"
                :label="dict.itemValue"
                :value="dict.itemText"
              />
            </el-select>
          </el-form-item>
        </template>
        <template v-if="form.serviceType === '2'">
          <el-form-item label="服务wsdl地址">
            <el-input v-model="form.webService.wsdl" placeholder="请输入服务wsdl地址" />
          </el-form-item>
          <el-form-item label="服务命名空间">
            <el-input v-model="form.webService.targetNamespace" placeholder="请输入服务命名空间" />
          </el-form-item>
          <el-form-item label="服务方法">
            <el-input v-model="form.webService.method" placeholder="请输入服务方法" />
          </el-form-item>
          <el-form-item label="服务请求报文">
            <el-input v-model="form.webService.soap" type="textarea" placeholder="请输入服务请求报文" />
          </el-form-item>
        </template>
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
import { getDataServiceDetail } from '@/api/market/dataservice'

export default {
  name: 'DataServiceDetail',
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
      title: '服务集成详情',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 表单参数
      form: {},
      // 状态数据字典
      statusOptions: [],
      serviceTypeOptions: [],
      httpMethodOptions: [],
      serviceHeaderList: []
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDicts('data_service_type').then(response => {
      if (response.success) {
        this.serviceTypeOptions = response.data
      }
    })
    this.getDicts('data_req_method').then(response => {
      if (response.success) {
        this.httpMethodOptions = response.data
      }
    })
  },
  mounted() {
    this.getDataService(this.data.id)
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    getDataService: function(id) {
      getDataServiceDetail(id).then(response => {
        if (response.success) {
          const { data } = response
          this.form = data.data
          this.serviceHeaderList.push({ 'KEY': 'service_key', 'VALUE': data.header.serviceKey, 'DESCRIPTION': '' })
          this.serviceHeaderList.push({ 'KEY': 'secret_key', 'VALUE': data.header.secretKey, 'DESCRIPTION': '' })
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
