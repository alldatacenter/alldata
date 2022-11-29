<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <change-record-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <change-record-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <change-record-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <change-record-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import ChangeRecordList from './ChangeRecordList'
import ChangeRecordAdd from './ChangeRecordAdd'
import ChangeRecordEdit from './ChangeRecordEdit'
import ChangeRecordDetail from './ChangeRecordDetail'

export default {
  name: 'ChangeRecord',
  components: { ChangeRecordList, ChangeRecordAdd, ChangeRecordEdit, ChangeRecordDetail },
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
