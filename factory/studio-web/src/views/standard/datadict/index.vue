<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-dict-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-dict-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <data-dict-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-dict-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataDictList from './DataDictList'
import DataDictAdd from './DataDictAdd'
import DataDictEdit from './DataDictEdit'
import DataDictDetail from './DataDictDetail'

export default {
  name: 'DataDict',
  components: { DataDictList, DataDictAdd, DataDictEdit, DataDictDetail },
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
