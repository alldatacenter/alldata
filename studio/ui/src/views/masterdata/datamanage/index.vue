<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataList from './DataList'
import DataAdd from './DataAdd'
import DataEdit from './DataEdit'
import DataDetail from './DataDetail'

export default {
  name: 'DataManage',
  components: { DataList, DataAdd, DataEdit, DataDetail },
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
