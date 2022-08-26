<template>
  <div class="wrapper">
    <Affix :offset-top="100">
      <Card class="card fixed-bottom">
        <affixTime :closeShop="true" @selected="clickBreadcrumb" />
      </Card>
    </Affix>

    <Card class="card">
      <div>
        <h4>客户增长趋势 <Button style="margin-left:10px" @click="()=>{enableShowMemberCount = !enableShowMemberCount; initMemberChart()}" size="small">{{enableShowMemberCount ? '关闭' : '显示'}}用户总人数</Button></h4>
        <div id="orderChart"></div>
      </div>
    </Card>

    <Card class="card">
      <div>
        <h4>客户增长报表</h4>
        <Table class="mt_10" stripe :columns="columns" :data="data"></Table>

      </div>
    </Card>

  </div>
</template>
<script>
import * as API_Member from "@/api/member";
import { Chart } from "@antv/g2";
import affixTime from "@/views/lili-components/affix-time";

export default {
  components: { affixTime },
  data() {
    return {
      columns: [ // 表头
        {
          key: "createDate",
          title: "日期",
          sortable: true,
        },
        {
          key: "memberCount",
          title: "当前会员",
        },
        {
          key: "newlyAdded",
          title: "新增会员",
        },
        {
          key: "activeQuantity",
          title: "活跃会员",
        },
      ],
      // 时间
      dateList: [
        {
          title: "今天",
          selected: false,
          value: "TODAY",
        },
        {
          title: "昨天",
          selected: false,
          value: "YESTERDAY",
        },
        {
          title: "最近7天",
          selected: true,
          value: "LAST_SEVEN",
        },
        {
          title: "最近30天",
          selected: false,
          value: "LAST_THIRTY",
        },
      ],
      year: "", // 当前年限
      orderChart: "", // 订单表格
      params: { // 请求参数
        searchType: "LAST_SEVEN",
        year: "",
        month: "",
        shopId: "",
      },

      data: [], // 数据
      enableShowMemberCount:false
    };
  },
  watch: {
    params: {
      handler(val) {
        this.init();
      },
      deep: true,
      immediate:true,
    },
    year(val) {
      this.params.year = new Date(val).getFullYear();
    },
  },
  methods: {
    // 订单图
    initMemberChart() {
      // 默认已经加载 legend-filter 交互
      /**
       * 将数据分成三组来进行展示
       */

      let count = [];
      let newly = [];
      let actives = [];

      this.data.forEach((item) => {
        if (this.enableShowMemberCount && (item.memberCount!="" || item.memberCount!=null)) {
          count.push({
            createDate: item.createDate,
            memberCount:item.memberCount,
            title: "当前会员数量",
          });
        }
        if (!this.enableShowMemberCount && (item.newlyAdded!="" || item.newlyAdded!=null)) {
          newly.push({
            createDate: item.createDate,
            memberCount: item.newlyAdded,
            title: "新增会员数量",
          });
        }
        if (!this.enableShowMemberCount && (item.activeQuantity!="" || item.activeQuantity!=null)) {
          actives.push({
            createDate: item.createDate,
            memberCount: item.activeQuantity,
            title: "当日活跃数量",
          });
        }
      });

      let data = [...count, ...newly, ...actives];

      this.orderChart.data(data);
      this.orderChart.scale({
        activeQuantity: {
          range: [0, 1],
          nice: true,
        },
      });
      this.orderChart.tooltip({
        showCrosshairs: true,
        shared: true,
      });

      this.orderChart
        .line()
        .position("createDate*memberCount")
        .color("title")
        .label("memberCount")
        .shape("smooth");

      this.orderChart
        .point()
        .position("createDate*memberCount")
        .color("title")
        .label("memberCount")
        .shape("circle")
        .style({
          stroke: "#fff",
          lineWidth: 1,
        });
         this.orderChart.area().position("createDate*memberCount").color("title").shape("smooth");


      this.orderChart.render();
    },
    // 条件查询
    clickBreadcrumb(item, index) {
      let callback = item;

      console.warn(callback);
      this.params = {...callback};
    },
    // 初始化数据
    init() {
      API_Member.getMemberStatistics(this.params).then((res) => {
        if (res.result) {
          res.result.forEach((item) => {
            item.activeQuantity += "";
          });
          this.data = res.result;

          if (!this.orderChart) {
            this.orderChart = new Chart({
              container: "orderChart",
              autoFit: true,
              height: 500,
              padding: [70, 70, 70, 70],
            });
          }
          this.initMemberChart();
        }
      });
    },
  },
  mounted() {
    let data = new Date();
    this.year = data;
  },
};
</script>
<style scoped lang="scss">
.wrapper {
  padding-bottom: 200px;
}
.card {
  margin-bottom: 10px;
}
</style>
