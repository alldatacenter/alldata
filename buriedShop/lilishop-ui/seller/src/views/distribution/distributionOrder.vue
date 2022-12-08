<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70"  @keydown.enter.native="handleSearch" class="search-form">
        <Form-item label="订单编号" prop="orderSn">
          <Input type="text" v-model="searchForm.orderSn" placeholder="请输入订单编号" clearable style="width: 200px" />
        </Form-item>
        <Form-item label="订单时间">
          <DatePicker type="daterange" v-model="timeRange" format="yyyy-MM-dd" placeholder="选择时间" style="width: 210px"></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
      </Form>

      <Table class="mt_10" :loading="loading" border :columns="columns" :data="data" ref="table"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10,20,50]" size="small"
          show-total show-elevator show-sizer></Page>
      </Row>
    </Card>

  </div>
</template>

<script>
import { getDistributionOrder } from "@/api/distribution";
import { orderStatusList } from "./dataJson";

export default {
  name: "distributionOrder",
  components: {},
  data() {
    return {
      timeRange: [], // 范围时间
      orderStatusList, // 订单状态列表
      distributionId: this.$route.query.id, // 分销id
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort:"create_time",
        order:"desc"
      },
      columns: [
        {
          title: "订单编号",
          key: "orderSn",
          minWidth: 120,
          tooltip: true,
        },


        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "状态",
          key: "distributionOrderStatus",
          width: 100,
          sortable: false,
          render: (h, params) => {
            if (params.row.distributionOrderStatus == "COMPLETE_CASH") {
              return h("Tag", { props: { color: "green" } },"提现完成");
            } else if (params.row.distributionOrderStatus == "WAIT_BILL") {
              return h("Tag", { props: { color: "blue" } } ,"待结算");
            } else if (params.row.distributionOrderStatus == "WAIT_CASH") {
              return h("Tag", { props: { color: "orange" } }, "待提现");
            } else if (params.row.distributionOrderStatus == "CANCEL") {
              return h("Tag", { props: { color: "red" } }, "订单已取消");
            }else if (params.row.distributionOrderStatus == "REFUND") {
              return h("Tag", { props: { color: "magenta" } }, "退款");
            }

          },
        },

        {
          title: "佣金金额",
          key: "rebate",
          width: 120,
          sortable: false,
          render: (h, params) => {
            if (params.row.rebate == null) {
              return h("div", this.$options.filters.unitPrice(0, "￥"));
            } else {
              return h(
                "div",
                this.$options.filters.unitPrice(params.row.rebate, "￥")
              );
            }
          },
        },
        {
          title: "创建时间",
          key: "createTime",
          width: 180,
          sortable: false,
        },
        {
          title: "解冻日期（T+1）",
          key: "settleCycle",
          width: 180,
          sortable: false,
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  methods: {
    init() { // 初始化数据
      this.getDataList();
    },
    // 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    // 改变页数
    changePageSize(v) {
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 获取订单数据
    getDataList() {
      this.searchForm.distributionId = this.distributionId;
      this.loading = true;
      if (this.timeRange && this.timeRange[0]) {
        let startTime = this.timeRange[0];
        let endTime = this.timeRange[1];
        this.searchForm.startTime = this.$options.filters.unixToDate(
          startTime / 1000
        );
        this.searchForm.endTime = this.$options.filters.unixToDate(
          endTime / 1000
        );
      }
      console.log(this.searchForm);
      // 带多条件搜索参数获取表单数据 请自行修改接口
      getDistributionOrder(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;

          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
  },
  mounted() {
    this.init();
  },
  watch: {
    $route(e) {
      this.distributionId = e.query.id ? e.query.id : undefined;
      this.getDataList();
    },
  },
};
</script>
<style lang="scss" >
@import "@/styles/table-common.scss";
</style>

