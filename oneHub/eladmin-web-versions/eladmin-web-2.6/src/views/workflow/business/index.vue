<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <business-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <business-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <business-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <business-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import BusinessList from './BusinessList'
import BusinessAdd from './BusinessAdd'
import BusinessEdit from './BusinessEdit'
import BusinessDetail from './BusinessDetail'

export default {
  name: 'Business',
  components: { BusinessList, BusinessAdd, BusinessEdit, BusinessDetail },
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
