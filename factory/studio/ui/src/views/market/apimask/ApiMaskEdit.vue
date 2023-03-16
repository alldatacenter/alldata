<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['market:mask:edit']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="数据API" prop="apiId">
          <el-select v-model="form.apiId" placeholder="请选择数据API" disabled>
            <el-option
              v-for="api in apiOptions"
              :key="api.id"
              :label="api.apiName"
              :value="api.id"
              :disabled="api.status === '0'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="脱敏名称" prop="maskName">
          <el-input v-model="form.maskName" placeholder="请输入脱敏名称" />
        </el-form-item>
        <el-form-item label="脱敏字段规则配置" prop="rules">
          <el-table
            :data="resParamList"
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
            <el-table-column prop="fieldName" label="字段名称" align="center" show-overflow-tooltip>
              <template slot-scope="scope">
                <el-button type="text" @click="fieldRule(scope.row.fieldName)">{{ scope.row.fieldName }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="fieldComment" label="描述" align="center" show-overflow-tooltip />
            <el-table-column prop="dataType" label="数据类型" align="center" show-overflow-tooltip />
            <el-table-column prop="exampleValue" label="示例值" align="center" show-overflow-tooltip />
            <el-table-column prop="cipherType" label="脱敏类型" align="center" show-overflow-tooltip />
            <el-table-column prop="cryptType" label="规则类型" align="center" show-overflow-tooltip />
          </el-table>
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

      <!-- 脱敏规则对话框 -->
      <el-dialog :title="cipher.title" :visible.sync="cipher.open" width="400px" append-to-body>
        <el-form ref="form2" :model="form2" :rules="rules2" label-width="80px">
          <el-form-item label="字段名称" prop="fieldName">
            <el-input v-model="form2.fieldName" placeholder="请输入字段名称" :disabled="true" />
          </el-form-item>
          <el-form-item label="脱敏类型" prop="cipherType">
            <el-select v-model="form2.cipherType" clearable placeholder="请选择脱敏类型" @change="cipherTypeSelectChanged">
              <el-option
                v-for="dict in cipherTypeOptions"
                :key="dict.id"
                :label="dict.itemValue"
                :value="dict.itemText"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="规则类型" prop="cryptType">
            <el-select v-model="form2.cryptType" clearable placeholder="请选择规则类型">
              <el-option
                v-for="dict in cryptTypeOptions"
                :key="dict.id"
                :label="dict.itemValue"
                :value="dict.itemText"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="submitFieldCipherForm">确 定</el-button>
          <el-button @click="cipher.open = false">取 消</el-button>
        </div>
      </el-dialog>
    </div>
  </el-card>
</template>

<script>
import { getApiMask, updateApiMask } from '@/api/market/apimask'
import { listDataApi, getDataApi } from '@/api/market/dataapi'

export default {
  name: 'ApiMaskEdit',
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
      title: '数据API脱敏编辑',
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
        id: undefined,
        apiId: undefined,
        maskName: undefined,
        rules: [],
        status: '1',
        remark: undefined
      },
      // 表单校验
      rules: {
        apiId: [
          { required: true, message: '数据API不能为空', trigger: 'blur' }
        ],
        maskName: [
          { required: true, message: '脱敏名称不能为空', trigger: 'blur' }
        ]
      },
      form2: {
        fieldName: undefined,
        cipherType: undefined,
        cryptType: undefined
      },
      rules2: {
        fieldName: [
          { required: true, message: '字段名称不能为空', trigger: 'blur' }
        ]
      },
      // 状态数据字典
      statusOptions: [],
      // 数据API数据字典
      apiOptions: [],
      // 脱敏字段信息
      resParamList: [],
      // 脱敏类型数据字典
      cipherTypeOptions: [],
      cryptTypeOptions: [],
      // 正则替换数据字典
      regexCryptoOptions: [],
      // 加密算法数据字典
      algorithmCryptoOptions: [],
      cipher: {
        // 是否显示弹出层
        open: false,
        // 弹出层标题
        title: '脱敏规则配置'
      }
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDicts('data_cipher_type').then(response => {
      if (response.success) {
        this.cipherTypeOptions = response.data
      }
    })
    this.getDicts('data_regex_crypto').then(response => {
      if (response.success) {
        this.regexCryptoOptions = response.data
      }
    })
    this.getDicts('data_algorithm_crypto').then(response => {
      if (response.success) {
        this.algorithmCryptoOptions = response.data
      }
    })
    this.getDataApiList()
  },
  mounted() {
    this.getApiMask()
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    getDataApiList() {
      listDataApi().then(response => {
        if (response.success) {
          this.apiOptions = response.data
        }
      })
    },
    /** 获取详情 */
    async getApiMask() {
      this.form = await getApiMask(this.data.id).then(response => {
        if (response.success) {
          return response.data
        }
      }) || {}
      if (this.form && this.form.apiId) {
        const dataApi = await getDataApi(this.form.apiId).then(response => {
          if (response.success) {
            return response.data
          }
        }) || {}
        if (dataApi && dataApi.resParams.length > 0) {
          this.resParamList = dataApi.resParams
          this.form.rules.forEach(rule => {
            const fieldParamIndex = this.resParamList.findIndex((param) => {
              return param.fieldName === rule.fieldName
            })
            if (fieldParamIndex !== -1) {
              const cipher = this.cipherTypeOptions.find((item) => {
                return item.itemText === rule.cipherType
              })
              let crypt = {}
              if (rule.cipherType === '1') {
                crypt = this.regexCryptoOptions.find((item) => {
                  return item.itemText === rule.cryptType
                })
              } else if (rule.cipherType === '2') {
                crypt = this.algorithmCryptoOptions.find((item) => {
                  return item.itemText === rule.cryptType
                })
              }
              const resParam = Object.assign({}, this.resParamList[fieldParamIndex], { cipherType: (cipher && cipher.itemValue) || undefined, cryptType: (crypt && crypt.itemValue) || undefined })
              this.$set(this.resParamList, fieldParamIndex, resParam)
            }
          })
        }
      }
    },
    fieldRule(fieldName) {
      this.cipher.open = true
      this.form2.fieldName = fieldName
      this.form2.cipherType = undefined
      this.form2.cryptType = undefined
      this.cryptTypeOptions = []
    },
    cipherTypeSelectChanged(val) {
      this.form2.cryptType = undefined
      this.cryptTypeOptions = []
      if (val === '1') {
        // 正则替换
        this.cryptTypeOptions = this.regexCryptoOptions
      } else if (val === '2') {
        // 加密算法
        this.cryptTypeOptions = this.algorithmCryptoOptions
      }
    },
    submitFieldCipherForm() {
      const fieldRuleIndex = this.form.rules.findIndex((item) => {
        return item.fieldName === this.form2.fieldName
      })
      if (fieldRuleIndex !== -1) {
        // 当返回-1时，则说明数组中没有
        this.form.rules.splice(fieldRuleIndex, 1)
      }
      if (this.form2.cipherType && this.form2.cryptType) {
        this.form.rules.push(this.form2)
      }
      const cipher = this.cipherTypeOptions.find((item) => {
        return item.itemText === this.form2.cipherType
      })
      const crypt = this.cryptTypeOptions.find((item) => {
        return item.itemText === this.form2.cryptType
      })
      const fieldParamIndex = this.resParamList.findIndex((item) => {
        return item.fieldName === this.form2.fieldName
      })
      const resParam = Object.assign({}, this.resParamList[fieldParamIndex], { cipherType: (cipher && cipher.itemValue) || undefined, cryptType: (crypt && crypt.itemValue) || undefined })
      this.$set(this.resParamList, fieldParamIndex, resParam)
      this.cipher.open = false
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.rules.length <= 0) {
            this.$message.error('脱敏字段规则配置不能为空')
            return
          }
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          updateApiMask(this.form).then(response => {
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
