<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <check-log-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import CheckLogList from './CheckLogList'

export default {
  name: 'CheckLog',
  components: { CheckLogList },
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
