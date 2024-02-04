<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <data-board-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DataBoardList from './DataBoardList'

export default {
  name: 'DataBoard',
  components: { DataBoardList },
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
