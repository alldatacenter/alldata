<template>
  <div ref="chart" :style="chartStyle" style="overflow: auto;">
    <pivot-table
      ref="pivottable"
      :data="data"
      :rows="rows"
      :columns="columns"
      :values="values"
    />
  </div>
</template>

<script>
import PivotTable from '@/components/PivotTable'

export default {
  name: 'ChartTable',
  components: {
    PivotTable
  },
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
    chartStyle: {
      type: Object,
      require: false,
      default: () => {
        return {
          height: '200px'
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
