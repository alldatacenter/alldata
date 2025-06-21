<template>
  <div className="app-container">
    <transition name="el-zoom-in-center">
      <dc-job-config-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import dcJobConfigList from './dcJobConfigList'

export default {
  name: 'DcJobConfig',
  components: { dcJobConfigList },
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

