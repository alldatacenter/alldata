<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <task-todo-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import TaskTodoList from './TaskTodoList'

export default {
  name: 'TaskTodo',
  components: { TaskTodoList },
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
