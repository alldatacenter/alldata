<template>
  <div>
    <Card>
      <Row @keydown.enter.native="handleSearch">
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
          <Button
            @click="handleSearch"
            type="primary"
            class="search-btn"
            >搜索</Button
          >
        </Form>
      </Row>
      <Table
        class="mt_10"
        :loading="loading"
        border
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
import { getDistributionListData, auditDistribution } from "@/api/distribution";

export default {
  name: "distributionApply",
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
          title: "会员名称",
          key: "memberName",
          minWidth: 150,
          tooltip: true,
        },
        {
          title: "会员姓名",
          key: "name",
          minWidth: 120,
        },
        {
          title: "提交时间",
          key: "createTime",
          minWidth: 150,
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          width: 200,
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "success",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.audit(params.row, "PASS");
                    },
                  },
                },
                "通过"
              ),
              h(
                "Button",
                {
                  props: {
                    type: "error",
                    size: "small",
                  },
                  on: {
                    click: () => {
                      this.audit(params.row, "REFUSE");
                    },
                  },
                },
                "拒绝"
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
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 获取列表数据
    getDataList() {
      this.loading = true;
      this.searchForm.distributionStatus = "APPLY";
      getDistributionListData(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    //审核
    audit(v, status) {
      let test = "拒绝";
      if (status == "PASS") {
        test = "通过";
      }
      let params = {
        status: status,
      };
      this.$Modal.confirm({
        title: "确认" + test,
        // 记得确认修改此处
        content: "您确认要" + test + " " + v.memberName + " ?",
        loading: true,
        onOk: () => {
          auditDistribution(v.id, params).then((res) => {
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
