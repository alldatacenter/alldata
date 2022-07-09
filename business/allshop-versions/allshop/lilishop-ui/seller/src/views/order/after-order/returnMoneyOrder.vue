<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
        <Form-item label="商品" prop="goodsName">
          <Input
            type="text"
            v-model="searchForm.goodsName"
            clearable
            placeholder="请输入商品名称"
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="会员名称" prop="memberName">
          <Input
            type="text"
            v-model="searchForm.memberName"
            clearable
            placeholder="请输入会员名称"
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="订单编号" prop="orderSn">
          <Input
            type="text"
            v-model="searchForm.orderSn"
            clearable
            placeholder="请输入订单编号"
            style="width: 200px"
          />
        </Form-item>

        <Form-item label="申请时间">
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
        <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
        <Button @click="handleReset" class="search-btn">重置</Button>
      </Form>

      <Table
        :loading="loading"
        border
        class="mt_10"
        :columns="columns"
        :data="data"

        ref="table"
      >

        <!-- 商品栏目格式化 -->
        <template slot="goodsSlot" slot-scope="{row}">
          <div style="margin-top: 5px;height: 90px; display: flex;">
            <div style="">
              <img :src="row.goodsImage" style="height: 80px;margin-top: 3px">
            </div>

            <div style="margin-left: 13px;">
              <div class="div-zoom">
                <a @click="linkTo(row.goodsId,row.skuId)">{{row.goodsName}}</a>
              </div>
              <Poptip trigger="hover" title="扫码在手机中查看" transfer>
                <div slot="content">
                  <vue-qr :text="wapLinkTo(row.goodsId,row.skuId)"  :margin="0" colorDark="#000" colorLight="#fff" :size="150"></vue-qr>
                </div>
                <img src="../../../assets/qrcode.svg" class="hover-pointer" width="20" height="20" alt="">
              </Poptip>
            </div>
          </div>

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

  export default {
    name: "returnMoneyOrder",
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
          serviceType:"RETURN_MONEY",
          orderSn:"",
          memberName:"",
          goodsName:""
        },
        selectDate: null,
        submitLoading: false, // 添加或编辑提交状态
        columns: [
          // 表头
          {
            title: "退款编号",
            key: "sn",
            minWidth: 150,
          },
          {
            title: "订单号",
            key: "orderSn",
            minWidth: 150,
          },

          {
            title: "商品",
            key: "sn",
            minWidth: 250,
            sortable: false,
            slot: "goodsSlot",
          },
          {
            title: "申请退款金额",
            key: "applyRefundPrice",
            width: 130,

            render: (h, params) => {
              return h(
                "div",
                this.$options.filters.unitPrice(params.row.applyRefundPrice, "￥")
              );
            },
          },
          {
            title: "会员",
            key: "memberName",
            minWidth: 130,
            tooltip: true
          },
          {
            title: "申请时间",
            key: "createTime",
            width: 170
          },
          {
            title: "售后状态",
            key: "serviceStatus",
            minWidth: 120,
            render: (h, params) => {
              if (params.row.serviceStatus == "APPLY") {
                return h('div', [h('tag', {props: {color: "blue"}}, '申请中'),]);
              } else if (params.row.serviceStatus == "PASS") {
                return h('div', [h('tag', {props: {color: "cyan"}}, '通过售后'),]);
              } else if (params.row.serviceStatus == "REFUSE") {
                return h('div', [h('tag', {props: {color: "volcano"}}, '拒绝售后'),]);
              } else if (params.row.serviceStatus == "BUYER_CANCEL") {
                return h('div', [h('tag', {props: {color: "purple"}}, '买家取消售后'),]);
              } else if (params.row.serviceStatus == "COMPLETE") {
                return h('div', [h('tag', {props: {color: "green"}}, '完成售后'),]);
              }else if (params.row.serviceStatus == "WAIT_REFUND") {
                return h('div', [h('tag', {props: {color: "geekblue"}}, '待平台退款'),]);
              }
            }
          },

          {
            title: "操作",
            key: "action",
            align: "center",
            width: 100,
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
      // 重置
      handleReset() {
        this.selectDate = ''
        this.searchForm = {}
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = 10;
        this.getDataList();
      },
      // 范围时间重新赋值
      selectDateRange(v) {
        if (v) {
          this.searchForm.startDate = v[0];
          this.searchForm.endDate = v[1];
        }
      },
      // 获取列表数据
      getDataList() {
        this.loading = true;
        API_Order.afterSaleOrderPage(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total;
          }
        });
        this.total = this.data.length;
        this.loading = false;
      },
      // 退款订单详情
      detail(v) {
        let sn = v.sn;
        this.$router.push({
          name: "return-goods-order-detail",
          query: { sn: sn },
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
<style lang="scss">
  // 建议引入通用样式 可删除下面样式代码
   @import "@/styles/table-common.scss";
</style>
