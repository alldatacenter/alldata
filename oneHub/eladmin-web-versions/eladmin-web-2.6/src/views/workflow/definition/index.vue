<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <definition-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DefinitionList from './DefinitionList'

export default {
  name: 'Definition',
  components: { DefinitionList },
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
