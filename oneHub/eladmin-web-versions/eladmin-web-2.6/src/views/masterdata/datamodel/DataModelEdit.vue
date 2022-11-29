<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['masterdata:model:edit']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-tabs v-model="activeName">
        <el-tab-pane label="基本信息" name="first">
          <el-form ref="form" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="form.modelName" placeholder="请输入模型名称" />
            </el-form-item>
            <el-form-item label="逻辑表" prop="modelLogicTable">
              <el-input v-model="form.modelLogicTable" placeholder="请输入逻辑表" />
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
        </el-tab-pane>
        <el-tab-pane label="字段信息" name="second">
          <el-button type="primary" @click="addRow">添加</el-button>
          <el-form ref="secondTable" :model="form" :rules="rules" size="mini">
            <el-table :data="form.modelColumns" border style="width: 100%; margin: 15px 0;">
              <el-table-column label="序号" type="index" align="center" width="55" />
              <el-table-column label="列名称">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.columnName'" :rules="rules.columnName">
                    <el-input :disabled="scope.row.isSystem === '1'" v-model="scope.row.columnName" clearable placeholder="请输入列名称" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列描述">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.columnComment'" :rules="rules.columnComment">
                    <el-input :disabled="scope.row.isSystem === '1'" v-model="scope.row.columnComment" clearable placeholder="请输入列描述" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列类型">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.columnType'" :rules="rules.columnType">
                    <el-select :disabled="scope.row.isSystem === '1'" v-model="scope.row.columnType" clearable placeholder="请选择">
                      <el-option
                        v-for="item in columnTypeOptions"
                        :key="item.id"
                        :label="item.itemValue"
                        :value="item.itemText"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="默认值">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.defaultValue'">
                    <el-input :disabled="scope.row.isSystem === '1'" v-model="scope.row.defaultValue" clearable placeholder="请输入默认值" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="长度" width="80">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.columnLength'">
                    <el-input-number :disabled="scope.row.isSystem === '1'" v-model="scope.row.columnLength" style="width: 60px;" :controls="false" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="小数位" width="70">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.columnScale'">
                    <el-input-number :disabled="scope.row.isSystem === '1'" v-model="scope.row.columnScale" style="width: 50px;" :controls="false" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="主键" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isPk'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isPk" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="必填" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isRequired'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isRequired" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-button :disabled="scope.row.isSystem === '1' || scope.$index < 8" icon="el-icon-arrow-up" circle @click="upRow(scope.$index)" />
                    <el-button :disabled="scope.row.isSystem === '1' || scope.$index === (form.modelColumns.length - 1)" icon="el-icon-arrow-down" circle @click="downRow(scope.$index)" />
                    <el-button :disabled="scope.row.isSystem === '1'" icon="el-icon-delete" circle @click="delRow(scope.$index)" />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="页面属性" name="third">
          <el-form ref="thirdTable" :model="form" :rules="rules" size="mini">
            <el-table :data="form.modelColumns" border style="width: 100%; margin: 15px 0;">
              <el-table-column label="序号" type="index" align="center" width="55" />
              <el-table-column label="列名称">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input :disabled="true" v-model="scope.row.columnName" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列描述">
                <template slot-scope="scope">
                  <el-form-item>
                    <el-input :disabled="true" v-model="scope.row.columnComment" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="插入" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isInsert'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isInsert" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="编辑" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isEdit'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isEdit" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="详情" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isDetail'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isDetail" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="列表" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isList'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isList" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="查询" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isQuery'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isQuery" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="查询方式" width="120">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.queryType'">
                    <el-select :disabled="scope.row.isSystem === '1' || scope.row.isQuery === '0'" v-model="scope.row.queryType" clearable placeholder="请选择">
                      <el-option
                        v-for="item in queryTypeOptions"
                        :key="item.id"
                        :label="item.itemValue"
                        :value="item.itemText"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="标准" align="center" width="55">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.isBindDict'">
                    <el-checkbox :disabled="scope.row.isSystem === '1'" v-model="scope.row.isBindDict" true-label="1" false-label="0" />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="标准字典类别" width="120">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.bindDictTypeId'">
                    <el-select :disabled="scope.row.isSystem === '1' || scope.row.isBindDict === '0'" v-model="scope.row.bindDictTypeId" clearable placeholder="请选择">
                      <el-option
                        v-for="item in dictTypeOptions"
                        :key="item.id"
                        :label="item.gbTypeName"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="标准字典字段" width="120">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.bindDictColumn'">
                    <el-select :disabled="scope.row.isSystem === '1' || scope.row.isBindDict === '0'" v-model="scope.row.bindDictColumn" clearable placeholder="请选择">
                      <el-option
                        v-for="item in gbColumnOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="显示类型" width="120">
                <template slot-scope="scope">
                  <el-form-item :prop="'modelColumns.' + scope.$index + '.htmlType'" :rules="rules.htmlType">
                    <el-select :disabled="scope.row.isSystem === '1'" v-model="scope.row.htmlType" clearable placeholder="请选择">
                      <el-option
                        v-for="item in htmlTypeOptions"
                        :key="item.id"
                        :label="item.itemValue"
                        :value="item.itemText"
                      />
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-card>
</template>

