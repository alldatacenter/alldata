<template>
  <div>
    <div class="affix-time">
      <Affix :offset-top="100">
        <div class="flex affix-box">
          <affixTime :closeShop="true" @selected="clickBreadcrumb"/>

          <InputNumber
            placeholder="展示前N"
            :max="200"
            :min="10"
            v-model="params.top"
          ></InputNumber>
          <Button style="margin-left: 10px" @click="search">搜索</Button>
        </div>
      </Affix>
    </div>
    <div id="container"></div>
  </div>
</template>

<script>
import {Chart} from "@antv/g2";
import {getHotWordsStatistics} from "@/api/index";
import affixTime from "@/views/lili-components/affix-time";

export default {
  components: {
    affixTime,
  },
  data() {
    return {
      params: {
        // 请求参数
        searchType: "LAST_SEVEN",
        year: "",
        month: "",
        top: 50
      },
      hotWordsChart:"", //图表
      hotWordsData:[] //数据
    };
  },
  computed: {},
  methods: {
    clickBreadcrumb(val) {
      this.params = {...this.params, ...val}
    },
    // 初始化图表
    async search() {
      const res = await getHotWordsStatistics(this.params);
      if (res.success) {
        this.hotWordsData=res.result;
        this.hotWordsChart.data(this.hotWordsData)
        this.hotWordsChart.render();
      }
    },
    handleClickSearch() {
    },
    init(){
      let chart = this.hotWordsChart
      chart = new Chart({
        container: "container",
        autoFit: true,
        height: 500,
        padding: [50, 20, 50, 20],
      });
      chart.scale("score", {
        alias: "搜索次数",
      });

      chart.axis("keywords", {
        tickLine: {
          alignTick: false,
        },
      });
      chart.axis("score", false);

      chart.tooltip({
        showMarkers: false,
      });
      chart.interval().position("keywords*score");
      chart.interaction("element-active");
      this.hotWordsChart=chart;
      this.search();
    }
  },
  mounted() {
    this.init();
  },
};
</script>

<style lang="scss" scoped>
.affix-time {
  padding-left: 15px;
}
</style>
