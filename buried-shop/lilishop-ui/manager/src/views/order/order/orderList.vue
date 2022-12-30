<template>
  <div>
    <Card>
      <Form
        ref="searchForm"
        @keydown.enter.native="handleSearch"
        :model="searchForm"
        inline
        :label-width="70"
        class="search-form"
      >
        <Form-item label="订单号" prop="orderSn">
          <Input
            type="text"
            v-model="searchForm.orderSn"
            placeholder="请输入订单号"
            clearable
            style="width: 160px"
          />
        </Form-item>
        <Form-item label="会员名称" prop="buyerName">
          <Input
            type="text"
            v-model="searchForm.buyerName"
            placeholder="请输入会员名称"
            clearable
            style="width: 160px"
          />
        </Form-item>

        <Form-item label="订单类型" prop="orderType">
          <Select
            v-model="searchForm.orderPromotionType"
            placeholder="请选择"
            clearable
            style="width: 160px"
          >
            <Option value="NORMAL">普通订单</Option>
            <Option value="PINTUAN">拼团订单</Option>
            <Option value="GIFT">赠品订单</Option>
            <Option value="POINTS">积分订单</Option>
            <Option value="KANJIA">砍价订单</Option>
          </Select>
        </Form-item>
        <Form-item label="下单时间">
          <DatePicker
            v-model="selectDate"
            type="datetimerange"
            format="yyyy-MM-dd"
            clearable
            @on-change="selectDateRange"
            placeholder="选择起始时间"
            style="width: 160px"
          ></DatePicker>
        </Form-item>
        <Form-item label="订单状态" prop="orderStatus">
          <Select
            v-model="searchForm.orderStatus"
            placeholder="请选择"
            clearable
            style="width: 160px"
          >
            <Option value="UNPAID">未付款</Option>
            <Option value="PAID">已付款</Option>
            <Option value="UNDELIVERED">待发货</Option>
            <Option value="DELIVERED">已发货</Option>
            <Option value="COMPLETED">已完成</Option>
            <Option value="TAKE">待核验</Option>
            <Option value="CANCELLED">已取消</Option>
          </Select>
        </Form-item>
        <Button
          @click="handleSearch"
          type="primary"
          icon="ios-search"
          class="search-btn"
          >搜索</Button
        >
      </Form>
      <div>
        <download-excel
          class="export-excel-wrapper"
          :data="data"
          :fields="fields"
          :fetch="exportOrder"
          name="商品订单.xls"
        >
          <Button type="info" class="export"> 导出订单 </Button>
        </download-excel>
      </div>

      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        sortable="custom"
      ></Table>

      <Row type="flex" justify="end" class="mt_10">
        <Page
          :current="searchForm.pageNumber"
          :total="total"
          :page-size="searchForm.pageSize"
          @on-change="changePage"
          @on-page-size-change="changePageSize"
          :page-size-opts="[10, 20, 50]"
          size="small"
          show-total
          show-elevator
          show-sizer
        ></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import * as API_Order from "@/api/order";
