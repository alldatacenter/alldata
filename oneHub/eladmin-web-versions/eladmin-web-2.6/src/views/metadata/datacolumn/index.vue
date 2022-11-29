<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-column-list v-if="options.showList" @showCard="showCard"></data-column-list>
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-column-detail v-if="options.showDetail" :data="options.data" @showCard="showCard"></data-column-detail>
    </transition>
  </div>
</template>

<script>
import DataColumnList from './DataColumnList'
import DataColumnDetail from './DataColumnDetail'

export default {
  name: 'DataColumn',
  components: { DataColumnList, DataColumnDetail },
  data () {
    return {
      options: {
        data: {},
        showList: true,
        showDetail: false
      }
    }
  },
  methods: {
    showCard (data) {
      Object.assign(this.options, data)
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
