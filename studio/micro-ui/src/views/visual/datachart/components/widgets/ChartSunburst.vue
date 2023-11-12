<template>
  <div ref="chart" :style="chartStyle">ChartSunburst</div>
</template>

<script>
export default {
  name: 'ChartSunburst',
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
      require: true
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
  created() {
    console.log(this.data)
  }
}
</script>

<style lang="scss" scoped>

</style>
