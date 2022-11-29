<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-source-list v-if="options.showList" @showCard="showCard"></data-source-list>
    </transition>
    <transition name="el-zoom-in-top">
      <data-source-add v-if="options.showAdd" :data="options.data" @showCard="showCard"></data-source-add>
    </transition>
    <transition name="el-zoom-in-top">
      <data-source-edit v-if="options.showEdit" :data="options.data" @showCard="showCard"></data-source-edit>
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-source-detail v-if="options.showDetail" :data="options.data" @showCard="showCard"></data-source-detail>
    </transition>
  </div>
</template>

<script>
import DataSourceList from './DataSourceList'
import DataSourceAdd from './DataSourceAdd'
import DataSourceEdit from './DataSourceEdit'
import DataSourceDetail from './DataSourceDetail'

export default {
  name: 'DataSource',
  components: { DataSourceList, DataSourceAdd, DataSourceEdit, DataSourceDetail },
  data () {
    return {
      options: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
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
