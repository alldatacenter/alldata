<template>
  <div class="app-container">
    <el-card class="box-card" shadow="always">
      <el-row>
        <el-col :span="24">
          <el-form ref="queryForm" :model="queryParams" :inline="true" class="demo-form-inline">
            <el-form-item label="数据表名">
              <el-input
                v-model="queryParams.tableName"
                placeholder="请输入数据表名"
                clearable
                size="small"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-table
            :data="tableDataList"
            stripe
            border
            :max-height="200"
            style="width: 100%; margin: 15px 0;"
          >
            <el-table-column prop="subjectArea" label="数据主题域" align="center" show-overflow-tooltip />
            <el-table-column prop="mappingName" label="映射名称" align="center" show-overflow-tooltip />
            <el-table-column prop="sourceTable" label="源表" align="center" show-overflow-tooltip />
            <el-table-column prop="targetTable" label="目标表" align="center" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <div id="chart" style="width: 100%; height: 300px;" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import echarts from 'echarts'

export default {
  name: 'DataBlood',
  data: function() {
    return {
      queryParams: {
        tableName: ''
      },
      chart: null,
      tableDataList: []
    }
  },
  created() {
  },
  mounted() {
    this.chart = echarts.init(document.getElementById('chart'))
  },
  beforeDestroy() {
    if (!this.chart) {
      return false
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    handleQuery() {
      this.tableDataList = [
        { subjectArea: 'DataCenter', mappingName: 'm_ts_test_table_inc', sourceTable: 'src_test_table', targetTable: 'ts_test_table' },
        { subjectArea: 'DataCenter', mappingName: 'm_ts_test_table_inc', sourceTable: 'ts_test_table', targetTable: 'th_test_table' },
        { subjectArea: 'DataCenter', mappingName: 'm_ts_test_table_inc', sourceTable: 'ts_test_table', targetTable: 'ti_test_table' },
        { subjectArea: 'DataCenter', mappingName: 'm_ods_test_table_inc', sourceTable: 'ti_test_table', targetTable: 't_test_table' }
      ]
      let data = { nodes: [], links: [] }
      const nodes = []
      const links = []
      const colors = ['#fbb4ae', '#b3cde3', '#ccebc5', '#decbe4']
      this.tableDataList.forEach(item => {
        nodes.push({
          name: item.sourceTable,
          itemStyle: {
            normal: {
              color: colors[Math.floor(Math.random() * colors.length)]
            }
          }
        })
        nodes.push({
          name: item.targetTable,
          itemStyle: {
            normal: {
              color: colors[Math.floor(Math.random() * colors.length)]
            }
          }
        })
        links.push({
          source: item.sourceTable,
          target: item.targetTable,
          value: item.mappingName.length,
          mapping: item.mappingName
        })
      })
      // nodes数组去重
      const res = new Map()
      const nodes_uniq = nodes.filter((node) => !res.has(node.name) && res.set(node.name, 1))
      data = {
        nodes: nodes_uniq,
        links: links
      }
      this.chart.clear()
      this.chart.setOption({
        title: {
          text: '血缘流向'
        },
        tooltip: {
          trigger: 'item',
          triggerOn: 'mousemove',
          formatter: function(x) {
            return x.data.mapping
          }
        },
        animation: false,
        series: [
          {
            type: 'sankey',
            focusNodeAdjacency: 'allEdges',
            nodeAlign: 'left',
            data: data.nodes,
            links: data.links,
            lineStyle: {
              color: 'source',
              curveness: 0.5
            }
          }
        ]
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
</style>
