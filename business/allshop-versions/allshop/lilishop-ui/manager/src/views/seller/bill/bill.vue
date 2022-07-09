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
        <Form-item label="筛选状态">
          <Select v-model="searchForm.billStatus" clearable style="width:160px">
            <Option value="OUT">已出账</Option>
            <Option value="CHECK">已对账</Option>
            <Option value="EXAMINE">已审核</Option>
            <Option value="COMPLETE">已付款</Option>
          </Select>
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
      </Form>
      <Row class="operation padding-row">
       
      </Row>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table" sortable="custom" @on-selection-change="changeSelect">
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
        billStatus:"" //状态
      },
      selectDate: null, // 选择一个时间段
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
    init() {
      this.getDataList();
    },
    changePage(v) { // 改变页码
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    changePageSize(v) { // 改变页数
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    handleSearch() { // 搜索
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    changeSelect(e) { // 表格选中
      this.selectList = e;
      this.selectCount = e.length;
    },
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
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
    detail(v) { // 账单详情
      let id = v.id;
      this.$router.push({
        name: "bill-detail",
        query: { id: id },
      });
    },
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        // 记得确认修改此处
        content: "您确认要删除 " + v.name + " ?",
        loading: true,
        onOk: () => {
          // 删除
          this.deleteRequest("/bill/delByIds/" + v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
      });
    },
    delAll() { // 多选删除数据
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要删除的数据");
        return;
      }
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除所选的 " + this.selectCount + " 条数据?",
        loading: true,
        onOk: () => {
          let ids = "";
          this.selectList.forEach(function (e) {
            ids += e.id + ",";
          });
          ids = ids.substring(0, ids.length - 1);
          // 批量删除
          this.deleteRequest("/bill/delByIds/" + ids).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
