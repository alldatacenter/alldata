<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-chart-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataChartList from './DataChartList'

export default {
  name: 'DataChart',
  components: { DataChartList },
  data() {
    return {
      options: {
        data: {},
        showList: true
      }
    }
  },
  methods: {
    showCard(data) {
      Object.assign(this.options, data)
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
