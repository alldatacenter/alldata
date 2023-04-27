<template>
  <div className="app-container">
    <transition name="el-zoom-in-center">
      <dc-job-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import dcJobList from './dcJobList'

export default {
  name: 'DcJob',
  components: { dcJobList },
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

