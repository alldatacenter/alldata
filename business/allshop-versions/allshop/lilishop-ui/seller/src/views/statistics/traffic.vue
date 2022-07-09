<template>
  <div>
    <Affix :offset-top="100">
      <Card class="card fixed-bottom">
        <affixTime @selected="clickBreadcrumb" />
      </Card>
    </Affix>
    <Card class="card">
      <div>
        <h4>流量概况</h4>

      </div>
      <div class="box">
        <div class="box-item">
          <div>
            访客数UV
          </div>
          <div>
            {{uvs||0}}
          </div>
        </div>

        <div class="box-item">
          <div>
            浏览量PV
          </div>
          <div>
            {{pvs||0}}
          </div>
        </div>

      </div>

    </Card>

    <Card class="card">
      <div>
        <h4>流量趋势</h4>
        <div id="orderChart"></div>
      </div>
    </Card>

    <Card class="card">
      <div>
        <h4>客户增长报表</h4>
        <Table class="table" stripe :columns="columns" :data="data"></Table>
      </div>
    </Card>
  </div>
</template>
<script>
import affixTime from "@/views/lili-components/affix-time";
import * as API_Member from "@/api/member";
import { Chart } from "@antv/g2";
import Cookies from "js-cookie";
export default {
  components: { affixTime },

  data() {
    return {
      // 时间

      uvs: 0, // 访客数
      pvs: 0, // 浏览量

      dateList: [
        // 日期选择列表
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

      orderChart: "", // 流量趋势数据
      params: {
        searchType: "LAST_SEVEN",
        year: "",
        month: "",
        storeId: JSON.parse(Cookies.get("userInfoSeller")).id || "",
      },
      columns: [
        {
          key: "date",
          title: "日期",
        },
        {
          key: "pvNum",
          title: "浏览量",
        },
        {
          key: "uvNum",
          title: "访客数",
        },
      ],

      data: [], // 客户增长报表数据
    };
  },
  watch: {
    params: {
      handler(val) {
        this.uvs = 0;
        this.pvs = 0;
        this.init();
      },
      deep: true,
    },
  },
  methods: {
    // 订单图
    initChart() {
      // 默认已经加载 legend-filter 交互
      /**
       * 将数据分成三组来进行展示
       */

      let uv = [];
      let pv = [];

      this.data.forEach((item) => {
        uv.push({
          date: item.date,
          uvNum: item.uvNum,
          title: "访客数UV",
          pv: item.uvNum,
        });
        pv.push({
          date: item.date,
          pvNum: item.pvNum,
          pv: item.pvNum,
          title: "浏览量PV",
        });
      });

      let data = [...uv, ...pv];

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
        .position("date*pv")
        .color("title")
        .label("pv")
        .shape("smooth");

      this.orderChart
        .point()
        .position("date*pv")
        .color("title")
        .label("pv")
        .shape("circle")
        .style({
          stroke: "#fff",
          lineWidth: 1,
        });

      this.orderChart.render();
    },
    // 时间筛选
    clickBreadcrumb(item, index) {
      let callback = JSON.parse(JSON.stringify(item));

      this.params = callback;
    },
    // 初始化
    init() {
      API_Member.getStatisticsList(this.params).then((res) => {
        if (res.result) {
          this.data = res.result;
          res.result.forEach((item) => {
            this.uvs += parseInt(item.uvNum);
            this.pvs += parseInt(item.pvNum);
          });

          if (!this.orderChart) {
            this.orderChart = new Chart({
              container: "orderChart",
              autoFit: true,
              height: 500,
              padding: [70, 70, 70, 70],
            });
          }
          this.initChart();
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style scoped lang="scss">
.table {
  margin-top: 10px;
}
.wrapper {
  padding-bottom: 200px;
}
.box-item {
  display: flex;

  flex-direction: column;
  width: 25%;
  font-weight: bold;
  // align-items: center;
  justify-content: center;
  > div {
    margin: 4px;
  }
}
.box {
  background: rgb(250, 250, 250);
  padding: 10px;
  margin-top: 10px;
  display: flex;
}
.card {
  margin-bottom: 10px;
}
</style>
