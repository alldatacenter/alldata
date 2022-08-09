<template>
  <div class="search">
    <Card>
      <Row @keydown.enter.native="handleSearch">
        <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
          <Form-item label="订单编号" prop="orderSn">
            <Input
              type="text"
              v-model="searchForm.orderSn"
              placeholder="请输入订单编号"
              clearable
              style="width: 200px"
            />
          </Form-item>
          <Form-item label="会员名称" prop="memberName">
            <Input
              type="text"
              v-model="searchForm.memberName"
              placeholder="请输入会员名称"
              clearable
              style="width: 200px"
            />
          </Form-item>
          <Form-item label="状态" prop="status">
            <Select v-model="searchForm.status" placeholder="请选择" clearable style="width: 200px">
              <Option value="NEW">新投诉</Option>
              <Option value="CANCEL">已撤销</Option>
              <Option value="WAIT_APPEAL">待申诉</Option>
              <Option value="COMMUNICATION">对话中</Option>
              <Option value="WAIT_ARBITRATION">等待仲裁</Option>
              <Option value="COMPLETE">已完成</Option>
            </Select>
          </Form-item>
          <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
        </Form>
      </Row>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
      >
        <template slot-scope="{row}" slot="goodsName">
          <a class="mr_10" @click="linkTo(row.goodsId,row.skuId)">{{row.goodsName}}</a>
          <Poptip trigger="hover" title="扫码在手机中查看" transfer>
            <div slot="content">
              <vue-qr :text="wapLinkTo(row.goodsId,row.skuId)"  :margin="0" colorDark="#000" colorLight="#fff" :size="150"></vue-qr>
            </div>
            <img src="../../../assets/qrcode.svg" style="vertical-align:bottom;" class="hover-pointer" width="20" height="20" alt="">
          </Poptip>
        </template>
      </Table>
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
  import vueQr from 'vue-qr'
  export default {
    name: "orderComplaint",
      components: {
      "vue-qr":vueQr
    },
    data() {
      return {
        loading: true, // 表单加载状态
        searchForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
          sort: "createTime", // 默认排序字段
          order: "desc", // 默认排序方式
        },
        columns: [
          // 表头
          {
            title: "会员名称",
            key: "memberName",
            width: 200,
            sortable: false,
          },
          {
            title: "订单编号",
            key: "orderSn",
            minWidth: 120,
            tooltip: true

          },
          {
            title: "商品名称",
            slot: "goodsName",
            minWidth: 170,
            tooltip: true

          },
          {
            title: "投诉主题",
            key: "complainTopic",
            tooltip: true
          },
          {
            title: "投诉时间",
            key: "createTime",
            width: 180,
          },
          {
            title: "投诉状态",
            key: "complainStatus",
            width: 100,
            render: (h, params) => {
              if (params.row.complainStatus == "NEW") {
                return h('div', [h('tag',{props: {color: "purple"}}, '新投诉'),]);
              } else if (params.row.complainStatus == "CANCEL") {
                return h('div', [h('tag', {props: {color: "cyan"}}, '已撤销'),]);
              } else if (params.row.complainStatus == "WAIT_APPEAL") {
                return h('div', [h('tag', {props: {color: "volcano"}}, '待申诉'),]);
              } else if (params.row.complainStatus == "COMMUNICATION") {
                return h('div', [h('tag', {props: {color: "orange"}}, '对话中'),]);
              }else if (params.row.complainStatus == "WAIT_ARBITRATION") {
                return h('div', [h('tag', {props: {color: "blue"}}, '等待仲裁'),]);
              }else if (params.row.complainStatus == "COMPLETE") {
                return h('div', [h('tag', {props: {color: "green"}}, '已完成'),]);
              }
            }
          },

          {
            title: "操作",
            key: "action",
            align: "center",
            fixed: "right",
            width: 150,
            render: (h, params) => {
              if(params.row.complainStatus === "COMPLETE"){
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
                    "详情"
                  ),
                ]);
              }else{
                return h("div", [
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
                          this.detail(params.row);
                        },
                      },
                    },
                    "处理"
                  ),
                ]);
              }

            },
          },

        ],
        data: [], // 表单数据
        total: 0, // 表单数据总数
      };
    },
    watch: {
      $route(){
        this.getDataList();
      }
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
      // 获取列表数据
      getDataList() {
        this.loading = true;
        API_Order.getOrderComplain(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total;
          }
        });
        this.total = this.data.length;
        this.loading = false;
      },
      //投诉详情
      detail(v) {
        let id = v.id;
        this.$router.push({
          name: "order-complaint-detail",
          query: { id: id },
        });
      },
    },
    mounted() {
      this.init();
    },
  };
</script>
