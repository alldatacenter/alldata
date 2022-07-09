<template>
  <div class="search">
    <Card>
      <Form
        ref="searchForm"
        :model="searchForm"
        inline
        :label-width="70"
        class="search-form"
      >
        <Form-item label="会员名称" prop="memberName">
          <Input
            type="text"
            v-model="searchForm.memberName"
            placeholder="请输入会员名称"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="充值单号" prop="rechargeSn">
          <Input
            type="text"
            v-model="searchForm.rechargeSn"
            placeholder="请输入充值单号"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="支付时间">
          <DatePicker
            v-model="selectDate"
            type="datetimerange"
            format="yyyy-MM-dd HH:mm:ss"
            clearable
            @on-change="selectDateRange"
            placeholder="选择起始时间"
            style="width: 200px"
          ></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
      </Form>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
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
  import {
    getUserRecharge,
  } from "@/api/member";
  export default {
    name: "recharge",
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
          memberName:""
        },
        selectDate: null, // 选择区间时间
        columns: [
          {
            title: "会员名称",
            key: "memberName",
            minWidth: 120,
            tooltip: true
          },
          {
            title: "订单号",
            key: "rechargeSn",
            minWidth: 180,
            tooltip: true
          },
          {
            title: "充值金额",
            key: "rechargeMoney",
            width: 160,
            sortable: true,
            render: (h, params) => {
              return h(
                "div",
                this.$options.filters.unitPrice(params.row.rechargeMoney, "￥")
              );
            },
          },
          {
            title: "充值方式",
            key: "rechargeWay",
            width: 120,
            render: (h, params) => {
              if (params.row.rechargeWay === 'ALIPAY') {
                return h('div', [h('span', {}, '支付宝')]);
              } else if (params.row.rechargeWay === 'WECHAT') {
                return h('div', [h('span', {}, '微信')]);
              } else if (params.row.rechargeWay === 'BANK_TRANSFER') {
                return h('div', [h('span', {}, '线下转账')]);
              } else {
                return h('div', [h('span', {}, '')]);
              }
            }
          },
          {
            title: "支付状态",
            key: "payStatus",
            align: "left",
            width: 120,
            sortable: false,
            render: (h, params) => {
              if (params.row.payStatus == "PAID") {
                return h("Tag", {props: {color: "green",},}, "已付款");
              } else {
                return h("Tag", {props: {color: "red",},}, "未付款");
              }
            },
          },
          {
            title: "充值时间",
            key: "createTime",
            align: "left",
            width: 190,
            sortable: false,
          },
          {
            title: "支付时间",
            key: "payTime",
            align: "left",
            width: 190,
            sortable: false,
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
        this.loading = true;
        getUserRecharge(this.searchForm).then((res) => {
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