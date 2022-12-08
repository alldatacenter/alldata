
<template>
  <div class="search">
    <Card>
      <Row @keydown.enter.native="handleSearch">
        <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
          <Form-item label="会员名称" prop="memberName">
            <Input type="text" v-model="searchForm.memberName" placeholder="请输入会员名称" clearable style="width: 200px" />
          </Form-item>
          <Form-item label="支付时间">
            <DatePicker v-model="selectDate" type="datetimerange" format="yyyy-MM-dd HH:mm:ss" clearable @on-change="selectDateRange" placeholder="选择起始时间" style="width: 200px"></DatePicker>
          </Form-item>
          <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
        </Form>
      </Row>
        <Table class="mt_10" :loading="loading" border :columns="columns" :data="data" ref="table"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]"
          size="small" show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import { getUserWallet } from "@/api/member";
export default {
  name: "walletLog",
  data() {
    return {
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        startDate: "", // 起始时间
        endDate: "", // 终止时间
        memberName: "",
      },
      selectDate: null, // 选择时间段
      columns: [
        // 表头
        {
          title: "会员名称",
          key: "memberName",
          minWidth: 100,
        },
        {
          title: "变动金额",
          key: "money",
          width: 150,
          render: (h, params) => {
            if (params.row.money > 0) {
              return h("div", [
                h(
                  "span",
                  {
                    style: {
                      color: "green",
                    },
                  },
                  this.$options.filters.unitPrice(params.row.money, "￥")
                ),
              ]);
            } else if (params.row.money < 0) {
              return h("div", [
                h(
                  "span",
                  {
                    style: {
                      color: "red",
                    },
                  },
                  this.$options.filters.unitPrice(-params.row.money, "- ￥")
                ),
              ]);
            }
          },
        },

        {
          title: "变更时间",
          key: "createTime",
          width: 200,
        },
        {
          title: "业务类型",
          key: "serviceType",
          width: 200,
          render: (h, params) => {
            if (params.row.serviceType == "WALLET_WITHDRAWAL") {
              return h("div", [h("span", {}, "余额提现")]);
            } else if (params.row.serviceType == "WALLET_PAY") {
              return h("div", [h("span", {}, "余额支付")]);
            } else if (params.row.serviceType == "WALLET_REFUND") {
              return h("div", [h("span", {}, "余额退款")]);
            } else if (params.row.serviceType == "WALLET_RECHARGE") {
              return h("div", [h("span", {}, "余额充值")]);
            } else {
              return h("div", [h("span", {}, "佣金提成")]);
            }
          },
        },
        {
          title: "详细",
          key: "detail",
          minWidth: 300,
          tooltip: true,
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
    // 时间段赋值
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    // 获取列表数据
    getDataList() {
      getUserWallet(this.searchForm).then((res) => {
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
};
</script>
