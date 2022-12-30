<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <running-instance-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import RunningInstanceList from './RunningInstanceList'

export default {
  name: 'RunningInstance',
  components: { RunningInstanceList },
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
