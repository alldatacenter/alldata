<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <contrast-stat-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import ContrastStatList from './ContrastStatList'

export default {
  name: 'ContrastStat',
  components: { ContrastStatList },
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