import JsonExcel from "vue-json-excel";
export default {
  name: "orderList",
  components: {
    "download-excel": JsonExcel,
  },
  data() {
    return {
      // 表格的表头以及内容
      fields: {
        订单编号: "sn",
        下单时间: "createTime",
        客户名称: "memberName",
        支付方式: {
          field: "clientType",
          callback: (value) => {
            if (value == "H5") {
              return "移动端";
            } else if (value == "PC") {
              return "PC端";
            } else if (value == "WECHAT_MP") {
              return "小程序端";
            } else if (value == "APP") {
              return "移动应用端";
            } else {
              return value;
            }
          },
        },
        商品数量: "groupNum",
        付款状态: {
          field: "payStatus",
          callback: (value) => {
            return value == "UNPAID"
              ? "未付款"
              : value == "PAID"
              ? "已付款"
              : "";
          },
        },
        店铺: "storeName",
      },
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        startDate: "", // 起始时间
        endDate: "", // 终止时间
        orderType: "",
        orderSn: "",
        buyerName: "",
        orderStatus: "",
      },
      selectDate: null,
      columns: [
        {
          title: "订单号",
          key: "sn",
          minWidth: 240,
          tooltip: true,
        },

        {
          title: "订单来源",
          key: "clientType",
          width: 120,
          render: (h, params) => {
            if (params.row.clientType == "H5") {
              return h("div", {}, "移动端");
            } else if (params.row.clientType == "PC") {
              return h("div", {}, "PC端");
            } else if (params.row.clientType == "WECHAT_MP") {
              return h("div", {}, "小程序端");
            } else if (params.row.clientType == "APP") {
              return h("div", {}, "移动应用端");
            } else {
              return h("div", {}, params.row.clientType);
            }
          },
        },
        {
          title: "订单类型",
          key: "orderPromotionType",
          width: 120,
          render: (h, params) => {
            if (params.row.orderPromotionType == "NORMAL") {
              return h("div", [
                h("tag", { props: { color: "blue" } }, "普通订单"),
              ]);
            } else if (params.row.orderPromotionType == "PINTUAN") {
              return h("div", [
                h("tag", { props: { color: "volcano" } }, "拼团订单"),
              ]);
            } else if (params.row.orderPromotionType == "GIFT") {
              return h("div", [
                h("tag", { props: { color: "green" } }, "赠品订单"),
              ]);
            } else if (params.row.orderPromotionType == "POINTS") {
              return h("div", [
                h("tag", { props: { color: "geekblue" } }, "积分订单"),
              ]);
            } else if (params.row.orderPromotionType == "KANJIA") {
              return h("div", [
                h("tag", { props: { color: "pink" } }, "砍价订单"),
              ]);
            }
          },
        },
        {
          title: "买家名称",
          key: "memberName",
          minWidth: 130,
          tooltip: true,
        },

        {
          title: "订单金额",
          key: "flowPrice",
          minWidth: 100,
          tooltip: true,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.flowPrice, "￥")
            );
          },
        },

        {
          title: "订单状态",
          key: "orderStatus",
          minWidth: 100,
          render: (h, params) => {
            if (params.row.orderStatus == "UNPAID") {
              return h("div", [
                h("tag", { props: { color: "magenta" } }, "未付款"),
              ]);
            } else if (params.row.orderStatus == "PAID") {
              return h("div", [
                h("tag", { props: { color: "blue" } }, "已付款"),
              ]);
            } else if (params.row.orderStatus == "UNDELIVERED") {
              return h("div", [
                h("tag", { props: { color: "geekblue" } }, "待发货"),
              ]);
            } else if (params.row.orderStatus == "DELIVERED") {
              return h("div", [
                h("tag", { props: { color: "cyan" } }, "已发货"),
              ]);
            } else if (params.row.orderStatus == "COMPLETED") {
              return h("div", [
                h("tag", { props: { color: "green" } }, "已完成"),
              ]);
            } else if (params.row.orderStatus == "TAKE") {
              return h("div", [
                h("tag", { props: { color: "volcano" } }, "待核验"),
              ]);
            } else if (params.row.orderStatus == "CANCELLED") {
              return h("div", [
                h("tag", { props: { color: "red" } }, "已取消"),
              ]);
            }
          },
        },
        {
          title: "下单时间",
          key: "createTime",
          width: 170,
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          width: 100,
          render: (h, params) => {
            return h(
              "Button",
              {
                props: { type: "info", size: "small" },
                style: { marginRight: "5px" },
                on: {
                  click: () => {
                    this.detail(params.row);
                  },
                },
              },
              "查看"
            );
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
    },
    // 分页 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    // 分页 改变页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 起止时间从新赋值
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    // 获取列表数据
    getDataList() {
      this.loading = true;
      API_Order.getOrderList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    // 跳转详情页面
    detail(v) {
      let sn = v.sn;
      this.$router.push({
        name: "order-detail",
        query: { sn: sn },
      });
    },
    // 导出订单
    async exportOrder() {
      const params = JSON.parse(JSON.stringify(this.searchForm));
      params.pageNumber = 1;
      params.pageSize = 10000;
      const result = await API_Order.getOrderList(params);
      if (result.success) {
        if (result.result.records.length === 0) {
          this.$Message.warning("暂无待发货订单");
          return [];
        } else {
          return result.result.records;
        }
      } else {
        this.$Message.warning("导出订单失败，请重试");
      }
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.export {
  margin: 10px 20px 10px 0;
}
.export-excel-wrapper {
  display: inline;
}
</style>
