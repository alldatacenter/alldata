<template>
  <div class="search">
    <Card>
      <Row @keydown.enter.native="handleSearch">
        <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
          <Form-item label="会员名称" prop="memberName">
            <Input type="text" v-model="searchForm.memberName" placeholder="请输入会员名称" clearable style="width: 200px" />
          </Form-item>
          <Form-item label="审核状态" prop="applyStatus">
            <Select v-model="searchForm.applyStatus" clearable style="width: 200px">
              <Option value="APPLY">申请中</Option>
              <Option value="VIA_AUDITING">审核通过(提现成功)</Option>
              <Option value="FAIL_AUDITING">审核拒绝</Option>
            </Select>
          </Form-item>
          <Form-item label="申请时间">
            <DatePicker v-model="selectDate" type="datetimerange" format="yyyy-MM-dd HH:mm:ss" clearable @on-change="selectDateRange" placeholder="选择起始时间" style="width: 200px"></DatePicker>
          </Form-item>
          <Form-item style="margin-left: -35px" class="br">
            <Button @click="handleSearch" type="primary" icon="ios-search">搜索
            </Button>
          </Form-item>
        </Form>
      </Row>
      <Table class="mt_10" :loading="loading" border :columns="columns" :data="data" ref="table" sortable="custom" @on-sort-change="changeSort" @on-selection-change="changeSelect"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]" size="small"
          show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
    <Modal :title="modalTitle" v-model="roleModalVisible" :mask-closable="false" :width="500">
      <Form :label-width="80">
        <FormItem label="申请编号">
          <span>{{showList.sn}}</span>
        </FormItem>
        <FormItem label="用户名称">
          <span>{{showList.memberName}}</span>
        </FormItem>
        <FormItem label="申请金额">
          <span>{{showList.applyMoney | unitPrice}}</span>
        </FormItem>
        <FormItem label="提现状态">
          <span>{{showList.applyStatus | paramTypeFilter}}</span>
        </FormItem>
        <FormItem label="申请时间">
          <span>{{showList.createTime}}</span>
        </FormItem>
        <FormItem label="审核备注">
          <Input v-model="audit" />
        </FormItem>

      </Form>
      <div slot="footer" v-if="showList.applyStatus == 'APPLY'">
        <Button type="text" @click="submitRole(false)">拒绝</Button>
        <Button type="primary" :loading="submitLoading" @click="submitRole(true)">通过
        </Button>
      </div>
    </Modal>

    <Modal :title="modalTitle" v-model="queryModalVisible" :mask-closable="false" :width="500">
      <Form :label-width="80">
        <FormItem label="申请编号：">
          <span>{{showList.sn}}</span>
        </FormItem>
        <FormItem label="用户名称：">
          <span>{{showList.memberName}}</span>
        </FormItem>
        <FormItem label="申请金额：">
          <span>{{showList.applyMoney}}</span>
        </FormItem>
        <FormItem label="提现状态：">
          <span>{{showList.applyStatus | paramTypeFilter}}</span>
        </FormItem>
        <FormItem label="申请时间：">
          <span>{{showList.createTime}}</span>
        </FormItem>
        <FormItem label="审核时间：">
          <span>{{showList.inspectTime}}</span>
        </FormItem>
        <FormItem label="审核备注：">
          <span>{{showList.inspectRemark}}</span>
        </FormItem>

      </Form>
      <div slot="footer" v-if="showList.applyStatus == 'APPLY'">
        <Button type="text" @click="submitRole(false)">拒绝</Button>
        <Button type="primary" :loading="submitLoading" @click="submitRole(true)">通过
        </Button>
      </div>
      <div slot="footer" v-else>
        <Button type="text" @click="queryModalVisible = false">取消</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import { getUserWithdrawApply, withdrawApply } from "@/api/member";

