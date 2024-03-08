<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['market:service:edit']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" placeholder="请输入服务名称" />
        </el-form-item>
        <el-form-item label="服务编号" prop="serviceNo">
          <el-input v-model="form.serviceNo" :disabled="true" />
        </el-form-item>
        <el-form-item label="服务类型" prop="serviceType">
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
          <el-form-item label="服务请求地址" prop="serviceUrl">
            <el-input v-model="form.httpService.url" placeholder="请输入服务请求地址" />
          </el-form-item>
          <el-form-item label="服务请求头" prop="serviceHeader">
            <el-input v-model="form.httpService.header" placeholder="请输入服务请求头，如{key:val}格式" />
          </el-form-item>
          <el-form-item label="服务请求参数" prop="serviceParam">
            <el-input v-model="form.httpService.param" placeholder="请输入服务请求参数，如{key:val}格式" />
          </el-form-item>
          <el-form-item label="服务请求方式" prop="serviceHttpMethod">
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
          <el-form-item label="服务wsdl地址" prop="serviceWsdl">
            <el-input v-model="form.webService.wsdl" placeholder="请输入服务wsdl地址" />
          </el-form-item>
          <el-form-item label="服务命名空间" prop="serviceTargetNamespace">
            <el-input v-model="form.webService.targetNamespace" placeholder="请输入服务命名空间" />
          </el-form-item>
          <el-form-item label="服务方法" prop="serviceMethod">
            <el-input v-model="form.webService.method" placeholder="请输入服务方法" />
          </el-form-item>
          <el-form-item label="服务请求报文" prop="serviceSoap">
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
import { getDataService, updateDataService } from '@/api/market/dataservice'

export default {
  name: 'DataServiceEdit',
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
      title: '服务集成编辑',
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
      form: {},
      // 表单校验
      rules: {
        serviceName: [
          { required: true, message: '服务名称不能为空', trigger: 'blur' }
        ],
        serviceType: [
          { required: true, message: '服务类型不能为空', trigger: 'change' }
        ],
        serviceUrl: [
          { required: true, message: '服务请求地址不能为空', trigger: 'blur' }
        ],
        serviceHttpMethod: [
          { required: true, message: '服务请求方式不能为空', trigger: 'blur' }
        ],
        serviceWsdl: [
          { required: true, message: '服务wsdl地址不能为空', trigger: 'blur' }
        ],
        serviceTargetNamespace: [
          { required: true, message: '服务命名空间不能为空', trigger: 'blur' }
        ],
        serviceSoap: [
          { required: true, message: '服务请求报文不能为空', trigger: 'blur' }
        ],
        serviceMethod: [
          { required: true, message: '服务方法不能为空', trigger: 'blur' }
        ]
      },
      // 状态数据字典
      statusOptions: [],
      serviceTypeOptions: [],
      httpMethodOptions: []
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
      getDataService(id).then(response => {
        if (response.success) {
          this.form = response.data
        }
      })
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          updateDataService(this.form).then(response => {
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
