<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <check-job-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import CheckJobList from './CheckJobList'

export default {
  name: 'CheckJob',
  components: { CheckJobList },
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
