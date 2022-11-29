<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <my-started-instance-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import MyStartedInstanceList from './MyStartedInstanceList'

export default {
  name: 'MyStartedInstance',
  components: { MyStartedInstanceList },
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
