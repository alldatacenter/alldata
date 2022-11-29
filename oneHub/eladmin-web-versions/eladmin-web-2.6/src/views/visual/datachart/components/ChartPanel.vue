<template>
  <component
    :is="currentChart.component"
    :data="chartData"
    :rows="rows"
    :columns="columns"
    :values="values"
    :chart-type="currentChart.value"
    :chart-theme="chartTheme"
    :chart-option="chartOption"
    :chart-series-type="chartSeriesType"
    :chart-style="chartStyle"
  />
</template>

<script>
import { chartTypes } from '@/utils/visual-chart'
import ChartTable from './widgets/ChartTable'
import ChartLine from './widgets/ChartLine'
import ChartBar from './widgets/ChartBar'
import ChartPie from './widgets/ChartPie'
import ChartKpi from './widgets/ChartKpi'
import ChartRadar from './widgets/ChartRadar'
import ChartFunnel from './widgets/ChartFunnel'
import ChartScatter from './widgets/ChartScatter'
import ChartGauge from './widgets/ChartGauge'
import ChartTreemap from './widgets/ChartTreemap'
import ChartWordCloud from './widgets/ChartWordCloud'
import ChartLiquidFill from './widgets/ChartLiquidFill'
import ChartSankey from './widgets/ChartSankey'
import ChartMap from './widgets/ChartMap'
import ChartTree from './widgets/ChartTree'
import ChartSunburst from './widgets/ChartSunburst'
import ChartPolar from './widgets/ChartPolar'

export default {
  name: 'ChartPanel',
  components: {
    ChartTable, ChartLine, ChartBar, ChartPie,
    ChartKpi, ChartRadar, ChartFunnel, ChartScatter,
    ChartGauge, ChartTreemap, ChartWordCloud, ChartLiquidFill,
    ChartSankey, ChartMap, ChartTree, ChartSunburst, ChartPolar
  },
  props: {
    chartSchema: {
      type: Object,
      required: true
    },
    chartData: {
      type: Array,
      required: true
    },
    chartStyle: {
      type: Object,
      require: false
    }
  },
  data() {
    return {
      chartTypes
    }
  },
  computed: {
    currentChart() {
      return chartTypes.find(item => item.value === this.chartSchema.chartType)
    },
    rows() {
      return this.chartSchema.rows.map((row, index, arr) => {
        return {
          key: `${row.col}`,
          label: `${row.col}`
        }
      }) || []
    },
    columns() {
      return this.chartSchema.columns.map((column, index, arr) => {
        return {
          key: `${column.col}`,
          label: `${column.col}`
        }
      }) || []
    },
    values() {
      return this.chartSchema.measures.map((measure, index, arr) => {
        return {
          key: `${measure.col}`,
          label: `${measure.col}`
        }
      }) || []
    },
    chartTheme() {
      return this.chartSchema.theme || 'default'
    },
    chartSeriesType() {
      return this.chartSchema.seriesType || undefined
    },
    chartOption() {
      return this.chartSchema.options || {}
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
