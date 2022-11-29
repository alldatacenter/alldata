<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-model-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-model-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-model-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-model-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataModelList from './DataModelList'
import DataModelAdd from './DataModelAdd'
import DataModelEdit from './DataModelEdit'
import DataModelDetail from './DataModelDetail'

export default {
  name: 'DataModel',
  components: { DataModelList, DataModelAdd, DataModelEdit, DataModelDetail },
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
