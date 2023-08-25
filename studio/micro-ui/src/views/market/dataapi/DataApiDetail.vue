<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['market:api:word']" size="mini" icon="el-icon-coin" round @click="handleWord">接口文档</el-button>
        <el-button v-hasPerm="['market:api:example']" size="mini" icon="el-icon-s-data" round @click="handleExample">接口示例</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-steps :active="active" finish-status="success" align-center>
        <el-step title="属性配置" />
        <el-step title="执行配置" />
        <el-step title="参数配置" />
      </el-steps>
      <el-form v-if="active == 1" ref="form1" :model="form1" label-width="80px" disabled>
        <el-form-item label="API名称" prop="apiName">
          <el-input v-model="form1.apiName" placeholder="请输入API名称" />
        </el-form-item>
        <el-form-item label="API版本" prop="apiVersion">
          <el-input v-model="form1.apiVersion" placeholder="请输入API版本" />
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
          <el-radio-group v-model="form1.status">
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
      <el-form v-if="active == 2" ref="form2" :model="form2" label-width="80px" disabled>
        <el-form-item label="配置方式" prop="configType">
          <el-select v-model="form2.configType" placeholder="请选择配置方式">
            <el-option
              v-for="dict in configTypeOptions"
              :key="dict.id"
              :label="dict.itemValue"
              :value="dict.itemText"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="sourceId">
          <el-select v-model="form2.sourceId" placeholder="请选择数据源">
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
          <el-select v-model="form2.table" value-key="id" placeholder="请选择数据库表">
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
                <el-checkbox v-model="scope.row.reqable" true-label="1" false-label="0" />
              </template>
            </el-table-column>
            <el-table-column prop="resable" label="是否作为返回参数" align="center" width="50">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.resable" true-label="1" false-label="0" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-row v-if="form2.configType === '2'">
          <el-col :span="24">
            <sql-editor
              ref="sqleditor"
              :value="form2.sqlText"
              :read-only="true"
              style="height: 300px;margin: 10px 10px;"
            />
          </el-col>
        </el-row>
      </el-form>
      <el-form v-if="active == 3" ref="form3" :model="form3" label-width="80px" disabled>
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
          <el-table-column prop="paramComment" label="描述" align="center" show-overflow-tooltip />
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
          <el-table-column prop="exampleValue" label="示例值" align="center" show-overflow-tooltip />
          <el-table-column prop="defaultValue" label="默认值" align="center" show-overflow-tooltip />
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
          <el-table-column prop="fieldComment" label="描述" align="center" show-overflow-tooltip />
          <el-table-column prop="dataType" label="数据类型" align="center" show-overflow-tooltip />
          <el-table-column prop="exampleValue" label="示例值" align="center" show-overflow-tooltip />
        </el-table>
      </el-form>
      <el-button v-if="active < 3" style="margin-top: 12px;" @click="handleNextStep">下一步</el-button>
      <el-button v-if="active > 1" style="margin-top: 12px;" @click="handleLastStep">上一步</el-button>
    </div>
  </el-card>
</template>

<script>
import { getDataApi, word } from '@/api/market/dataapi'
import { listDataSource } from '@/api/metadata/datasource'
import { listDataTable } from '@/api/metadata/datatable'
import SqlEditor from '@/components/SqlEditor'

export default {
  name: 'DataApiDetail',
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
      title: '数据API详情',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false,
        showExample: false
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
        times: 5,
        seconds: 60,
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
          { required: true, message: '请求方式不能为空', trigger: 'blur' }
        ],
        resType: [
          { required: true, message: '返回格式不能为空', trigger: 'blur' }
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
          { required: true, message: '配置方式不能为空', trigger: 'blur' }
        ],
        sourceId: [
          { required: true, message: '数据源不能为空', trigger: 'blur' }
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
    console.log('id:' + this.data.id)
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
  mounted() {
    this.getDataApi(this.data.id)
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
      this.active++
    },
    /** 步骤条上一步 */
    handleLastStep() {
      this.active--
    },
    /** 获取详情 */
    async getDataApi(id) {
      this.form1 = await getDataApi(id).then(response => {
        if (response.success) {
          return response.data
        }
      }) || {}
      this.form2 = this.form1.executeConfig
      this.form2.table = { id: this.form2.tableId }
      this.form3.reqParams = this.form1.reqParams
      this.form3.resParams = this.form1.resParams
      if (this.form2.configType === '1') {
        this.tableOptions = await listDataTable({ sourceId: this.form2.sourceId }).then(response => {
          if (response.success) {
            return response.data
          }
        }) || []
      }
    },
    handleExample() {
      this.showOptions.data.id = this.data.id
      this.showOptions.showList = false
      this.showOptions.showAdd = false
      this.showOptions.showEdit = false
      this.showOptions.showDetail = false
      this.showOptions.showExample = true
      this.$emit('showCard', this.showOptions)
    },
    /** 接口文档 */
    handleWord() {
      word(this.data.id).then(response => {
        const blob = new Blob([response])
        const fileName = '接口文档.docx'
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
