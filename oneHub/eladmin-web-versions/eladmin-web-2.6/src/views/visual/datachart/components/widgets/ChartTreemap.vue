<template>
  <div ref="chart" :style="chartStyle">ChartTreemap</div>
</template>

<script>
import echarts from 'echarts'
import { convertPathToMap, SEPARATOR } from '@/utils/visual-chart'

export default {
  name: 'ChartTreemap',
  props: {
    data: {
      type: Array,
      required: true,
      default: () => []
    },
    rows: {
      type: Array,
      required: true,
      default: () => []
    },
    columns: {
      type: Array,
      required: true,
      default: () => []
    },
    values: {
      type: Array,
      required: true,
      default: () => []
    },
    chartType: {
      type: String,
      required: true
    },
    chartTheme: {
      type: String,
      require: true,
      default: 'default'
    },
    chartOption: {
      type: Object,
      require: false,
      default: () => ({})
    },
    chartStyle: {
      type: Object,
      require: false,
      default: () => {
        return {
          height: '200px'
        }
      }
    }
  },
  data() {
    return {
      localRows: [],
      localColumns: [],
      localValues: [],
      localData: [],
      // 连接符
      connector: '-',
      chart: null,
      calcOption: {
        tooltip: {
          trigger: 'item',
          formatter: '{b} : {c}'
        }
      },
      calcData: {
        seriesObj: {}
      }
    }
  },
  computed: {
    watchAllProps() {
      const { rows, columns, values, data } = this
      return { rows, columns, values, data }
    }
  },
  watch: {
    watchAllProps() {
      this.init()
      this.mergeChartOption()
    },
    chartTheme() {
      this.mergeChartTheme()
    },
    chartOption: {
      handler(newValue, oldValue) {
        this.mergeChartOption()
      },
      deep: true
    }
  },
  mounted() {
    this.renderChart()
    this.$on('resized', this.handleResize)
    window.addEventListener('resize', this.handleResize)
  },
  created() {
    this.init()
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
    }
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    init() {
      if (this.rows.length || this.columns.length || this.values.length) {
        this.handleDataClone()
        this.setValuesToColAndRow()
        this.handleCalcData()
      } else {
        console.warn('[Warn]: props.rows, props.columns, props.values at least one is not empty.')
      }
    },
    // clone data
    handleDataClone() {
      this.localRows = JSON.parse(JSON.stringify(this.rows))
      this.localColumns = JSON.parse(JSON.stringify(this.columns))
      this.localValues = JSON.parse(JSON.stringify(this.values))
      this.localData = Object.freeze(this.data)
    },
    // set the `values` attribute to rows and columns
    setValuesToColAndRow() {
      const rowKeys = this.localRows.map(({ key }) => key)
      const columnKeys = this.localColumns.map(({ key }) => key)
      const rowValues = this._findCategory(rowKeys, this.localData)
      const columnValues = this._findCategory(columnKeys, this.localData)
      this.localRows.forEach((row) => {
        const { key, values } = row
        this.$set(row, 'values', values || rowValues[key] || [])
      })
      this.localColumns.forEach((column) => {
        const { key, values } = column
        this.$set(column, 'values', values || columnValues[key] || [])
      })
    },
    // 计算值
    handleCalcData() {
      if (!this.localRows.length || !this.localValues.length) return
      const seriesData = this.recursion(0, {}, this.localRows, this.localValues, this.localData)
      const seriesObj = {}
      const seriesName = 'treemap'
      seriesObj[seriesName] = {
        name: seriesName,
        type: this.chartType,
        leafDepth: 1,
        visibleMin: 1,
        label: { position: 'insideTopLeft' },
        data: seriesData
      }
      this.calcData.seriesObj = seriesObj
    },
    // 递归
    recursion(depth, conditions = {}, rows = [], values = [], data = []) {
      const _result = []
      if (depth < rows.length) {
        const row = rows[depth]
        row.values.forEach((val) => {
          conditions[row.key] = val
          const filters = Object.keys(conditions)
            .filter((key, index) => index <= depth)
            .reduce((obj, key) => {
              obj[key] = conditions[key]
              return obj
            }, {})
          const filterData = data.filter((item) => {
            let status = true
            for (const key in filters) {
              if (filters[key] !== item[key]) {
                status = false
                return
              }
            }
            return status
          })
          if (filterData.length) {
            if (depth === (rows.length - 1)) {
              values.forEach(({ key }) => {
                const value = this._reduceValue(filterData, key)
                _result.push({
                  name: val,
                  value: value
                })
              })
            } else {
              _result.push({
                name: val,
                children: this.recursion(depth + 1, conditions, rows, values, filterData)
              })
            }
          }
        })
      }
      return _result
    },
    handleResize() {
      if (this.chart) {
        this.chart.resize()
      }
    },
    renderChart() {
      if (!this.$refs.chart) return
      let option = Object.assign({}, this.chartOption, this.calcOption)
      option = JSON.parse(JSON.stringify(option))
      const series = JSON.parse(JSON.stringify(Object.values(this.calcData.seriesObj)))
      this.parseChartSeries(option, series, this.chartSeriesType)
      option.series = series
      setTimeout(() => {
        if (!this.chart) {
          if (this.chartTheme !== 'default') {
            require('./themes/' + this.chartTheme + '.js')
          }
          this.chart = echarts.init(this.$refs.chart, this.chartTheme)
        }
        this.chart.clear()
        this.chart.setOption(option)
      }, 0)
    },
    mergeChartTheme() {
      if (!this.$refs.chart) return
      if (this.chart) {
        // 使用刚指定的配置项和数据显示图表
        let option = Object.assign({}, this.chartOption, this.calcOption)
        option = JSON.parse(JSON.stringify(option))
        const series = JSON.parse(JSON.stringify(Object.values(this.calcData.seriesObj)))
        this.parseChartSeries(option, series, this.chartSeriesType)
        option.series = series
        if (this.chartTheme !== 'default') {
          require('./themes/' + this.chartTheme + '.js')
        }
        this.chart.dispose()
        // 基于准备好的dom，初始化echarts实例
        this.chart = echarts.init(this.$refs.chart, this.chartTheme)
        this.chart.setOption(option)
      }
    },
    mergeChartOption() {
      if (!this.$refs.chart) return
      if (this.chart) {
        let option = Object.assign({}, this.chartOption, this.calcOption)
        option = JSON.parse(JSON.stringify(option))
        const series = JSON.parse(JSON.stringify(Object.values(this.calcData.seriesObj)))
        this.parseChartSeries(option, series, this.chartSeriesType)
        option.series = series
        this.chart.clear()
        this.chart.setOption(option, true)
      }
    },
    parseChartSeries(option, series, type) {
      series.forEach((item) => {
        item.label.formatter = (params) => {
          return params.name + '\n\nvalue : ' + params.value
        }
      })
    },
    _combineRowPaths(...arrays) {
      const len = arrays.length
      let _result = []
      if (len) {
        const rowPaths = arrays.reduce((prev, curr) => {
          const arr = []
          prev.values.forEach(_prevEl => {
            const prevKey = prev.key.split(SEPARATOR)
            curr.values.forEach(_currEl => {
              const currKey = curr.key
              const conditions = {}
              prevKey.forEach((key, i) => {
                conditions[key] = _prevEl.split(SEPARATOR)[i]
              })
              conditions[currKey] = _currEl
              // 判断数据里是否有该项
              const filter = this.localData.some((data) => {
                let status = true
                for (const key in conditions) {
                  if (conditions[key] !== data[key]) {
                    status = false
                    return
                  }
                }
                return status
              })
              if (filter) {
                arr.push(_prevEl + SEPARATOR + _currEl)
              }
            })
          })
          return { key: prev.key + SEPARATOR + curr.key, values: arr }
        }) || {}
        _result = rowPaths.values || []
      }
      return _result
    },
    _combineColPaths(...arrays) {
      return arrays.length ? arrays.reduce((prev, curr) => {
        const arr = []
        prev.forEach(_prevEl => {
          curr.forEach(_currEl => {
            arr.push(_prevEl + SEPARATOR + _currEl)
          })
        })
        return arr
      }) : arrays
    },
    _findCategory(keys = [], data = []) {
      const _result = {}
      data.forEach(item => {
        keys.forEach(key => {
          // Remove duplicates
          _result[key] = _result[key] || []
          _result[key].push(item[key])
          _result[key] = [...new Set(_result[key])]
        })
      })
      return _result
    },
    _reduceValue(data, key) {
      if (!data.length) return 0
      return data.reduce((sum, item) => { return sum + Number(item[key]) }, 0)
    },
    _filterData(conditions, data) {
      return data.filter((data) => {
        let status = true
        for (const key in conditions) {
          if (conditions[key] !== data[key]) {
            status = false
            return
          }
        }
        return status
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
