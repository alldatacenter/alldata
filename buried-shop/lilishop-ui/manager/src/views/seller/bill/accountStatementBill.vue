<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
        <Form-item label="账单编号" prop="sn">
          <Input type="text" v-model="searchForm.sn" placeholder="请输入账单编号" clearable style="width: 200px" />
        </Form-item>
        <Form-item label="出帐时间" prop="createTime">
          <DatePicker v-model="selectDate" type="daterange" format="yyyy-MM-dd HH:mm:ss" clearable @on-change="selectDateRange" placeholder="选择起始时间" style="width: 200px">
          </DatePicker>
        </Form-item>

        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
      </Form>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table" class="mt_10" @on-selection-change="changeSelect">
      </Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]"
          size="small" show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import * as API_Shop from "@/api/shops";

export default {
  name: "bill",
  components: {},
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
      },
      selectDate: null, // 选择一个事件段
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      columns: [
        // 表头
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "账单号",
          key: "sn",
          minWidth: 200,
          tooltip: true,
        },
        {
          title: "生成时间",
          key: "createTime",
          width: 120,
        },
        {
          title: "结算时间段",
          key: "startTime",
          width: 200,
          render: (h, params) => {
            return h("div", params.row.startTime + "~" + params.row.endTime);
          },
        },
        {
          title: "店铺名称",
          key: "storeName",
          minWidth: 120,
          tooltip: true,
        },

        {
          title: "结算金额",
          key: "billPrice",
          width: 130,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.billPrice, "￥")
            );
          },
        },
        {
          title: "状态",
          key: "billStatus",
          width: 100,
          render: (h, params) => {
            if (params.row.billStatus == "OUT") {
              return h("Tag", {props: {color: "blue",},},"已出账");
            } else if (params.row.billStatus == "CHECK") {
              return h("Tag", {props: {color: "geekblue",},},"已对账");
            } else if (params.row.billStatus == "EXAMINE") {
              return h("Tag", {props: {color: "purple",},},"已审核");
            } else {
              return h("Tag", {props: {color: "green",},},"已付款");
            }
          },
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          width: 120,
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.detail(params.row);
                    },
                  },
                },
                "详细"
              ),
            ]);
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
      this.clearSelectAll();
    },
    // 分页 改变页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.getDataList();
    },
    // 清除选中状态
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    // 选中回调
    changeSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
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

      this.searchForm.startTime &&
        (this.searchForm.startTime = this.$options.filters.unixToDate(
          this.searchForm.startTime / 1000
        ));
      this.searchForm.endTime &&
        (this.searchForm.endTime = this.$options.filters.unixToDate(
          this.searchForm.endTime / 1000
        ));
      this.searchForm.billStatus = "CHECK";
      API_Shop.getBuyBillPage(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    // 详情
    detail(v) {
      let id = v.id;
      this.$router.push({
        name: "bill-detail",
        query: { id: id },
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
/deep/ .ivu-col {
  min-height: 100vh;
}
</style>
