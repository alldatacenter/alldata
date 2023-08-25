<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['market:api:add']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-steps :active="active" finish-status="success" align-center>
        <el-step title="属性配置" />
        <el-step title="执行配置" />
        <el-step title="参数配置" />
      </el-steps>
      <el-form v-if="active == 1" ref="form1" :model="form1" :rules="rules1" label-width="80px">
        <el-form-item label="API名称" prop="apiName">
          <el-input v-model="form1.apiName" placeholder="请输入API名称" />
        </el-form-item>
        <el-form-item label="API版本" prop="apiVersion">
          <el-input v-model="form1.apiVersion" placeholder="请输入API版本，如v1.0.0" />
        </el-form-item>
        <el-form-item label="API路径" prop="apiUrl">
          <el-input v-model="form1.apiUrl" placeholder="请输入API路径" />
        </el-form-item>
        <el-form-item label="请求方式" prop="reqMethod">
          <el-select v-model="form1.reqMethod" placeholder="请选择请求方式">
            <el-option
              v-for="dict in reqMethodOptions"
              :key="dict.id"
              :label="dict.itemValue"
              :value="dict.itemText"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="返回格式" prop="resType">
          <el-select v-model="form1.resType" placeholder="请选择返回格式">
            <el-option
              v-for="dict in resTypeOptions"
              :key="dict.id"
              :label="dict.itemValue"
              :value="dict.itemText"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="IP黑名单" prop="deny">
          <el-input v-model="form1.deny" type="textarea" placeholder="请输入IP黑名单多个用英文,隔开" />
        </el-form-item>
        <el-form-item label="是否限流" prop="rateLimit">
          <el-radio-group v-model="form1.rateLimit.enable">
            <el-radio
              v-for="dict in whetherOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{ dict.itemValue }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form1.rateLimit.enable === '1'" label="限流配置">
          每<el-input-number v-model="form1.rateLimit.seconds" controls-position="right" :min="1" />秒内限制请求
          <el-input-number v-model="form1.rateLimit.times" controls-position="right" :min="1" />次
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form1.status" disabled>
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{ dict.itemValue }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form1.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <el-form v-if="active == 2" ref="form2" :model="form2" :rules="rules2" label-width="80px">
        <el-form-item label="配置方式" prop="configType">
          <el-select v-model="form2.configType" placeholder="请选择配置方式" @change="configTypeSelectChanged">
            <el-option
              v-for="dict in configTypeOptions"
              :key="dict.id"
              :label="dict.itemValue"
              :value="dict.itemText"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="sourceId">
          <el-select v-model="form2.sourceId" placeholder="请选择数据源" @change="sourceSelectChanged">
            <el-option
              v-for="source in sourceOptions"
              :key="source.id"
              :label="source.sourceName"
              :value="source.id"
              :disabled="source.status === '0'"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form2.configType === '1'" label="数据库表" prop="tableName">
          <el-select v-model="form2.table" value-key="id" placeholder="请选择数据库表" @change="tableSelectChanged">
            <el-option
              v-for="item in tableOptions"
              :key="item.id"
              :label="item.tableComment ? item.tableComment : item.tableName"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form2.configType === '1'" label="字段列表">
          <el-table
            :data="form2.fieldParams"
            stripe
            border
            :max-height="300"
            style="width: 100%; margin: 15px 0;"
          >
            <el-table-column prop="columnPosition" label="序号" width="55" align="center" />
            <el-table-column prop="columnName" label="列名" align="center" show-overflow-tooltip />
            <el-table-column prop="dataType" label="数据类型" align="center" show-overflow-tooltip />
            <el-table-column prop="dataLength" label="数据长度" align="center" show-overflow-tooltip />
            <el-table-column prop="dataPrecision" label="数据精度" align="center" show-overflow-tooltip />
            <el-table-column prop="dataScale" label="数据小数位" align="center" show-overflow-tooltip />
            <el-table-column prop="columnKey" label="是否主键" align="center" show-overflow-tooltip>
              <template slot-scope="scope">
                <span v-if="scope.row.columnKey === '1'">Y</span>
                <span v-if="scope.row.columnKey === '0'">N</span>
              </template>
            </el-table-column>
            <el-table-column prop="columnNullable" label="是否允许为空" align="center" show-overflow-tooltip>
              <template slot-scope="scope">
                <span v-if="scope.row.columnNullable === '1'">Y</span>
                <span v-if="scope.row.columnNullable === '0'">N</span>
              </template>
            </el-table-column>
            <el-table-column prop="dataDefault" label="列默认值" align="center" show-overflow-tooltip />
            <el-table-column prop="columnComment" label="列注释" align="center" show-overflow-tooltip />
            <el-table-column prop="reqable" label="是否作为请求参数" align="center" width="50">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.reqable" true-label="1" false-label="0" @change="checked=>reqCheckChange(scope.row, checked)" />
              </template>
            </el-table-column>
            <el-table-column prop="resable" label="是否作为返回参数" align="center" width="50">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.resable" true-label="1" false-label="0" @change="checked=>resCheckChange(scope.row, checked)" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-row v-if="form2.configType === '2'">
          <el-col :span="24">
            <sql-editor
              ref="sqleditor"
              :value="form2.sqlText"
              style="height: 300px;margin: 10px 10px;"
              @changeTextarea="changeTextarea($event)"
            />
          </el-col>
        </el-row>
        <el-form-item v-if="form2.configType === '2'">
          <el-button size="mini" type="primary" @click="sqlParse">SQL解析</el-button>
        </el-form-item>
      </el-form>
      <el-form v-if="active == 3" ref="form3" :model="form3" label-width="80px">
        <el-divider content-position="left">请求参数</el-divider>
        <el-table
          :data="form3.reqParams"
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
          <el-table-column prop="paramName" label="参数名称" align="center" show-overflow-tooltip />
          <el-table-column prop="nullable" label="是否允许为空" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row.nullable" true-label="1" false-label="0" />
            </template>
          </el-table-column>
          <el-table-column prop="paramComment" label="描述" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-input v-model="scope.row.paramComment" placeholder="请输入描述" />
            </template>
          </el-table-column>
          <el-table-column prop="paramType" label="参数类型" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-select v-model="scope.row.paramType" placeholder="请选择参数类型">
                <el-option
                  v-for="dict in paramTypeOptions"
                  :key="dict.id"
                  :label="dict.itemValue"
                  :value="dict.itemText"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="whereType" label="操作符" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-select v-model="scope.row.whereType" placeholder="请选择操作符">
                <el-option
                  v-for="dict in whereTypeOptions"
                  :key="dict.id"
                  :label="dict.itemValue"
                  :value="dict.itemText"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="exampleValue" label="示例值" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-input v-model="scope.row.exampleValue" placeholder="请输入示例值" />
            </template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-input v-model="scope.row.defaultValue" placeholder="请输入默认值" />
            </template>
          </el-table-column>
        </el-table>
        <el-divider content-position="left">返回字段</el-divider>
        <el-table
          :data="form3.resParams"
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
          <el-table-column prop="fieldName" label="字段名称" align="center" show-overflow-tooltip />
          <el-table-column prop="fieldComment" label="描述" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-input v-model="scope.row.fieldComment" placeholder="请输入描述" />
            </template>
          </el-table-column>
          <el-table-column prop="dataType" label="数据类型" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-input v-model="scope.row.dataType" placeholder="请输入数据类型" />
            </template>
          </el-table-column>
          <el-table-column prop="exampleValue" label="示例值" align="center" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-input v-model="scope.row.exampleValue" placeholder="请输入示例值" />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <el-button v-if="active < 3" style="margin-top: 12px;" @click="handleNextStep">下一步</el-button>
      <el-button v-if="active > 1" style="margin-top: 12px;" @click="handleLastStep">上一步</el-button>
    </div>
  </el-card>
