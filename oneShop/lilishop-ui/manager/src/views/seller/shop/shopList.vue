<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
        <Form-item label="会员名称" prop="memberName">
          <Input
            type="text"
            v-model="searchForm.memberName"
            placeholder="请输入会员名称"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="店铺名称" prop="storeName">
          <Input
            type="text"
            v-model="searchForm.storeName"
            placeholder="请输入店铺名称"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="店铺状态">
          <Select v-model="searchForm.storeDisable" clearable style="width: 200px">
            <Option value="OPEN">开启中</Option>
            <Option value="CLOSED">已关闭</Option>
            <Option value="APPLY">申请中</Option>
            <Option value="APPLYING">审核中</Option>
            <Option value="REFUSED">审核拒绝</Option>
          </Select>
        </Form-item>
        <Form-item label="创建时间" prop="createTime">
          <DatePicker v-model="selectDate" type="datetimerange" format="yyyy-MM-dd HH:mm:ss" clearable @on-change="selectDateRange" placeholder="选择起始时间" style="width: 200px"></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
      </Form>
      <Row class="operation padding-row">
        <Button @click="add" type="primary">添加</Button>
      </Row>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]"
          size="small" show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import {
  getShopListData,
  disableShop,
  enableBrand,
  shopAudit,
} from "@/api/shops";
export default {
  name: "shop",
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
      selectDate: null, // 创建时间
      columns: [
        // 表头
        {
          title: "店铺名称",
          key: "storeName",
          minWidth: 120,
          align: "left",
        },
        {
          title: "会员名称",
          key: "memberName",
          minWidth: 130,
          tooltip: true
        },
        {
          title: "店铺地址",
          key: "storeAddressPath",
          width: 300,
          tooltip: true,
          render: (h, params) => {
            return h(
              "Tag",
              {

              },
              params.row.storeAddressPath ||  "暂未填写"
            );
          },
        },
        {
          title: "是否自营",
          key: "selfOperated",
          align: "left",
          width: 120,
          render: (h, params) => {
            return h(
              "Tag",
              {
                props: {
                  color: params.row.selfOperated ? "volcano" : "green",
                },
              },
              params.row.selfOperated  ? "自营" : "非自营"
            );
          },
        },

        {
          title: "店铺状态",
          key: "storeDisable",
          align: "left",
          width: 130,
          render: (h, params) => {
            if (params.row.storeDisable == "OPEN") {
              return h("Tag", {props: {color: "green",},},"开启中");
            } else if (params.row.storeDisable == "CLOSED") {
              return h("Tag", {props: {color: "volcano",},},"已关闭");
            } else if (params.row.storeDisable == "APPLY") {
              return h("Tag", {props: {color: "geekblue",},},"申请中");
            } else if (params.row.storeDisable == "APPLYING") {
              return h("Tag", {props: {color: "purple",},},"审核中");
            } else if (params.row.storeDisable == "REFUSED") {
              return h("Tag", {props: {color: "red",},},"审核拒绝");
            }
          },
        },
        {
          title: "创建时间",
          key: "createTime",
          align: "left",
          width: 170
        },
        {
          title: "操作",
          key: "action",
          width: 200,
          align: "center",
          fixed: "right",
          render: (h, params) => {
            let enableOrDisable = "";
            if (params.row.storeDisable == "OPEN") {
              enableOrDisable = h(
                "Button",
                {
                  props: {
                    size: "small",
                    type: "error"
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.disable(params.row);
                    },
                  },
                },
                "关闭"
              );
            } else if (params.row.storeDisable == "CLOSED") {
              enableOrDisable = h(
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
                      this.enable(params.row);
                    },
                  },
                },
                "开启"
              );
            } else if (params.row.storeDisable == "APPLYING") {
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
                        this.audit(params.row);
                      },
                    },
                  },
                  "审核"
                ),
                h(
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
                        this.edit(params.row);
                      },
                    },
                  },
                  "修改"
                ),
              ]);
            }

            return h("div", [
              h(
                "Button",
                {
                  props: {
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                    display: this.selectedShop ? "inline-block" : "none",
                  },
                  on: {
                    click: () => {
                      this.callback(params.row);
                    },
                  },
                },
                "选择"
              ),
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
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small",
                    ghost:true
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.edit(params.row);
                    },
                  },
                },
                "修改"
              ),
              enableOrDisable,
            ]);
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
      selectedShop: false, //用于是否选择店铺
    };
  },

  methods: {
    // 回调给父级
    callback(val) {
      this.$emit("callback", val);
    },
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
    // 起止时间分别赋值
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    // 获取列表数据
    getDataList() {
      this.loading = true;
      // 带多条件搜索参数获取表单数据 请自行修改接口
      getShopListData(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    // 添加店铺
    add() {
      this.$router.push({ path: '/shop-operation'});
    },
    // 修改店铺
    edit(v) {
      this.$router.push({ path: '/shop-operation', query: { shopId: v.id } });
    },
    // 关闭店铺
    disable(v) {
      this.$Modal.confirm({
        title: "确认关闭",
        content: "您确认要关闭店铺 " + v.storeName + " ?",
        loading: true,
        onOk: () => {
          disableShop(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
      });
    },
    //查看店铺详细
    detail(row){
      this.$router.push({ name: "shop-detail", query: { id: row.id } });
    },
    // 审核店铺
    audit(v) {
      this.$Modal.confirm({
        title: "审核店铺",
        content: "您确认要审核通过店铺 " + v.storeName + " ?",
        okText: "通过",
        cancelText: "驳回",
        loading: true,
        onOk: () => {
          shopAudit(v.id, 0).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
        onCancel: () => {
          shopAudit(v.id, 1).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 启用店铺
    enable(v) {
      this.$Modal.confirm({
        title: "确认开启",
        content: "您确认要开启店铺 " + v.storeName + " ?",
        loading: true,
        onOk: () => {
          enableBrand(v.id).then((res) => {
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
