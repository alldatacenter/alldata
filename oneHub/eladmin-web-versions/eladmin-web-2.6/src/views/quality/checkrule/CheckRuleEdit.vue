<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['quality:rule:edit']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="核查类型" prop="ruleItemId">
          <el-select v-model="form.ruleItemId" placeholder="请选择核查类型" @change="ruleItemSelectChanged">
            <el-option
              v-for="item in ruleItemOptions"
              :key="item.id"
              :label="item.itemExplain"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规则级别" prop="ruleLevelId">
          <el-select v-model="form.ruleLevelId" placeholder="请选择规则级别">
            <el-option
              v-for="item in ruleLevelOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="ruleSourceId">
          <el-select v-model="form.ruleSourceId" placeholder="请选择数据源" @change="sourceSelectChanged">
            <el-option
              v-for="source in sourceOptions"
              :key="source.id"
              :label="source.sourceName"
              :value="source.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据表" prop="ruleTableId">
          <el-select v-model="form.ruleTableId" placeholder="请选择数据表" @change="tableSelectChanged">
            <el-option
              v-for="table in tableOptions"
              :key="table.id"
              :label="table.tableName"
              :value="table.id">
              <span style="float: left">{{ table.tableName + '(' + table.tableComment + ')' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="核查字段" prop="ruleColumnId">
          <el-select v-model="form.ruleColumnId" placeholder="请选择核查字段" @change="columnSelectChanged">
            <el-option
              v-for="column in columnOptions"
              :key="column.id"
              :label="column.columnName"
              :value="column.id">
              <span style="float: left">{{ column.columnName + '(' + column.columnComment + ')' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-divider content-position="left">核查配置</el-divider>
        <el-row v-if="form.ruleConfig.ruleItemCode === 'timeliness_key'">
          <el-col :span="24">
            <el-form-item label="判定阀值">
              <el-input-number v-model="form.ruleConfig.timeliness.threshold" :controls="false" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.ruleConfig.ruleItemCode === 'consistent_key'">
          <el-col :span="12">
            <el-form-item label="标准字典类别">
              <el-select v-model="form.ruleConfig.consistent.gbTypeId" placeholder="请选择" @change="dictTypeSelectChanged">
                <el-option
                  v-for="item in dictTypeOptions"
                  :key="item.id"
                  :label="item.gbTypeName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标准字典字段">
              <el-select v-model="form.ruleConfig.consistent.bindGbColumn" placeholder="请选择">
                <el-option
                  v-for="item in gbColumnOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.ruleConfig.ruleItemCode === 'relevance_key'">
          <el-col :span="12">
            <el-form-item label="关联表">
              <el-select v-model="form.ruleConfig.relevance.relatedTableId" placeholder="请选择" @change="relatedTableSelectChanged">
                <el-option
                  v-for="table in tableOptions"
                  :key="table.id"
                  :label="table.tableName"
                  :value="table.id"
                >
                  <span style="float: left">{{ table.tableName + '(' + table.tableComment + ')' }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联字段">
              <el-select v-model="form.ruleConfig.relevance.relatedColumnId" placeholder="请选择" @change="relatedColumnSelectChanged">
                <el-option
                  v-for="column in relatedColumnOptions"
                  :key="column.id"
                  :label="column.columnName"
                  :value="column.id"
                >
                  <span style="float: left">{{ column.columnName + '(' + column.columnComment + ')' }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.ruleConfig.ruleItemCode === 'accuracy_key_length'">
          <el-col :span="24">
            <el-form-item label="最大长度">
              <el-input-number v-model="form.ruleConfig.accuracy.maxLength" :controls="false" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { listRuleLevel, listRuleItem, getCheckRule, updateCheckRule } from '@/api/quality/checkrule'
import { listDataSource } from '@/api/metadata/datasource'
import { listDataTable } from '@/api/metadata/datatable'
import { listDataColumn } from '@/api/metadata/datacolumn'
import { listDataDictType } from '@/api/standard/datadict'

export default {
  name: 'CheckRuleEdit',
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
      title: '核查规则编辑',
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
        ruleName: [
          { required: true, message: '规则名称不能为空', trigger: 'blur' }
        ],
        ruleItemId: [
          { required: true, message: '核查类型不能为空', trigger: 'change' }
        ],
        ruleLevelId: [
          { required: true, message: '规则级别不能为空', trigger: 'change' }
        ],
        ruleSourceId: [
          { required: true, message: '数据源不能为空', trigger: 'change' }
        ],
        ruleTableId: [
          { required: true, message: '数据表不能为空', trigger: 'change' }
        ],
        ruleColumnId: [
          { required: true, message: '核查字段不能为空', trigger: 'change' }
        ]
      },
      // 状态数据字典
      statusOptions: [],
      // 规则级别数据字典
      ruleLevelOptions: [],
      // 核查类型数据字典
      ruleItemOptions: [],
      sourceOptions: [],
      tableOptions: [],
      columnOptions: [],
      dictTypeOptions: [],
      gbColumnOptions: [
        { value: 'gb_code', label: '标准编码' },
        { value: 'gb_name', label: '标准名称' }
      ],
      relatedColumnOptions: []
    }
  },
  created() {
    console.log('id:' + this.data.id)
    this.getDicts('sys_common_status').then(response => {
      if (response.success) {
        this.statusOptions = response.data
      }
    })
    listDataDictType().then(response => {
      if (response.success) {
        this.dictTypeOptions = response.data
      }
    })
    this.getRuleLevelList()
    this.getDataSourceList()
  },
  mounted() {
    this.getCheckRule(this.data.id)
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 获取详情 */
    async getCheckRule(id) {
      this.form = await getCheckRule(id).then(response => {
        if (response.success) {
          return response.data
        }
      })
      this.ruleItemOptions = await listRuleItem({ ruleTypeId: this.form.ruleTypeId }).then(response => {
        if (response.success) {
          return response.data
        }
      })
      this.tableOptions = await listDataTable({ sourceId: this.form.ruleSourceId }).then(response => {
        if (response.success) {
          return response.data
        }
      })
      this.columnOptions = await listDataColumn({ sourceId: this.form.ruleSourceId, tableId: this.form.ruleTableId }).then(response => {
        if (response.success) {
          return response.data
        }
      })
      if (this.form.ruleConfig.ruleItemCode === 'relevance_key') {
        listDataColumn({ sourceId: this.form.ruleSourceId, tableId: this.form.ruleConfig.relevance.relatedTableId }).then(response => {
          if (response.success) {
            this.relatedColumnOptions = response.data
          }
        })
      }
    },
    getRuleLevelList() {
      listRuleLevel().then(response => {
        if (response.success) {
          this.ruleLevelOptions = response.data
        }
      })
    },
    getDataSourceList() {
      listDataSource().then(response => {
        if (response.success) {
          this.sourceOptions = response.data
        }
      })
    },
    ruleItemSelectChanged(val) {
      const item = this.ruleItemOptions.find(function(item) {
        return item.id === val
      })
      this.form.ruleConfig.ruleItemCode = item.itemCode
    },
    sourceSelectChanged(val) {
      listDataTable({ sourceId: val }).then(response => {
        if (response.success) {
          this.tableOptions = response.data
          this.columnOptions = []
          const source = this.sourceOptions.find(function(item) {
            return item.id === val
          })
          this.form.ruleSource = source.sourceName
          this.form.ruleDbType = source.dbType
          this.form.ruleTableId = ''
          this.form.ruleTable = ''
          this.form.ruleTableComment = ''
          this.form.ruleColumnId = ''
          this.form.ruleColumn = ''
          this.form.ruleColumnComment = ''
        }
      })
    },
    tableSelectChanged(val) {
      listDataColumn({ sourceId: this.form.ruleSourceId, tableId: val }).then(response => {
        if (response.success) {
          this.columnOptions = response.data
          const table = this.tableOptions.find(function(item) {
            return item.id === val
          })
          this.form.ruleTable = table.tableName
          this.form.ruleTableComment = table.tableComment
          this.form.ruleColumnId = ''
          this.form.ruleColumn = ''
          this.form.ruleColumnComment = ''
        }
      })
    },
    columnSelectChanged(val) {
      const column = this.columnOptions.find(function(item) {
        return item.id === val
      })
      this.form.ruleColumn = column.columnName
      this.form.ruleColumnComment = column.columnComment
      this.$forceUpdate()
    },
    dictTypeSelectChanged(val) {
      const item = this.dictTypeOptions.find(function(item) {
        return item.id === val
      })
      this.form.ruleConfig.consistent.gbTypeCode = item.gbTypeCode
      this.form.ruleConfig.consistent.gbTypeName = item.gbTypeName
    },
    relatedTableSelectChanged(val) {
      listDataColumn({ sourceId: this.form.ruleSourceId, tableId: val }).then(response => {
        if (response.success) {
          this.relatedColumnOptions = response.data
          const table = this.tableOptions.find(function(item) {
            return item.id === val
          })
          this.form.ruleConfig.relevance.relatedTable = table.tableName
          this.form.ruleConfig.relevance.relatedTableComment = table.tableComment
          this.form.ruleConfig.relevance.relatedColumnId = ''
          this.form.ruleConfig.relevance.relatedColumn = ''
          this.form.ruleConfig.relevance.relatedColumnComment = ''
        }
      })
    },
    relatedColumnSelectChanged(val) {
      const column = this.relatedColumnOptions.find(function(item) {
        return item.id === val
      })
      this.form.ruleConfig.relevance.relatedColumn = column.columnName
      this.form.ruleConfig.relevance.relatedColumnComment = column.columnComment
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          updateCheckRule(this.form).then(response => {
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
