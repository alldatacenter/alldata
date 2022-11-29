<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-api-list v-if="options.showList" @showCard="showCard"></data-api-list>
    </transition>
    <transition name="el-zoom-in-top">
      <data-api-add v-if="options.showAdd" :data="options.data" @showCard="showCard"></data-api-add>
    </transition>
    <transition name="el-zoom-in-top">
      <data-api-edit v-if="options.showEdit" :data="options.data" @showCard="showCard"></data-api-edit>
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-api-detail v-if="options.showDetail" :data="options.data" @showCard="showCard"></data-api-detail>
    </transition>
    <transition name="el-zoom-in-bottom">
      <data-api-example v-if="options.showExample" :data="options.data" @showCard="showCard"></data-api-example>
    </transition>
  </div>
</template>

<script>
import DataApiList from './DataApiList'
import DataApiAdd from './DataApiAdd'
import DataApiEdit from './DataApiEdit'
import DataApiDetail from './DataApiDetail'
import DataApiExample from './DataApiExample'

export default {
  name: 'DataApi',
  components: { DataApiList, DataApiAdd, DataApiEdit, DataApiDetail, DataApiExample },
  data () {
    return {
      options: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false,
        showExample: false
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
