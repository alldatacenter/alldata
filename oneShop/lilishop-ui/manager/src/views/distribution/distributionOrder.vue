<template>
  <div>
    <Card>
      <Form ref="searchForm" @keydown.enter.native="handleSearch" :model="searchForm" inline :label-width="70" class="search-form">
        <Form-item label="订单编号"  prop="orderSn">
          <Input
            type="text"
            v-model="searchForm.orderSn"
            placeholder="请输入订单编号"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="分销商" prop="distributionName">
          <Input
            type="text"
            v-model="searchForm.distributionName"
            placeholder="请输入分销商名称"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="店铺名称">
          <Select v-model="searchForm.storeId" placeholder="请选择" @on-query-change="searchChange" filterable
                  clearable style="width: 150px">
            <Option v-for="item in shopList" :value="item.id" :key="item.id">{{ item.storeName }}</Option>
          </Select>
        </Form-item>
        <Form-item label="订单时间">
          <DatePicker type="daterange" v-model="timeRange" format="yyyy-MM-dd" placeholder="选择时间"
                      style="width: 210px"></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
      </Form>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table" class="mt_10">
        <template slot-scope="{row}" slot="goodsMsg">
          <div class="goods-msg">
            <img :src="row.image" width="60" height="60" alt="">
            <div>
              <div class="div-zoom">
                <a @click="linkTo(row.goodsId,row.skuId)">{{row.goodsName}}</a>
              </div>
              <div style="color:#999;font-size:10px">数量：x{{row.num}}</div>
              <Poptip trigger="hover" title="扫码在手机中查看" transfer>
                <div slot="content">
                  <vue-qr :text="wapLinkTo(row.goodsId,row.skuId)"  :margin="0" colorDark="#000" colorLight="#fff" :size="150"></vue-qr>
                </div>
                <img src="../../assets/qrcode.svg" class="hover-pointer" width="20" height="20" alt="">
              </Poptip>
            </div>
          </div>
        </template>
        <template slot-scope="{row}" slot="distributionOrderStatus">
          <Tag :color="filterStatusColor(row.distributionOrderStatus)">{{filterStatus(row.distributionOrderStatus)}}</Tag>
        </template>
      </Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize"
          @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10,20,50]"
          size="small" show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
  import {
    getDistributionOrder
  } from "@/api/distribution";
  import {orderStatusList} from './dataJson'
  import {getShopListData} from '@/api/shops'
  import vueQr from 'vue-qr'

  export default {
    name: "distributionOrder",
    components: {
         "vue-qr":vueQr
    },
    data() {
      return {
        timeRange: [], // 范围时间
        orderStatusList, // 订单状态列表
        shopList: [], // 店铺列表
        distributionId: this.$route.query.id, // 分销id
        loading: true, // 表单加载状态
        searchForm: { // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
          sort:"create_time",
          order:"desc"
        },
        columns: [
          {
            title: "订单编号",
            key: "orderSn",
            minWidth: 180,
            fixed: "left",
            tooltip: true
          },
          {
            title: '商品信息',
            slot: 'goodsMsg',
            minWidth: 150
          },

          {
            title: "分销商",
            key: "distributionName",
            tooltip: true,
            minWidth:80,
          },
          {
            title: "店铺名称",
            key: "storeName",
            minWidth:80,
            tooltip: true
          },
          {
            title: "状态",
            slot: "distributionOrderStatus",
            minWidth:80,
          },
          {
            title: "佣金金额",
            key: "rebate",
            minWidth:80,
            sortable: false,
            render: (h, params) => {
              if(params.row.rebate == null){
                return h("div", this.$options.filters.unitPrice(0, '￥'));
              }else{
                return h("div", this.$options.filters.unitPrice(params.row.rebate, '￥'));
              }

            }
          },
          {
            fixed: "right",
            title: "创建时间",
            key: "createTime",
            minWidth:100,
            sortable: false,
          }
        ],
        data: [], // 表单数据
        total: 0 // 表单数据总数
      };
    },
    methods: {
      // 初始化数据
      init() {
        this.getDataList();
        this.getShopList()
      },
      //分页 改变页码
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
      // 获取列表数据
      getDataList() {
        this.searchForm.distributionId = this.distributionId;
        this.loading = true;
        if (this.timeRange && this.timeRange[0]) {
          let startTime = this.timeRange[0]
          let endTime = this.timeRange[1]
          this.searchForm.startTime = this.$options.filters.unixToDate(startTime / 1000)
          this.searchForm.endTime = this.$options.filters.unixToDate(endTime / 1000)
        }
        // 带多条件搜索参数获取表单数据 请自行修改接口
        getDistributionOrder(this.searchForm).then(res => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;

            this.total = res.result.total;
          }
        });
        this.total = this.data.length;
        this.loading = false;
      },
      getShopList(val) { // 获取店铺列表 搜索用
        const params = {
          pageNumber: 1,
          pageSize: 10,
          storeName: ''
        }
        if (val) {
          params.storeName = val;
        } else {
          params.storeName = ''
        }

        getShopListData(params).then(res => {
          this.shopList = res.result.records
        })
      },
      searchChange(val) { // 店铺搜索，键盘点击回调
        this.getShopList(val)
      },
      filterStatus (status) { // 过滤订单状态
        const arr = [
          {status: 'WAIT_BILL', title: '待结算'},
          {status: 'WAIT_CASH', title: '待提现'},
          {status: 'COMPLETE_CASH', title: '提现完成'},
          {status: 'CANCEL', title: '订单取消'},
          {status: 'REFUND', title: '退款'},
        ]
        for (let i=0;i<arr.length;i++) {
          if (arr[i].status === status) {
            return arr[i].title;
          }
        }
      },
      filterStatusColor (status) { // 状态tag标签颜色
        const arr = [
          {status: 'WAIT_BILL', color: 'blue'},
          {status: 'WAIT_CASH', color: 'orange'},
          {status: 'COMPLETE_CASH', color: 'green'},
          {status: 'CANCEL', color: 'red'},
          {status: 'REFUND', color: 'magenta'},
        ]
        for (let i=0;i<arr.length;i++) {
          if (arr[i].status === status) {
            return arr[i].color;
          }
        }
      }
    },
    mounted() {
      this.init();
    },
    watch: {
      $route(e) { // 监听路由，参数变化调取接口
        this.distributionId = e.query.id ? e.query.id : undefined;
        this.getDataList();
      }
    }
  };
</script>
<style lang="scss">
  .goods-msg {
    display: flex;
    align-items: center;
    >div{
      margin-left: 10px;
    }
  }
</style>

