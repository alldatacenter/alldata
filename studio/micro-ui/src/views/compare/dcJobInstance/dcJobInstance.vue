<template>
  <div className="app-container">
    <transition name="el-zoom-in-center">
      <dc-job-instance-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import dcJobInstanceList from './dcJobInstanceList'

export default {
  name: 'DcJobInstance',
  components: { dcJobInstanceList },
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

