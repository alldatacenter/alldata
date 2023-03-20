<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <log-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <log-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import LogList from './LogList'
import LogDetail from './LogDetail'

export default {
  name: 'ApiLog',
  components: { LogList, LogDetail },
  data() {
    return {
      options: {
        data: {},
        showList: true,
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
