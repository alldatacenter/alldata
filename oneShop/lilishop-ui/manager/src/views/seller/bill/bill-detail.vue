<template>
  <div>
    <Card>
      <p slot="title">商家信息</p>
      <div class="flex flex_align_item">
        <p>店铺名称：{{ bill.storeName }}</p>
        <p>银行开户名：{{ bill.bankAccountName }}</p>
        <p>银行账号：{{ bill.bankAccountNumber }}</p>
        <p>开户行支行名称：{{ bill.bankName }}</p>
        <p>支行联行号：{{ bill.bankCode }}</p>
      </div>
    </Card>
    <Card class="mt_10 mb_10">
      <p slot="title">账单详细</p>

      <div class="tips-status">
        <span>账单状态 ：</span>
        <span class="theme_color">{{
          bill.billStatus | unixSellerBillStatus
        }}</span>
        <Button
          v-if="bill.billStatus == 'CHECK'"
          size="mini"
          type="primary"
          @click="pass()"
          >付款</Button
        >
      </div>

      <table>
        <tbody>
          <tr v-for="(item, index) in data" :key="index">
            <td>{{ item.name }}：</td>
            <td>{{ item.value }}</td>
          </tr>
        </tbody>
      </table>
      <div>
        <h3 class="ml_10" style="padding:10px;">结算详细</h3>
        <div class="bill-detail-price">

          <span>
            <p>积分结算金额</p>
            <p class="increase-color">
              +{{ bill.pointSettlementPrice || 0 | unitPrice("￥") }}
            </p>
          </span>
          <span>
            <p>平台优惠券补贴</p>
            <p class="increase-color">
              +{{ bill.siteCouponCommission || 0 | unitPrice("￥") }}
            </p>
          </span>

          <span>
            <p>砍价商品结算金额</p>
            <p class="increase-color">
              +{{ bill.kanjiaSettlementPrice || 0 | unitPrice("￥") }}
            </p>
          </span>
          <span>
            <p>退单分销返现返还</p>
            <p class="increase-color">
              +{{ bill.distributionRefundCommission || 0 | unitPrice("￥") }}
            </p>
          </span>
          <span>
            <p>退单产生退还佣金金额</p>
            <p class="increase-color">
              +{{ bill.refundCommissionPrice || 0 | unitPrice("￥") }}
            </p>
          </span>
           <span>
            <p>退单金额</p>
            <p class="theme_color">
              -{{ bill.refundPrice || 0 | unitPrice("￥") }}
            </p>
          </span>

          <span>
            <p>平台收取佣金</p>
            <p class="theme_color">
              -{{ bill.commissionPrice || 0 | unitPrice("￥") }}
            </p>
          </span>
          <span>
            <p>分销返现支出</p>
            <p class="theme_color">
              -{{ bill.distributionCommission || 0 | unitPrice("￥") }}
            </p>
          </span>
          <span>
            <p>退单平台优惠券补贴返还</p>
            <p class="theme_color">
              -{{ bill.siteCouponRefundCommission || 0 | unitPrice("￥") }}
            </p>
          </span>
        </div>
      </div>
    </Card>
    <Tabs active-key="key1" @on-click="clickTabs">
      <Tab-pane label="入账流水" key="key1">
        <Card>
          <Table
            :loading="loading"
            border
            :columns="orderColumns"
            :data="order"
            ref="table"
          ></Table>
          <Row type="flex" justify="end" class="mt_10">
            <Page
              :current="orderParam.pageNumber"
              :total="orderTotal"
              :page-size="orderParam.pageSize"
              @on-change="orderChangePage"
              @on-page-size-change="orderChangePageSize"
              size="small"
              show-total
              show-elevator
            ></Page>
          </Row>
        </Card>
      </Tab-pane>
      <Tab-pane label="退款流水" key="key2">
        <Card>
          <Table
            :loading="loading"
            border
            :columns="refundColumns"
            :data="refund"
            ref="table"
          ></Table>
          <Row type="flex" justify="end" class="mt_10">
            <Page
              :current="refundParam.pageNumber"
              :total="refundTotal"
              :page-size="refundParam.pageSize"
              @on-change="getRefund()"
              @on-page-size-change="getRefund()"
              size="small"
              show-total
              show-elevator
            ></Page>
          </Row>
        </Card>
      </Tab-pane>
    </Tabs>
  </div>
