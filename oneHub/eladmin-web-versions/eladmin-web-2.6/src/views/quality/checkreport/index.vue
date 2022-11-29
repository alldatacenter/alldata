<template>
  <div class="app-container">
    <el-card class="box-card" shadow="always">
      <div slot="header" class="clearfix">
        <el-form ref="queryForm" :model="queryParams" :inline="true" class="form-inline">
          <el-form-item label="时间">
            <el-date-picker
              v-model="queryParams.checkDate"
              type="date"
              :clearable="false"
              format="yyyy-MM-dd"
              value-format="yyyy-MM-dd"
              :picker-options="pickerOption"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="body-wrapper">
        <h4 style="text-align: center;">{{date}}质量分析报告</h4>
        <el-divider content-position="left"><h3>错误量统计分析</h3></el-divider>
        <el-row :gutter="20">
          <el-col :span="10">
            <h5>按数据源统计错误数量</h5>
            <el-table
              :data="reportTableData1"
              :span-method="objectSpanMethod1"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleLevelName" label="规则级别" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center">
                <template scope="scope">
                  <el-progress :percentage="scope.row.checkErrorCount" :stroke-width="20" :text-inside="true" :color="colorFormat(scope.row.ruleLevelName)" :format="cellFormat" />
                </template>
              </el-table-column>
            </el-table>
          </el-col>
          <el-col :span="10" :offset="4">
            <h5>按规则类型统计错误数量</h5>
            <el-table
              :data="reportTableData2"
              :span-method="objectSpanMethod2"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleTypeName" label="规则类型" align="center" />
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center">
                <template scope="scope">
                  <el-badge :value="scope.row.ruleLevelName" :type="typeFormat(scope.row.ruleLevelName)">
                    {{ scope.row.checkErrorCount }}
                  </el-badge>
                </template>
              </el-table-column>
            </el-table>
          </el-col>
        </el-row>
        <el-divider content-position="left"><h3>规则类型统计分析</h3></el-divider>
        <el-row>
          <el-col :span="24">
            <h5>唯一性分析</h5>
            <el-table
              :data="uniqueTableData"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleTableName" label="数据表(中文)" align="center" />
              <el-table-column prop="ruleTableComment" label="数据表(英文)" align="center" />
              <el-table-column prop="ruleColumnName" label="核查字段(中文)" align="center" />
              <el-table-column prop="ruleColumnComment" label="核查字段(英文)" align="center" />
              <el-table-column prop="checkTotalCount" label="核查数" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center" />
            </el-table>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <h5>完整性分析</h5>
            <el-table
              :data="integrityTableData"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleTableName" label="数据表(中文)" align="center" />
              <el-table-column prop="ruleTableComment" label="数据表(英文)" align="center" />
              <el-table-column prop="ruleColumnName" label="核查字段(中文)" align="center" />
              <el-table-column prop="ruleColumnComment" label="核查字段(英文)" align="center" />
              <el-table-column prop="checkTotalCount" label="核查数" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center" />
            </el-table>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <h5>一致性分析</h5>
            <el-table
              :data="consistentTableData"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleTableName" label="数据表(中文)" align="center" />
              <el-table-column prop="ruleTableComment" label="数据表(英文)" align="center" />
              <el-table-column prop="ruleColumnName" label="核查字段(中文)" align="center" />
              <el-table-column prop="ruleColumnComment" label="核查字段(英文)" align="center" />
              <el-table-column prop="checkTotalCount" label="核查数" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center" />
            </el-table>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <h5>关联性分析</h5>
            <el-table
              :data="relevanceTableData"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleTableName" label="数据表(中文)" align="center" />
              <el-table-column prop="ruleTableComment" label="数据表(英文)" align="center" />
              <el-table-column prop="ruleColumnName" label="核查字段(中文)" align="center" />
              <el-table-column prop="ruleColumnComment" label="核查字段(英文)" align="center" />
              <el-table-column prop="checkTotalCount" label="核查数" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center" />
            </el-table>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <h5>及时性分析</h5>
            <el-table
              :data="timelinessTableData"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleTableName" label="数据表(中文)" align="center" />
              <el-table-column prop="ruleTableComment" label="数据表(英文)" align="center" />
              <el-table-column prop="ruleColumnName" label="核查字段(中文)" align="center" />
              <el-table-column prop="ruleColumnComment" label="核查字段(英文)" align="center" />
              <el-table-column prop="checkTotalCount" label="核查数" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center" />
            </el-table>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <h5>准确性分析</h5>
            <el-table
              :data="accuracyTableData"
              border
              tooltip-effect="dark"
              :max-height="250"
              style="width: 100%; margin: 15px 0;"
            >
              <el-table-column prop="ruleName" label="规则名称" align="center" />
              <el-table-column prop="ruleSourceName" label="数据源" align="center" />
              <el-table-column prop="ruleTableName" label="数据表(中文)" align="center" />
              <el-table-column prop="ruleTableComment" label="数据表(英文)" align="center" />
              <el-table-column prop="ruleColumnName" label="核查字段(中文)" align="center" />
              <el-table-column prop="ruleColumnComment" label="核查字段(英文)" align="center" />
              <el-table-column prop="checkTotalCount" label="核查数" align="center" />
              <el-table-column prop="checkErrorCount" label="不合规数" align="center" />
            </el-table>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getReportBySource, getReportByType, getReportDetail } from '@/api/quality/checkreport'
