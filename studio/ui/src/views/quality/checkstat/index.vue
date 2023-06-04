<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <check-stat-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import CheckStatList from './CheckStatList'

export default {
  name: 'CheckStat',
  components: { CheckStatList },
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