export default {
  name: "withdrawApply",
  components: {},
  data() {
    return {
      modalTitle: "", //弹出框标题
      openSearch: true, // 显示搜索
      openTip: true, // 显示提示
      loading: true, // 表单加载状态
      audit: "", // 审核备注
      roleModalVisible: false, // 审核模态框
      queryModalVisible: false, // 审核模态框
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        startDate: "", // 起始时间
        endDate: "", // 终止时间
        memberName: "",
        applyStatus: "",
      },
      selectDate: null, // 选择时间段
      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      showList: {}, // 可操作选项
      columns: [
        {
          title: "申请编号",
          key: "sn",
          align: "left",
          tooltip: true,
        },
        {
          title: "用户名称",
          key: "memberName",
          align: "left",
          tooltip: true,
        },
        {
          title: "申请金额",
          key: "applyMoney",
          align: "left",
          width: 120,
          render: (h, params) => {
            return h("div", [
              h(
                "span",
                {},
                this.$options.filters.unitPrice(params.row.applyMoney)
              ),
            ]);
          },
        },
        {
          title: "提现状态",
          align: "left",
          key: "applyStatus",
          width: 120,
          render: (h, params) => {
            if (params.row.applyStatus == "APPLY") {
              return h("Tag", { props: { color: "volcano" } }, "申请中");
            } else if (params.row.applyStatus == "VIA_AUDITING") {
              return h("Tag", { props: { color: "green" } }, "审核通过");
            } else if (params.row.applyStatus == "SUCCESS") {
              return h("Tag", { props: { color: "blue" } }, "提现成功");
            } else {
              return h("Tag", { props: { color: "red" } }, "审核拒绝");
            }
          }
        },
        {
          title: "申请时间",
          key: "createTime",
          align: "left",
          width: 170,
        },
        {
          title: "审核时间",
          key: "inspectTime",
          align: "left",
          width: 170,
        },
        {
          title: "操作",
          key: "action",
          width: 120,
          align: "center",
          fixed: "right",
          render: (h, params) => {
            if (params.row.applyStatus == "APPLY") {
              return h(
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
                      this.showList = {};
                      this.roleModalVisible = true;
                      this.showList = params.row;
                      this.audit =""
                    },
                  },
                },
                "审核"
              );
            } else {
              return h(
                "Button",
                {
                  props: {
                    type: "primary",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.showList = {};
                      this.queryModalVisible = true;
                      this.showList = params.row;
                      this.modalTitle = "查看";
                    },
                  },
                },
                "查看"
              );
            }
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  filters: {
    paramTypeFilter(val) {
      if (val === "APPLY") {
        return "申请中";
      } else if (val === "VIA_AUDITING") {
        return "审核通过(提现成功)";
      } else if (val === "FAIL_AUDITING") {
        return "审核拒绝";
      } else {
        return "未知状态";
      }
    },
  },
  methods: {
    submitRole(res) {
      const params = {};
      params.applyId = this.showList.id;
      params.result = res;
      params.remark = this.audit;
      if (res === false && params.remark === "") {
        this.$Message.error("审核备注不能为空");
        return;
      }
      withdrawApply(params).then((res) => {
        this.loading = false;
        if (res == true) {
          this.$Message.success("操作成功");
          this.roleModalVisible = false;
          this.getDataList();
        }
      });
    },
    init() {
      this.getDataList();
    },
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },
    changePageSize(v) {
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    handleReset() {
      this.$refs.searchForm.resetFields();
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.selectDate = null;
      this.searchForm.startDate = "";
      this.searchForm.endDate = "";
      this.searchForm.memberName = "";
      // 重新加载数据
      this.getDataList();
    },
    changeSort(e) {
      this.searchForm.sort = e.key;
      this.searchForm.order = e.order;
      if (e.order === "normal") {
        this.searchForm.order = "";
      }
      this.getDataList();
    },
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    changeSelect(e) {
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
      // 带多条件搜索参数获取表单数据 请自行修改接口
      getUserWithdrawApply(this.searchForm).then((res) => {
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