import moment from 'moment'

export default {
  name: 'CheckReport',
  data() {
    return {
      queryParams: {
        checkDate: moment(moment().add(-1, 'days').startOf('day').valueOf()).format('YYYY-MM-DD')
      },
      pickerOption: {
        disabledDate(date) {
          return date.getTime() > Date.now() - 24 * 60 * 60 * 1000
        }
      },
      date: moment().subtract('days', 1).format('YYYY年MM月DD日'),
      spanArr1: [],
      position1: 0,
      reportTableData1: [],
      spanArr2: [],
      position2: 0,
      reportTableData2: [],
      // 唯一性核查数据
      uniqueTableData: [],
      // 完整性核查数据
      integrityTableData: [],
      // 准确性核查数据
      accuracyTableData: [],
      // 一致性核查数据
      consistentTableData: [],
      // 关联性核查数据
      relevanceTableData: [],
      // 及时性核查数据
      timelinessTableData: []
    }
  },
  created() {
    this.getReportData1()
    this.getReportData2()
    this.getReportData3()
  },
  methods: {
    handleQuery() {
      this.getReportData1()
      this.getReportData2()
      this.getReportData3()
      this.date = moment(this.queryParams.checkDate).format('YYYY年MM月DD日')
    },
    getReportData1() {
      getReportBySource({ checkDate: this.queryParams.checkDate }).then(response => {
        if (response.success) {
          this.reportTableData1 = response.data
          this.rowspan1()
        }
      })
    },
    getReportData2() {
      getReportByType({ checkDate: this.queryParams.checkDate }).then(response => {
        if (response.success) {
          this.reportTableData2 = response.data
          this.rowspan2()
        }
      })
    },
    rowspan1() {
      this.reportTableData1.forEach((item, index) => {
        if (index === 0) {
          this.spanArr1.push(1)
          this.position1 = 0
        } else {
          if (this.reportTableData1[index].ruleSourceId === this.reportTableData1[index - 1].ruleSourceId) {
            this.spanArr1[this.position1] += 1
            this.spanArr1.push(0)
          } else {
            this.spanArr1.push(1)
            this.position1 = index
          }
        }
      })
    },
    objectSpanMethod1({ row, column, rowIndex, columnIndex }) {
      if (columnIndex === 0) {
        const _row = this.spanArr1[rowIndex]
        const _col = _row > 0 ? 1 : 0
        return {
          rowspan: _row,
          colspan: _col
        }
      }
    },
    rowspan2() {
      this.reportTableData2.forEach((item, index) => {
        if (index === 0) {
          this.spanArr2.push(1)
          this.position2 = 0
        } else {
          if (this.reportTableData2[index].ruleTypeId === this.reportTableData2[index - 1].ruleTypeId) {
            this.spanArr2[this.position2] += 1
            this.spanArr2.push(0)
          } else {
            this.spanArr2.push(1)
            this.position2 = index
          }
        }
      })
    },
    objectSpanMethod2({ row, column, rowIndex, columnIndex }) {
      if (columnIndex === 0) {
        const _row = this.spanArr2[rowIndex]
        const _col = _row > 0 ? 1 : 0
        return {
          rowspan: _row,
          colspan: _col
        }
      }
    },
    colorFormat(level) {
      if (level === '低') {
        return '#409eff'
      } else if (level === '中') {
        return '#e6a23c'
      } else {
        return '#f56c6c'
      }
    },
    cellFormat(percentage) {
      return `${percentage}`
    },
    typeFormat(level) {
      if (level === '低') {
        return 'primary'
      } else if (level === '中') {
        return 'warning'
      } else {
        return 'danger'
      }
    },
    getReportData3() {
      getReportDetail({ checkDate: this.queryParams.checkDate }).then(response => {
        if (response.success) {
          this.uniqueTableData = response.data.unique
          this.integrityTableData = response.data.integrity
          this.accuracyTableData = response.data.accuracy
          this.consistentTableData = response.data.consistent
          this.relevanceTableData = response.data.relevance
          this.timelinessTableData = response.data.timeliness
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
.form-inline {
  ::v-deep .el-form-item {
    margin-bottom: 0px;
  }
}
::v-deep .el-badge__content {
  margin-top: 10px;
  right: 0px;
}
::v-deep .el-table__header th {
  background-color: #f5f5f5 !important;
}
</style>