</template>
<script>
import * as filters from "@/utils/filters";
import * as API_Shop from "@/api/shops";
export default {
  name: "bill-detail",
  data() {
    return {
      loading: false,
      columns: [
        // 表头
        {
          title: "项目",
          key: "name",
          width: 250,
        },
        {
          title: "值",
          key: "value",
        },
      ],
      data: [
        // 数据
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
        {
          name: "计算中",
          value: 0,
        },
      ],
      id: "", // 账单id
      bill: {}, // 账单详情
      order: [], // 订单列表
      orderParam: {
        // 请求参数
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "id", // 默认排序字段
        order: "desc", // 默认排序方式
        flowType: "PAY",
        startDate: null,
        endDate: null,
      },
      orderColumns: [
        // 订单表头
        {
          title: "入账时间",
          key: "createTime",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "订单编号",
          key: "orderSn",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "订单金额",
          key: "finalPrice",
          width: 120,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.finalPrice, "￥")
            );
          },
        },
        {
          title: "平台分佣",
          key: "commissionPrice",
          width: 120,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.commissionPrice, "￥")
            );
          },
        },
        {
          title: "平台优惠券",
          key: "siteCouponPrice",
          render: (h, params) => {
            if (params.row.siteCouponPrice == null) {
              return h("div", "-");
            } else {
              return h(
                "div",
                this.$options.filters.unitPrice(
                  params.row.siteCouponPrice,
                  "￥"
                )
              );
            }
          },
        },
        {
          title: "平台优惠券补贴金额",
          key: "siteCouponCommission",
          render: (h, params) => {
            if (params.row.siteCouponCommission == null) {
              return h("div", "-");
            } else {
              return h(
                "div",
                this.$options.filters.unitPrice(
                  params.row.siteCouponCommission,
                  "￥"
                )
              );
            }
          },
        },
        {
          title: "分销金额",
          key: "distributionRebate",
          width: 120,
          render: (h, params) => {
            if (params.row.distributionRebate == null) {
              return h("div", "-");
            } else {
              return h(
                "div",
                this.$options.filters.unitPrice(
                  params.row.distributionRebate,
                  "￥"
                )
              );
            }
          },
        },
        {
          title: "应结金额",
          key: "billPrice",
          width: 120,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.billPrice, "￥")
            );
          },
        },
      ],
      refund: [], // 退款单
      refundParam: {
        // 请求参数
        flowTypeEnum: "PAY",
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "id", // 默认排序字段
        order: "desc", // 默认排序方式
        flowType: "REFUND",
        startDate: null,
        endDate: null,
      },
      refundColumns: [
        // 退款单表头
        {
          title: "退款时间",
          key: "createTime",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "退款流水编号",
          key: "sn",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "订单编号",
          key: "orderSn",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "售后编号",
          key: "refundSn",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "退款金额",
          key: "finalPrice",
          width: 120,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.finalPrice, "￥")
            );
          },
        },
        {
          title: "退还佣金",
          key: "commissionPrice",
          minWidth: 120,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.commissionPrice, "￥")
            );
          },
        },

        {
          title: "退还平台优惠券",
          key: "siteCouponCommission",
          minWidth: 110,
        },
        {
          title: "退还分销",
          key: "distributionRebate",
          minWidth: 120,
          render: (h, params) => {
            if (params.row.distributionRebate == null) {
              return h("div", "-");
            } else {
              return h(
                "div",
                this.$options.filters.unitPrice(
                  params.row.distributionRebate,
                  "￥"
                )
              );
            }
          },
        },

        {
          title: "合计金额",
          key: "billPrice",
          minWidth: 120,
          render: (h, params) => {
            if (params.row.billPrice == null) {
              return h("div", "-");
            } else {
              return h(
                "div",
                this.$options.filters.unitPrice(params.row.billPrice, "￥")
              );
            }
          },
        },
      ],
      orderTotal: 0, // 订单总数
      refundTotal: 0, // 退款单总数
    };
  },
  methods: {
    //订单页数发生变化
    orderChangePage(v) {
      this.orderParam.pageNumber = v;
      this.getOrder();
    },
    //订单每页条数变化
    orderChangePageSize(v) {
      this.orderParam.pageNumber = 1;
      this.orderParam.pageSize = v;
      this.getOrder();
    },
    //退款单页数发生变化
    refundOrderChangePage(v) {
      this.refundParam.pageNumber = v;
      this.getRefund();
    },
    //退款单每页条数变化
    refundOrderChangePageSize(v) {
      this.refundParam.pageSize = v;
      tthis.getRefund();
    },
    clickTabs(index) {
      this.orderParam.pageNumber = 1
      if (index == 1) {
        this.orderParam.flowType = "REFUND";
        this.getRefund();
      } else {
        this.orderParam.flowType = "PAY";
        this.getOrder();
      }
    },

    pass() {
      API_Shop.pay(this.id).then((res) => {
        if (res.success) {
          this.$Message.success(res.message);
          this.init();
        }
      });
    },

    init() {
      this.id = this.$route.query.id;
      this.getDetail();
    },
    getDetail() {
      API_Shop.getBuyBillDetail(this.id).then((res) => {
        if (res.success) {
          this.bill = res.result;
          //初始化表格
          this.initTable();
          //初始化订单信息
          this.orderParam.startDate = this.bill.startTime;
          this.orderParam.endDate = this.bill.endTime;
          this.refundParam.startDate = this.bill.startTime;
          this.refundParam.endDate = this.bill.endTime;
          this.getOrder();
        }
      });
    },
    initTable() {
      let bill = this.bill;
      this.data[0].name = "结算单号";
      this.data[0].value = bill.sn;

      this.data[1].name = "起止日期";
      this.data[1].value = bill.startTime + "~" + bill.endTime;

      this.data[2].name = "出帐日期";
      this.data[2].value = bill.createTime;

      this.data[3].name = "当前状态";
      this.data[3].value = filters.unixSellerBillStatus(bill.billStatus);

      this.data[4].name = "当前店铺";
      this.data[4].value = bill.storeName;

      this.data[5].name = "平台打款时间";
      this.data[5].value = bill.payTime === null ? "未付款" : bill.payTime;
      this.data[6].name = "订单付款总金额";
      this.data[6].value = filters.unitPrice(
        bill.orderPrice ? bill.orderPrice : 0,
        "¥"
      );
      this.data[7].name = "结算金额";
      this.data[7].value = filters.unitPrice(
        bill.billPrice ? bill.billPrice : 0,
        "¥"
      );
    },
    getOrder() {
      API_Shop.getStoreFlow(this.id, this.orderParam).then((res) => {
        if (res.result) {
          this.order = res.result.records;
          this.orderTotal = res.result.total;
        }
      });
      this.orderTotal = this.order.length;
    },
    getRefund() {
      API_Shop.getStoreFlow(this.id, this.orderParam).then((res) => {
        this.loading = false;
        if (res.result) {
          this.refund = res.result.records;
          this.refundTotal = res.result.total;

          this.$set(this, "refund", res.result.records);
          console.log();
        }
      });
      this.refundTotal = this.refund.length;
    },
  },
  mounted() {
    this.init();
  },
};
</script>

<style scoped lang="scss">
.flex {
  justify-content: space-between;
  flex-wrap: wrap;
  > p {
    width: 50%;
    margin: 15px 0;
  }
}
.tips-status {
  padding: 10px;
  font-size: 14px;
  > span {
    font-weight: bold;
    margin-right: 8px;
  }
  > span:nth-of-type(2) {
    color: $theme_color;
  }
}
table {
  font-size: 14px;
  margin-left: 40px;
  tr {
    font-size: 12px;
    height: 40px;
    padding: 10px;
    td:nth-child(1) {
      width: 120px;
    }
  }
}
.bill-detail-price {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding: 10px;
  > span {
    font-size: 14px;
    text-align: center;
    width: 200px;
    margin-bottom: 20px;
  }
  .increase-color {
    color: green;
    margin-top:5px
  }
  .theme_color {
    margin-top:5px
  }

}
</style>
