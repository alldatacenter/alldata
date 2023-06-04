<template>
  <div className="app-container">
    <transition name="el-zoom-in-center">
      <dc-job-log-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import dcJobLogList from './dcJobLogList'

export default {
  name: 'DcJobLog',
  components: { dcJobLogList },
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

