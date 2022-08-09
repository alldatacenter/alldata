<template>
  <div class="search">
    <Card>
      <Row @keydown.enter.native="handleSearch">
        <Form
          ref="searchForm"
          :model="searchForm"
          inline
          :label-width="70"
          class="search-form"
        >
          <Form-item label="开始时间" prop="startDay">
            <DatePicker
              type="date"
              v-model="searchForm.startDate"
              format="yyyy-MM-dd HH:mm:ss"
              placeholder="请选择"
              clearable
              style="width: 200px"
            ></DatePicker>
          </Form-item>
          <Form-item label="结束时间" prop="endDate">
            <DatePicker
              type="date"
              v-model="searchForm.endDate"
              format="yyyy-MM-dd HH:mm:ss"
              di
              placeholder="请选择"
              clearable
              style="width: 200px"
            ></DatePicker>
          </Form-item>
          <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
          <Button @click="handleReset" class="search-btn">重置</Button>
        </Form>
      </Row>
      <Table
        :loading="loading"
        border
        class="mt_10"
        :columns="columns"
        :data="data"
        ref="table"
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
  import * as API_Shop from "@/api/shops";

  export default {
    name: "accountStatementBill",
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
        columns: [
          {
            title: "账单号",
            key: "sn",
            minWidth: 250,
            tooltip: true          
          },
          {
            title: "生成时间",
            key: "createTime",
            minWidth: 120,
          },
          {
            title: "结算时间段",
            key: "startTime",
            width: 200,
            tooltip: true,
            render: (h, params) => {
              return h('div', params.row.startTime +"~"+params.row.endTime)
            }
          },
          {
            title: "结算金额",
            key: "billPrice",
            minWidth: 100,
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
            }
          },

          {
            title: "操作",
            key: "action",
            align: "center",
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
                  "查看"
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
      },
      // 分页 改变页数
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
      // 重置
      handleReset() {
        this.searchForm = {}
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = 10;
        this.getDataList();
      },
      // 获取列表数据
      getDataList() {
        this.loading = true;
        this.searchForm.billStatus = "OUT"
        API_Shop.getBillPage(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total;
          }
        });
        this.total = this.data.length;
        this.loading = false;
      },
      // 查看详情
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
    // 页面缓存处理，从该页面离开时，修改KeepAlive为false，保证进入该页面是刷新
    beforeRouteLeave(to, from, next) {
      from.meta.keepAlive = false
      next()
    }
  };
</script>
<style lang="scss" scoped>
  // 建议引入通用样式 可删除下面样式代码
   @import "@/styles/table-common.scss";
   /deep/ .ivu-col{
     min-height: 100vh;
   }
</style>
