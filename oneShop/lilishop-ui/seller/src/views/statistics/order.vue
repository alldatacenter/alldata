<template>

  <div class="wrapper">
    <Affix :offset-top="100">
      <Card class="card fixed-bottom">

        <affixTime @selected="clickBreadcrumb" />

      </Card>
    </Affix>

    <Card class="card">
      <div>
        <h4>交易概况</h4>
        <div class="flex">
          <div class="transactionList">
            <div class="transaction-item" v-for="(item,index) in transactionList" :key="index">
              <h4>{{item.label}}</h4>
              <div class="transaction-card" v-if="item.label=='转换'">

                <div class="card-item">
                  <div class="card-item-label">访客数UV</div>
                  <div class="card-item-value">{{overViewList.uvNum || 0}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">下单转化率</div>
                  <div class="card-item-value">{{overViewList.orderConversionRate || '0%'}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">付款转化率</div>
                  <div class="card-item-value">{{overViewList.paymentsConversionRate ||'0%'}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">全店转化率</div>
                  <div class="card-item-value">{{overViewList.overallConversionRate || '0%'}}</div>
                </div>

              </div>
              <div class="transaction-card" v-if="item.label=='订单'">
                <div class="card-item">
                  <div class="card-item-label">下单笔数</div>
                  <div class="card-item-value">{{overViewList.orderNum || 0}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">下单人数</div>
                  <div class="card-item-value">{{overViewList.orderMemberNum || 0}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">下单金额</div>
                  <div class="card-item-value">{{overViewList.orderAmount || 0 | unitPrice('￥')}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">付款笔数</div>
                  <div class="card-item-value">{{overViewList.paymentOrderNum  || 0}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">付款人数</div>
                  <div class="card-item-value">{{overViewList.paymentsNum || 0}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">付款金额</div>
                  <div class="card-item-value">{{ overViewList.paymentAmount || 0 | unitPrice('￥')}}</div>
                </div>

              </div>
              <div class="transaction-card" v-if="item.label=='退单'">

                <div class="card-item">
                  <div class="card-item-label">退单笔数</div>
                  <div class="card-item-value">{{overViewList.refundOrderNum || 0}}</div>
                </div>
                <div class="card-item">
                  <div class="card-item-label">退单金额</div>
                  <div class="card-item-value">{{overViewList.refundOrderPrice || 0 | unitPrice('￥')}}</div>
                </div>

              </div>
            </div>
          </div>
          <div class="shap">
            <div id="overViewChart">
              <!-- 上 -->
              <div class="block">
                <div class="box">
                  <span>访客数</span>
                  <span>{{overViewList.uvNum  || 0}}</span>
                </div>
              </div>
              <!-- 中 -->
              <div class="block">
                <div class="box">
                  <span>下单笔数</span>
                  <span>{{overViewList.orderNum  || 0}}</span>
                </div>
              </div>
              <!-- 下 -->
              <div class="block">
                <div class="box">
                  <span>付款笔数</span>
                  <span>{{overViewList.paymentOrderNum  || 0 }}</span>
                </div>
              </div>

              <!-- 线 -->
              <div class="rightBorder">

              </div>
              <div class="leftTopBorder">

              </div>
              <div class="leftBottomBorder">

              </div>
              <!--数据 -->
              <div class="leftTopTips">
                <div>下单转化率 </div>
                <div>{{overViewList.orderConversionRate  || '0%' }}</div>
              </div>
              <div class="leftBottomTips">
                <div>付款转化率</div>
                <div>{{overViewList.paymentsConversionRate  || '0%'}}</div>
              </div>
              <div class="rightTips">
                <div>整体转换率</div>
                <div>{{overViewList.overallConversionRate  || '0%'}}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <Card class="card">
      <div>
        <h4>交易趋势</h4>
        <div>

        </div>
      </div>

      <div>

        <div id="orderChart"></div>

      </div>
    </Card>
    <Card class="card">
      <div>
        <h4>订退单统计</h4>
        <div class="breadcrumb" style="margin-bottom:20px;">
          <RadioGroup v-model="orderOrRefund" type="button" size="small" button-style="solid">
            <Radio :label="1">订单</Radio>
            <Radio :label="0">退单</Radio>
          </RadioGroup>
        </div>
        <div>
          <Table stripe :columns="columns" :data="data"></Table>
        </div>
        <Page @on-change="(index)=>{refundParams.pageNumber = index}" @on-page-size-change="(size)=>{refundParams.pageSize= size}" class="mt_10" show-total show-elevator :total="total" />
      </div>
    </Card>

  </div>
</template>
<script>
import * as API_Goods from "@/api/goods";
import { Chart } from "@antv/g2";

import orderRow from "./order/orderDetail";
import refundRow from "./order/refundOrder";
import affixTime from "@/views/lili-components/affix-time";
import Cookies from "js-cookie";
export default {
  components: { orderRow, refundRow, affixTime },

  data() {
    return {
      orderOrRefund: 1, // 订单还是退单
      total:0, // 订单总数
      // 订单状态
      orderStatusList: {
        UNDELIVERED: "待发货",
        UNPAID: "未付款",
        PAID: "已付款",
        DELIVERED: "已发货",
        CANCELLED: "已取消",
        COMPLETED: "已完成",
        TAKE: "已完成",
      },

      serviceTypeList: {
        // 服务类型
        CANCEL: "取消",
        RETURN_GOODS: "退货",
        EXCHANGE_GOODS: "换货",
        RETURN_MONEY: "退款",
      },
      serviceStatusList: {
        // 服务类型
        APPLY: "申请售后",
        PASS: "通过售后",
        REFUSE: "拒绝售后",
        BUYER_RETURN: "买家退货，待卖家收货",
        SELLER_RE_DELIVERY: "商家换货/补发",
        SELLER_CONFIRM: "卖家确认收货",
        SELLER_TERMINATION: "卖家终止售后",
        BUYER_CONFIRM: "买家确认收货",
        BUYER_CANCEL: "买家取消售后",
        WAIT_REFUND: "等待平台退款",
        COMPLETE: "完成售后",
      },
      //

      data: [], //定退单存储值

      columns: [], // 定退单title

      orderColumns: [
        // 订单表头
        {
          type: "expand",
          width: 50,
          render: (h, params) => {
            return h(orderRow, {
              props: {
                res: params.row,
              },
            });
          },
        },
        {
          title: "商家名称",
          key: "storeName",
        },
        {
          title: "用户名",
          key: "memberName",
        },

        {
          title: "订单状态",
          key: "orderStatus",
          render: (h, params) => {
            return h("div", this.orderStatusList[params.row.orderStatus]);
          },
        },
        {
          title: "创建时间",
          key: "createTime",
        },

        {
          title: "支付时间",
          key: "paymentTime",
          render: (h, params) => {
            return h("div", params.row.paymentTime || "暂无");
          },
        },
        {
          title: "价格",
          key: "flowPrice",
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.flowPrice, "￥")
            );
          },
        },
      ],
      refundColumns: [
        // 退货单表头
        {
          type: "expand",
          width: 50,
          render: (h, params) => {
            return h(refundRow, {
              props: {
                res: params.row,
              },
            });
          },
        },
        {
          title: "商品图片",
          key: "goodsImage",
          render: (h, params) => {
            return h("img", {
              attrs: {
                src: params.row.goodsImage,
              },
              style: {
                width: "60px",
                verticalAlign: "middle",
              },
            });
          },
        },

        {
          title: "商品名称",
          key: "goodsName",
        },
        {
          title: "商家名称",
          key: "sellerName",
        },
        {
          title: "售后单类型",
          key: "serviceType",
          render: (h, params) => {
            return h("div", this.serviceTypeList[params.row.serviceType]);
          },
        },
        {
          title: "售后单状态",
          key: "serviceStatus",
          render: (h, params) => {
            return h("div", this.serviceStatusList[params.row.serviceStatus]);
          },
        },
        {
          title: "退款时间",
          key: "refundTime",
          render: (h, params) => {
            return h("div", params.row.refundTime || "暂无");
          },
        },
        {
          title: "申请退款金额",
          key: "applyRefundPrice",
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.applyRefundPrice, "￥")
            );
          },
        },
        {
          title: "申请原因",
          key: "reason",
        },
        {
          title: "实际金额",
          key: "flowPrice",
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.flowPrice, "￥")
            );
          },
        },
      ],

      // 交易概况
      transactionList: [
        {
          label: "转换",
          value: "",
        },
        {
          label: "订单",
          value: "",
        },
        {
          label: "退单",
          value: "",
        },
      ],

      chartList: [], // 绘制订单图表数据
      orderChart: "", //订单图表

      overViewList: {}, // 绘制订单统计概览
      overViewChart: "", //订单订单统计概览图标

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

      // 订单传参
      orderParams: {
        searchType: "LAST_SEVEN", // TODAY ,  YESTERDAY , LAST_SEVEN , LAST_THIRTY
        year: "",
        storeId: JSON.parse(Cookies.get("userInfoSeller")).id || "",
        memberId: "",
      },
      // 订单概念
      overViewParams: {
        month: "",
        searchType: "LAST_SEVEN", // TODAY ,  YESTERDAY , LAST_SEVEN , LAST_THIRTY
        storeId: JSON.parse(Cookies.get("userInfoSeller")).id || "",
        year: "",
      },
      defaultParams: {
        month: "",
        searchType: "LAST_SEVEN", // TODAY ,  YESTERDAY , LAST_SEVEN , LAST_THIRTY
        storeId: JSON.parse(Cookies.get("userInfoSeller")).id || "",
        year: "",
      },

      refundIndex: 0, // 当前退单下标详情
      // 退单订单
      refundParams: {
        pageNumber: 1,
        pageSize: 10,
        searchType: "LAST_SEVEN",
        storeId: JSON.parse(Cookies.get("userInfoSeller")).id || "",
        year: "",
      },

      //
      //
    };
  },
  watch: {
    refundParams: {
      handler() {
        console.log(this.refundIndex);
        if (this.refundIndex == 1) {
          this.getOrderRefundList();
        } else {
          this.getOrderList();
        }
      },
      deep: true,
      immediate: true,
    },
    orderParams: {
      handler() {
        this.initOrderChartList();
      },
      deep: true,
    },
    overViewParams: {
      handler() {
        this.initOrderOverViewList();
      },
      deep: true,
    },
    orderOrRefund: {
      // 订单还是退单
      handler(val) {
        if (val == 1) {
          this.getOrderList();
        } else {
          this.getOrderRefundList();
        }
      },
    },
  },
  methods: {
    // 订单图
    initOrderChart() {
      // 默认已经加载 legend-filter 交互
      let data = this.chartList;

      data.forEach((item) => {
        item.createTime = item.createTime.split(" ")[0];
        item.title = "交易额";
      });
      this.orderChart.data(data);

      this.orderChart.tooltip({
        showCrosshairs: true,
        shared: true,
      });

      this.orderChart
        .line()
        .position("createTime*price")
        .label("price")
        .color("title")
        .shape("smooth");

      this.orderChart
        .point()
        .position("createTime*price")
        .label("price")
        .color("title")
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

      this.orderParams = callback;

      this.overViewParams = callback;
      this.refundParams = callback;
    },

    // 实例化订单概览
    async initOrderOverViewList() {
      const res = await API_Goods.getOrderOverView(this.overViewParams);
      if (res.success) {
        this.overViewList = res.result;
      }
    },
    // 实例化订单图表
    async initOrderChartList(name) {
      const res = await API_Goods.getOrderChart(this.orderParams);
      if (res.success) {
        this.chartList = res.result;

        if (!this.orderChart) {
          this.orderChart = new Chart({
            container: "orderChart",
            autoFit: true,
            height: 500,
            padding: [70, 70, 70, 70],
          });
        }

        this.initOrderChart(); //订单表
      }
    },
    // 统计相关订单统计
    async getOrderList() {
      const res = await API_Goods.statisticsOrderList(this.refundParams);
      if (res.success) {
        this.data = res.result.records;
        this.columns = this.orderColumns;
        this.total = res.result.total;
      }
    },

    //  统计相关退单统计
    async getOrderRefundList() {
      const res = await API_Goods.statisticsOrderRefundList(this.refundParams);
      if (res.success) {
        this.data = res.result.records;
        this.columns = this.refundColumns;
        this.total = res.result.total;
      }
    },

    // 实例化初始值
    initBaseParams() {
      let data = new Date();
      this.getOrderList();
      this.orderParams.year = data.getFullYear();
      this.overViewParams.year = data.getFullYear();
    },
  },

  mounted() {
    this.initBaseParams();
  },
};
</script>
<style scoped lang="scss">
.active {
  color: $theme_color;
  position: relative;

  &::before {
    content: "";
    position: absolute;
    width: 100%;
    height: 3px;
    bottom: -5px;
    left: 0;
    background: $theme_color;
  }
}
.breadcrumb {
  span {
    cursor: pointer;
  }
}

