<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-service-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-service-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-service-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-service-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataServiceList from './DataServiceList'
import DataServiceAdd from './DataServiceAdd'
import DataServiceEdit from './DataServiceEdit'
import DataServiceDetail from './DataServiceDetail'

export default {
  name: 'DataSource',
  components: { DataServiceList, DataServiceAdd, DataServiceEdit, DataServiceDetail },
  data() {
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
    showCard(data) {
      Object.assign(this.options, data)
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
