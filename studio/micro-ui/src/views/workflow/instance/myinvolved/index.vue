<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <my-involved-instance-list v-if="options.showList" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import MyInvolvedInstanceList from './MyInvolvedInstanceList'

export default {
  name: 'MyInvolvedInstance',
  components: { MyInvolvedInstanceList },
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