.page-col {
  text-align: right;
  margin: 10px 0;
}
.wrapper {
  padding-bottom: 200px;
}
.page {
  text-align: right;
  margin: 20px 0;
}
#overViewChart {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  position: relative;
  margin-left: 20px;
  > .leftTopTips {
    position: absolute;
    top: 68px;
    left: -2px;
    width: 85px;
    text-align: center;
    background: rgb(255, 255, 255);
    z-index: 1;
    padding: 5px 0px;
  }
  > .leftBottomTips {
    position: absolute;
    bottom: 100px;
    left: -2px;
    width: 85px;
    text-align: center;
    background: rgb(255, 255, 255);
    z-index: 1;
    padding: 5px 0px;
  }
  > .rightTips {
    position: absolute;
    bottom: 240px;
    right: 0px;
    width: 85px;
    text-align: center;
    background: rgb(255, 255, 255);
    z-index: 1;
    padding: 5px 0px;
  }
  > .rightBorder {
    width: 110px;
    position: absolute;
    top: 30px;
    right: 40px;
    border: 2px solid #d9d9d9;
    border-left: 0;
    height: 280px;
  }
  > .leftTopBorder {
    border: 2px solid #d9d9d9;
    height: 118px;
    width: 56px;
    position: absolute;
    left: 40px;
    top: 30px;
    border-right: 0;
  }
  > .leftBottomBorder {
    width: 108px;
    border: 2px solid #d9d9d9;
    height: 150px;
    position: absolute;
    bottom: 45px;
    left: 40px;
    border-right: 0;
  }
  > .block {
    height: 0px;
    border-left: 30px solid transparent;
    border-right: 30px solid transparent;
    position: relative;
    background: rgb(255, 255, 255);
    z-index: 1;
    position: relative;
  }
  > .block:nth-of-type(1) {
    width: 240px;
    border-top: 90px solid #ff4646;
    > .box {
      left: -30px;
      top: -90px;
      width: 240px;
      height: 90px;
    }
  }
  > .block:nth-of-type(2) {
    width: 172px;
    border-top: 100px solid #ff8585;
    margin-top: 10px;
    > .box {
      left: -29px;
      top: -100px;
      width: 172px;
      height: 100px;
    }
  }
  > .block:nth-of-type(3) {
    width: 100px;
    margin-top: 10px;
    border-top: 150px solid #ffb396;
    border-left: 50px solid transparent;
    border-right: 50px solid transparent;

    > .box {
      left: -50px;
      top: -150px;
      width: 100px;
      height: 120px;
      z-index: 2;
    }
  }
  /deep/ .box {
    color: #fff;
    position: absolute;

    z-index: 2;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    > span {
      font-size: 16px;
      font-weight: bold;
    }
  }
}

.transaction-item {
  margin: 10px 0;
}
h4 {
  margin: 0 0 20px 0;
}
.transactionList {
  flex: 7;
  padding: 0 20px;
}
.shap {
  width: 400px;
  margin-left: 20px;
  margin-top: 50px;
}

.transaction-card {
  height: 120px;
  border-radius: 0.4em;
  display: flex;
  background: #f3f5f7;
}
.card-item-label {
  font-weight: bold;
  font-size: #666;
  font-size: 15px;
  margin-bottom: 10px;
}
.card-item-value {
  font-size: 15px;
  font-weight: bold;
  color: $theme_color;
}
.card-item {
  height: 100%;

  width: 20%;
  align-items: center;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.order-col {
  display: flex;
  > div {
    margin-right: 8px;
    padding: 16px;
    font-size: 15px;
  }
}
.order-list {
  display: flex;
}
.tips {
  margin: 0 8px;
}
.card {
  margin-bottom: 10px;
}
</style>
