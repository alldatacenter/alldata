<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-set-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-set-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-set-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-set-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataSetList from './DataSetList'
import DataSetAdd from './DataSetAdd'
import DataSetEdit from './DataSetEdit'
import DataSetDetail from './DataSetDetail'

export default {
  name: 'DataSet',
  components: { DataSetList, DataSetAdd, DataSetEdit, DataSetDetail },
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
