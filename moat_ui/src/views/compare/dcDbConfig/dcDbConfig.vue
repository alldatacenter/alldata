<template>
  <div className="app-container">
    <transition name="el-zoom-in-center">
      <dc-db-config-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import dcDbConfigList from './dcDbConfigList'

export default {
  name: 'DcDbConfig',
  components: { dcDbConfigList },
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

