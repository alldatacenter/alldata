<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-screen-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataScreenList from './DataScreenList'

export default {
  name: 'DataScreen',
  components: { DataScreenList },
  data() {
    return {
      options: {
        data: {},
        showList: true
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
