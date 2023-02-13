<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <check-rule-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <check-rule-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <check-rule-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-bottom">
      <check-rule-detail v-if="options.showDetail" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import CheckRuleList from './CheckRuleList'
import CheckRuleAdd from './CheckRuleAdd'
import CheckRuleEdit from './CheckRuleEdit'
import CheckRuleDetail from './CheckRuleDetail'

export default {
  name: 'CheckRule',
  components: { CheckRuleList, CheckRuleAdd, CheckRuleEdit, CheckRuleDetail },
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