</template>

<script>
import { addDataApi, sqlParse } from '@/api/market/dataapi'
import { listDataSource } from '@/api/metadata/datasource'
import { listDataTable } from '@/api/metadata/datatable'
import { listDataColumn } from '@/api/metadata/datacolumn'
import SqlEditor from '@/components/SqlEditor'

export default {
  name: 'DataApiAdd',
  components: {
    SqlEditor
  },
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
      title: '数据API新增',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false,
        showExample: false
      },
      // 保存按钮
      loadingOptions: {
        loading: false,
        loadingText: '保存',
        isDisabled: false
      },
      active: 1,
      // 表单参数
      form1: {
        id: undefined,
        apiName: undefined,
        apiVersion: undefined,
        apiUrl: undefined,
        reqMethod: undefined,
        resType: undefined,
        deny: undefined,
        rateLimit: {
          enable: '1',
          times: 5,
          seconds: 60
        },
        status: '1',
        remark: undefined,
        executeConfig: {},
        reqParams: [],
        resParams: []
      },
      // 表单校验
      rules1: {
        apiName: [
          { required: true, message: 'API名称不能为空', trigger: 'blur' }
        ],
        apiVersion: [
          { required: true, message: 'API版本不能为空', trigger: 'blur' }
        ],
        apiUrl: [
          { required: true, message: 'API路径不能为空', trigger: 'blur' }
        ],
        reqMethod: [
          { required: true, message: '请求方式不能为空', trigger: 'change' }
        ],
        resType: [
          { required: true, message: '返回格式不能为空', trigger: 'change' }
        ]
      },
      form2: {
        configType: undefined,
        sourceId: undefined,
        tableId: undefined,
        tableName: undefined,
        fieldParams: [],
        sqlText: undefined
      },
      rules2: {
        configType: [
          { required: true, message: '配置方式不能为空', trigger: 'change' }
        ],
        sourceId: [
          { required: true, message: '数据源不能为空', trigger: 'change' }
        ]
      },
      form3: {
        reqParams: [],
        resParams: []
      },
      // 请求方式数据字典
      reqMethodOptions: [],
      // 返回格式数据字典
      resTypeOptions: [],
      // 是否数据字典
      whetherOptions: [],
      // 状态数据字典
      statusOptions: [],
      // 数据源数据字典
      sourceOptions: [],
      // 数据库表数据字典
      tableOptions: [],
      // 配置方式数据字典
      configTypeOptions: [],
      // 操作符数据字典
      whereTypeOptions: [],
      // 参数类型数据字典
      paramTypeOptions: []
    }
  },
  created() {
    this.getDicts('data_req_method').then(response => {
      if (response.success) {
        this.reqMethodOptions = response.data
      }
    })
    this.getDicts('data_res_type').then(response => {
      if (response.success) {
        this.resTypeOptions = response.data
      }
    })
    this.getDicts('sys_yes_no').then(response => {
      if (response.success) {
        this.whetherOptions = response.data
      }
    })
    this.getDicts('data_api_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDataSourceList()
    this.getDicts('data_config_type').then(response => {
      if (response.success) {
        this.configTypeOptions = response.data
      }
    })
    this.getDicts('data_where_type').then(response => {
      if (response.success) {
        this.whereTypeOptions = response.data
      }
    })
    this.getDicts('data_param_type').then(response => {
      if (response.success) {
        this.paramTypeOptions = response.data
      }
    })
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    getDataSourceList() {
      listDataSource().then(response => {
        if (response.success) {
          this.sourceOptions = response.data
        }
      })
    },
    /** 步骤条下一步 */
    handleNextStep() {
      if (this.active === 1) {
        this.$refs['form1'].validate(valid => {
          if (valid) {
            this.active++
          }
        })
      } else if (this.active === 2) {
        this.$refs['form2'].validate(valid => {
          if (valid) {
            this.active++
          }
        })
      }
    },
    /** 步骤条上一步 */
    handleLastStep() {
      this.active--
    },
    changeTextarea(val) {
      this.form2.sqlText = val
    },
    configTypeSelectChanged(val) {
      if (this.form2.configType === '1' && this.form2.sourceId && this.tableOptions.length <= 0) {
        const data = {}
        data.sourceId = this.form2.sourceId
        listDataTable(data).then(response => {
          if (response.success) {
            this.tableOptions = response.data
            this.form2.fieldParams = []
          }
        })
      }
    },
    sourceSelectChanged(val) {
      if (this.form2.configType && this.form2.configType === '1') {
        const data = {}
        data.sourceId = val
        listDataTable(data).then(response => {
          if (response.success) {
            this.tableOptions = response.data
            this.form2.fieldParams = []
          }
        })
      }
    },
    tableSelectChanged(item) {
      const data = {}
      data.sourceId = item.sourceId
      data.tableId = item.id
      this.form2.tableId = item.id
      this.form2.tableName = item.tableName
      listDataColumn(data).then(response => {
        if (response.success) {
          this.form2.fieldParams = response.data
          this.form3.reqParams = []
          this.form3.resParams = []
        }
      })
    },
    sqlParse() {
      if (!this.form2.sourceId) {
        this.$message.error('数据源不能为空')
        return
      }
      if (!this.form2.sqlText) {
        this.$message.error('解析SQL不能为空')
        return
      }
      const data = {}
      data.sourceId = this.form2.sourceId
      data.sqlText = this.form2.sqlText
      sqlParse(data).then(response => {
        if (response.success) {
          const { data } = response
          this.form3.reqParams = data.reqParams
          this.form3.resParams = data.resParams
          this.$message.success('解析成功，请进行下一步')
        }
      })
    },
    reqCheckChange(row, checked) {
      if (checked === '1') {
        const json = {}
        json.paramName = row.columnName
        json.paramComment = row.columnComment || undefined
        json.nullable = '0'
        this.form3.reqParams.push(json)
      } else {
        this.form3.reqParams.splice(this.form3.reqParams.findIndex(item => item.paramName === row.columnName), 1)
      }
    },
    resCheckChange(row, checked) {
      if (checked === '1') {
        const json = {}
        json.fieldName = row.columnName
        json.fieldComment = row.columnComment || undefined
        json.dataType = row.dataType || undefined
        this.form3.resParams.push(json)
      } else {
        this.form3.resParams.splice(this.form3.resParams.findIndex(item => item.fieldName === row.columnName), 1)
      }
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form3'].validate(valid => {
        if (valid) {
          if (this.form3.reqParams.length <= 0) {
            this.$message.error('请求参数不能为空')
            return
          }
          if (this.form3.resParams.length <= 0) {
            this.$message.error('返回字段不能为空')
            return
          }
          this.form1.sourceId = this.form2.sourceId
          this.form1.executeConfig = this.form2
          this.form1.reqParams = this.form3.reqParams
          this.form1.resParams = this.form3.resParams
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          addDataApi(this.form1).then(response => {
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
