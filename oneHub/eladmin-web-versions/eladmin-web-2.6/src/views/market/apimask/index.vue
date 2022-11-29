<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <api-mask-list v-if="options.showList" @showCard="showCard"></api-mask-list>
    </transition>
    <transition name="el-zoom-in-top">
      <api-mask-add v-if="options.showAdd" :data="options.data" @showCard="showCard"></api-mask-add>
    </transition>
    <transition name="el-zoom-in-top">
      <api-mask-edit v-if="options.showEdit" :data="options.data" @showCard="showCard"></api-mask-edit>
    </transition>
    <transition name="el-zoom-in-bottom">
      <api-mask-detail v-if="options.showDetail" :data="options.data" @showCard="showCard"></api-mask-detail>
    </transition>
  </div>
</template>

<script>
import ApiMaskList from './ApiMaskList'
import ApiMaskAdd from './ApiMaskAdd'
import ApiMaskEdit from './ApiMaskEdit'
import ApiMaskDetail from './ApiMaskDetail'

export default {
  name: 'ApiMask',
  components: { ApiMaskList, ApiMaskAdd, ApiMaskEdit, ApiMaskDetail },
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
