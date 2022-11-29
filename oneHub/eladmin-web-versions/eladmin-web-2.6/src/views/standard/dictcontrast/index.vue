<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <dict-contrast-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <dict-contrast-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <dict-contrast-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <dict-contrast-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import DictContrastList from './DictContrastList'
import DictContrastAdd from './DictContrastAdd'
import DictContrastEdit from './DictContrastEdit'
import DictContrastDetail from './DictContrastDetail'

export default {
  name: 'DictContrast',
  components: { DictContrastList, DictContrastAdd, DictContrastEdit, DictContrastDetail },
  data() {
    return {
      options: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
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
