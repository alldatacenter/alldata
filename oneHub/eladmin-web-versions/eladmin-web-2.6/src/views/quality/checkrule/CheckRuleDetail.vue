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
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" />
        </el-form-item>
        <el-form-item label="核查类型">
          <el-select v-model="form.ruleItemId">
            <el-option
              v-for="item in ruleItemOptions"
              :key="item.id"
              :label="item.itemExplain"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规则级别">
          <el-select v-model="form.ruleLevelId">
            <el-option
              v-for="item in ruleLevelOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源">
          <el-select v-model="form.ruleSourceId">
            <el-option
              v-for="source in sourceOptions"
              :key="source.id"
              :label="source.sourceName"
              :value="source.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据表">
          <el-select v-model="form.ruleTableId">
            <el-option
              v-for="table in tableOptions"
              :key="table.id"
              :label="table.tableName"
              :value="table.id">
              <span style="float: left">{{ table.tableName + '(' + table.tableComment + ')' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="核查字段">
          <el-select v-model="form.ruleColumnId">
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
              <el-select v-model="form.ruleConfig.consistent.gbTypeId" placeholder="请选择">
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
              <el-select v-model="form.ruleConfig.relevance.relatedTableId" placeholder="请选择">
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
              <el-select v-model="form.ruleConfig.relevance.relatedColumnId" placeholder="请选择">
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
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.id"
              :label="dict.itemText"
            >{{ dict.itemValue }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
    </div>
  </el-card>
</template>

<script>
import { listRuleLevel, listRuleItem, getCheckRule } from '@/api/quality/checkrule'
import { listDataSource } from '@/api/metadata/datasource'
import { listDataTable } from '@/api/metadata/datatable'
import { listDataColumn } from '@/api/metadata/datacolumn'
import { listDataDictType } from '@/api/standard/datadict'

export default {
  name: 'CheckRuleDetail',
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
      title: '核查规则详情',
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
      }) || []
      this.columnOptions = await listDataColumn({ sourceId: this.form.ruleSourceId, tableId: this.form.ruleTableId }).then(response => {
        if (response.success) {
          return response.data
        }
      }) || []
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