<script>
import { getDataModel, updateDataModel } from '@/api/masterdata/datamodel'
import { listDataDictType } from '@/api/standard/datadict'

export default {
  name: 'DataModelEdit',
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
      title: '数据模型编辑',
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
        modelName: [
          { required: true, message: '模型名称不能为空', trigger: 'blur' }
        ],
        modelLogicTable: [
          { required: true, message: '逻辑表不能为空', trigger: 'blur' }
        ],
        columnName: [
          { required: true, message: '列名称不能为空', trigger: 'blur' }
        ],
        columnComment: [
          { required: true, message: '列描述不能为空', trigger: 'blur' }
        ],
        columnType: [
          { required: true, message: '列类型不能为空', trigger: 'change' }
        ],
        htmlType: [
          { required: true, message: '显示类型不能为空', trigger: 'change' }
        ]
      },
      // 状态数据字典
      statusOptions: [],
      activeName: 'first',
      columnTypeOptions: [],
      queryTypeOptions: [],
      htmlTypeOptions: [],
      dictTypeOptions: [],
      gbColumnOptions: [
        { value: 'gb_code', label: '标准编码' },
        { value: 'gb_name', label: '标准名称' }
      ]
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    this.getDicts('data_type_mysql').then(response => {
      if (response.success) {
        this.columnTypeOptions = response.data
      }
    })
    this.getDicts('data_query_type').then(response => {
      if (response.success) {
        this.queryTypeOptions = response.data
      }
    })
    this.getDicts('data_html_type').then(response => {
      if (response.success) {
        this.htmlTypeOptions = response.data
      }
    })
    listDataDictType().then(response => {
      if (response.success) {
        this.dictTypeOptions = response.data
      }
    })
  },
  mounted() {
    this.getDataModel(this.data.id)
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    getDataModel: function(id) {
      getDataModel(id).then(response => {
        if (response.success) {
          this.form = response.data
        }
      })
    },
    addRow() {
      const item = {
        columnName: '',
        columnComment: '',
        columnType: 'varchar',
        columnLength: '255',
        columnScale: '0',
        defaultValue: '',
        isSystem: '0',
        isPk: '0',
        isRequired: '1',
        isInsert: '1',
        isEdit: '1',
        isDetail: '1',
        isList: '1',
        isQuery: '0',
        queryType: '',
        htmlType: 'input',
        isBindDict: '0',
        bindDictTypeId: '',
        bindDictColumn: ''
      }
      this.form.modelColumns.push(item)
    },
    upRow(index) {
      if (index > 0) {
        const data = this.form.modelColumns[index - 1]
        this.form.modelColumns.splice(index - 1, 1)
        this.form.modelColumns.splice(index, 0, data)
      } else {
        this.$message.warning('已经是第一条，不可上移')
        return false
      }
    },
    downRow(index) {
      if ((index + 1) === this.form.modelColumns.length) {
        this.$message.warning('已经是最后一条，不可下移')
        return false
      } else {
        const data = this.form.modelColumns[index + 1]
        this.form.modelColumns.splice(index + 1, 1)
        this.form.modelColumns.splice(index, 0, data)
      }
    },
    delRow(index) {
      this.form.modelColumns.splice(index, 1)
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          updateDataModel(this.form).then(response => {
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
