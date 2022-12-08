<template>
  <div class="search">
    <Card>
      <Tabs @on-click="handleClickTab">
        <TabPane
          v-for="(item, index) in templatesWay"
          :key="index"
          :name="item.template"
          :label="item.label"
        >
          <components v-if="item.template == currentTemplate" :is="currentTemplate"></components>
        </TabPane>
      </Tabs>
    </Card>
  </div>
</template>

<script>
import todayHotWords from "./template/todayHotWords";
import historyHotWords from "./template/historyHotWords";
import setupHotWords from "./template/setupHotWords";
import statisticsHotWords from "./template/statisticsHotWords";
export default {
  name: "hotWords",
  components: {
    todayHotWords,
    historyHotWords,
    setupHotWords,
    statisticsHotWords
  },
  data() {
    return {
      // 模版集合key value
      templatesWay: [
        {
          template: "todayHotWords",
          label: "今日热次",
        },
        {
          template: "historyHotWords",
          label: "历史热词",
        },
        {
          template: "statisticsHotWords",
          label: "热词统计",
        },
        {
          template: "setupHotWords",
          label: "设置热词",
        },
      ],
      // 引入模板
      templates: {
        todayHotWords,
        historyHotWords,
        setupHotWords,
        statisticsHotWords
      },
      // 当前模版
      currentTemplate: "todayHotWords",
    };
  },
  methods: {
    handleClickTab(val) {
      this.currentTemplate = val;
    },
  },

};
</script>
<style lang="scss" scoped></style>
