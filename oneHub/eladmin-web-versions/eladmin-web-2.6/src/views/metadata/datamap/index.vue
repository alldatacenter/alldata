<template>
  <div class="app-container">
    <el-card class="box-card" shadow="always">
      <div class="body-wrapper">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="数据库">
            <el-select v-model="searchForm.sourceId" placeholder="数据库">
              <el-option
                v-for="item in sourceOptions"
                :key="item.id"
                :label="item.sourceName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="数据表">
            <el-select v-model="searchForm.tableId" clearable placeholder="数据表">
              <el-option
                v-for="item in tableOptions"
                :key="item.id"
                :label="item.tableComment ? item.tableComment : item.tableName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="btnEnable" @click="onSubmit">查询</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
        <div id="chart" :style="{width: '100%', height: 'calc(100vh - 300px)'}" />
      </div>
    </el-card>
  </div>
</template>

<script>
import { listDataSource } from '@/api/metadata/datasource'
import { listDataTable } from '@/api/metadata/datatable'
import { getDataMetadataTree } from '@/api/metadata/datacolumn'
import echarts from 'echarts'

export default {
  name: 'DataMap',
  data: function() {
    return {
      searchForm: {
        sourceId: '',
        tableId: ''
      },
      btnEnable: true,
      sourceOptions: [],
      tableOptions: [],
      treeData: [],
      chart: null,
      chartList: [],
      chartLinks: [],
      chartLegend: [],
      colors: [{
        c1: '#00c7ef',
        c2: '#0AF3FF'
      },
      {
        c1: '#FF8E14',
        c2: '#FFA12F'
      },
      {
        c1: '#AF5AFF',
        c2: '#B62AFF'
      },
      {
        c1: '#25dd59',
        c2: '#29f463'
      },
      {
        c1: '#6E35FF',
        c2: '#6E67FF'
      },
      {
        c1: '#002AFF',
        c2: '#0048FF'
      },
      {
        c1: '#8CD282',
        c2: '#95F300'
      },
      {
        c1: '#3B0EFF',
        c2: '#604BFF'
      },
      {
        c1: '#00BE74',
        c2: '#04FDB8'
      },
      {
        c1: '#4a3ac6',
        c2: '#604BFF'
      }
      ]
    }
  },
  watch: {
    'searchForm.sourceId': {
      immediate: true,
      // handler:是一个回调函数，即监听到变化应该执行的函数
      handler(value) {
        if (value) {
          // 清空数据
          this.searchForm.tableId = ''
          this.getDataTableList(value)
          this.btnEnable = false
        }
      }
    }
  },
  created() {
    this.getDataSourceList()
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
    getDataSourceList() {
      listDataSource().then(response => {
        if (response.success) {
          this.sourceOptions = response.data
        }
      })
    },
    getDataTableList(sourceId) {
      const data = {}
      data.sourceId = sourceId
      listDataTable(data).then(response => {
        if (response.success) {
          this.tableOptions = response.data
        }
      })
    },
    onSubmit() {
      getDataMetadataTree('column', { sourceId: this.searchForm.sourceId, tableId: this.searchForm.tableId }).then(response => {
        if (response.success) {
          const { data } = response
          this.treeData = data
          this.initChart()
        }
      })
    },
    handleList(arr, idx, color, category) {
      arr.forEach((item, index) => {
        if (item.label === null) {
          return false
        }
        // 设置节点大小
        let symbolSize = 10
        switch (idx) {
          case 0:
            symbolSize = 70
            break
          case 1:
            symbolSize = 50
            break
          default:
            symbolSize = 10
            break
        }
        // 每个节点所对应的文本标签的样式
        let label = null
        switch (idx) {
          case 0:
          case 1:
            label = {
              position: 'inside',
              rotate: 0
            }
            break
          default:
            break
        }
        // 计算出颜色,从第二级开始
        if (idx === 0) {
          color = this.colors[0]
        }
        if (idx === 1) {
          color = this.colors.find((itemm, eq) => eq === index % 10)
          this.chartLegend.push(item.label)
        }
        // 设置线条颜色
        const lineStyle = {
          color: color.c2
        }
        // 设置节点样式
        let bgcolor = null
        if (idx === 0) {
          bgcolor = {
            type: 'radial',
            x: 0.5,
            y: 0.5,
            r: 0.5,
            colorStops: [{
              offset: 0,
              color: color.c1 // 0% 处的颜色
            },
            {
              offset: 0.8,
              color: color.c1 // 80% 处的颜色
            },
            {
              offset: 1,
              color: 'rgba(0, 0, 0, 0.3)' // 100% 处的颜色
            }
            ],
            global: false
          }
        } else {
          bgcolor = {
            type: 'radial',
            x: 0.5,
            y: 0.5,
            r: 0.5,
            colorStops: [{
              offset: 0,
              // 0% 处的颜色
              color: color.c1
            },
            {
              offset: 0.4,
              // 0% 处的颜色
              color: color.c1
            },
            {
              offset: 1,
              // 100% 处的颜色
              color: color.c2
            }
            ],
            global: false
          }
        }
        let itemStyle = null
        if (item.children && item.children.length !== 0) {
          // 非子节点
          itemStyle = {
            borderColor: color.c2,
            color: bgcolor
          }
        } else {
          // 子节点
          item.isEnd = true
          if (item.isdisease === 'true') {
            itemStyle = {
              color: color.c2,
              borderColor: color.c2
            }
          } else {
            itemStyle = {
              color: 'transparent',
              borderColor: color.c2
            }
          }
        }
        // 可以改变来实现节点发光效果，但体验不好
        itemStyle = Object.assign(itemStyle, {
          shadowColor: 'rgba(255, 255, 255, 0.5)',
          shadowBlur: 10
        })

        if (idx === 1) {
          category = item.label
        }
        let obj = {
          name: item.label,
          symbolSize: symbolSize,
          category: category,
          label,
          color: bgcolor,
          itemStyle,
          lineStyle
        }
        obj = Object.assign(item, obj)
        if (idx === 0) {
          obj = Object.assign(obj, {
            root: true
          })
        }
        if (item.children && item.children.length === 0) {
          obj = Object.assign(obj, {
            isEnd: true
          })
        }
        this.chartList.push(obj)
        if (item.children && item.children.length > 0) {
          this.handleList(item.children, idx + 1, color, category)
        }
      })
    },
    handleLink(arr, idx, color) {
      arr.forEach(item => {
        if (item.children) {
          item.children.forEach((item2, eq) => {
            if (idx === 0) {
              color = this.colors.find((itemm, eq2) => eq2 === eq % 10)
            }
            let lineStyle = null
            switch (idx) {
              case 0:
                if (item2.children.length > 0) {
                  lineStyle = {
                    normal: {
                      color: 'target'
                    }
                  }
                } else {
                  lineStyle = {
                    normal: {
                      color: color.c2
                    }
                  }
                }
                break
              default:
                lineStyle = {
                  normal: {
                    color: 'source'
                  }
                }
                break
            }
            const obj = {
              source: item.id,
              target: item2.id,
              lineStyle
            }
            this.chartLinks.push(obj)
            if (item2.children && item.children.length > 0) {
              this.handleLink(item.children, idx + 1)
            }
          })
        }
      })
    },
    initChart() {
      this.chart.showLoading()
      this.chartList = []
      this.chartLinks = []
      this.chartLegend = []
      // 获取表名
      const categories = this.treeData[0].children.map(item => {
        return {
          name: item.label
        }
      })
      // 计算list
      this.handleList(JSON.parse(JSON.stringify(this.treeData)), 0)
      // 计算links
      this.handleLink(JSON.parse(JSON.stringify(this.treeData)), 0)
      // 绘制图表
      const option = {
        backgroundColor: '#000',
        toolbox: {
          show: true,
          left: 'right',
          right: 20,
          top: 'bottom',
          bottom: 20
        },
        legend: {
          show: true,
          data: this.chartLegend,
          textStyle: {
            color: '#fff',
            fontSize: 10
          },
          icon: 'circle',
          type: 'scroll',
          orient: 'vertical',
          left: 'right',
          right: 20,
          top: 20,
          bottom: 80,
          pageIconColor: '#00f6ff',
          pageIconInactiveColor: '#fff',
          pageIconSize: 12,
          pageTextStyle: {
            color: '#fff',
            fontSize: 12
          }
        },
        selectedMode: 'false',
        bottom: 20,
        left: 0,
        right: 0,
        top: 0,
        animationDuration: 1500,
        animationEasingUpdate: 'quinticInOut',
        series: [{
          name: '数据地图',
          type: 'graph',
          hoverAnimation: true,
          layout: 'force',
          force: {
            repulsion: 300,
            edgeLength: 100
          },
          nodeScaleRatio: 0.6,
          draggable: true,
          roam: true,
          symbol: 'circle',
          data: this.chartList,
          links: this.chartLinks,
          categories: categories,
          focusNodeAdjacency: true,
          scaleLimit: {
            // 所属组件的z分层，z值小的图形会被z值大的图形覆盖
            // 最小的缩放值
            min: 0.5,
            // 最大的缩放值
            max: 9
          },
          edgeSymbol: ['circle', 'arrow'],
          edgeSymbolSize: [4, 8],
          label: {
            normal: {
              show: true,
              position: 'right',
              color: '#fff',
              distance: 5,
              fontSize: 10
            }
          },
          lineStyle: {
            normal: {
              width: 1.5,
              curveness: 0,
              type: 'solid'
            }
          }
        }]
      }
      this.chart.setOption(option)
      this.chart.hideLoading()
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
::v-deep .el-divider {
  margin: 0;
}
</style>
