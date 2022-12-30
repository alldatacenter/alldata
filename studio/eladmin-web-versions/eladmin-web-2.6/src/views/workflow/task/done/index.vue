<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <task-done-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import TaskDoneList from './TaskDoneList'

export default {
  name: 'TaskDone',
  components: { TaskDoneList },
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
