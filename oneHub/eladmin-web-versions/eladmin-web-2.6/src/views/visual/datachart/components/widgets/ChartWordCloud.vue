<template>
  <div ref="chart" :style="chartStyle">ChartWordcloud</div>
</template>

<script>
import echarts from 'echarts'
require('echarts-wordcloud')
import { convertPathToMap, SEPARATOR } from '@/utils/visual-chart'

export default {
  name: 'ChartWordcloud',
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
        tooltip: { trigger: 'item' }
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
      const _rowPaths = this._combineRowPaths(
        this.localData,
        ...this.localRows.map(({ key, values }) => { return { key, values } })
      )
      const _rowKeys = this.localRows.map(({ key }) => key)
      const _colPaths = this._combineColPaths(
        ...this.localColumns.map(({ values }) => values)
      )
      const _colKeys = this.localColumns.map(({ key }) => key)
      // 行对应的条件
      const rowConditions = convertPathToMap(_rowPaths, _rowKeys)
      // 列对应的条件
      const colConditions = convertPathToMap(_colPaths, _colKeys)
      // 针对没传入行或列的处理
      !colConditions.length && colConditions.push({})
      !rowConditions.length && rowConditions.push({})
      const seriesObj = {}
      colConditions.forEach((colCondition, colConditionIndex) => {
        const isEmptyCol = !Object.keys(colCondition).length
        this.localValues.forEach(({ key }, index) => {
          const seriesName = isEmptyCol ? key : Object.values(colCondition).join(this.connector) + this.connector + key
          seriesObj[seriesName] = {
            name: seriesName,
            type: this.chartType,
            drawOutOfBound: true,
            data: []
          }
        })
      })
      // 计算每个series数据
      colConditions.forEach((colCondition, colConditionIndex) => {
        const isEmptyCol = !Object.keys(colCondition).length
        rowConditions.forEach((rowCondition, rowConditionIndex) => {
          // 当前单元对应的条件
          const conditions = Object.assign({}, rowCondition, colCondition)
          // 通过当前单元对应的条件，过滤数据
          const filterData = this._filterData(conditions, this.localData)
          // 多个值，多条数据
          this.localValues.forEach(({ key }) => {
            const value = this._reduceValue(filterData, key)
            const seriesName = isEmptyCol ? key : Object.values(colCondition).join(this.connector) + this.connector + key
            seriesObj[seriesName].data.push(
              {
                name: Object.values(rowCondition).join(this.connector),
                value: value
              }
            )
          })
        })
      })
      this.calcData.seriesObj = seriesObj
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
        option.series = series
        this.chart.clear()
        this.chart.setOption(option, true)
      }
    },
    _combineRowPaths(data, ...arrays) {
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
              const filter = data.some((data) => {
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
