<template>
  <div class="wrapper">
    <!-- 卡片组件 -->
    <card _Title="我的售后" :_Size="16"></card>
    <!-- 搜索 筛选 -->
    <div class="mb_20 box">
      <div class="global_float_right">
        <Input
          class="width_300"
          search
          enter-button
          v-model="params.sn"
          @on-search="getList"
          placeholder="请输入订单号搜索"
        />
      </div>
    </div>
    <!-- 列表 -->
    <empty v-if="orderList.length === 0"/>
    <div class="order-content" v-else>
      <template v-for="(order, onderIndex) in orderList">
        <div class="order-list" :key="onderIndex">
          <div class="order-header">
            <div>
              <div v-if="order.serviceStatus">{{ filterOrderStatus(order.serviceStatus) }}</div>
              <div>
                售后单号：{{ order.sn }} &nbsp; &nbsp; &nbsp;{{ order.createTime }}
                &nbsp; &nbsp;{{ order.memberName | secrecyMobile }}
              </div>
            </div>
            <div>
              <span>申请退款金额：<span class="global_color">{{ order.applyRefundPrice | unitPrice("￥") }}</span></span>
            </div>
          </div>
          <div class="order-body">
            <div class="goods-list">
              <img @click="goodsDetail(order.skuId, order.goodsId)" class="hover-color" :src="order.goodsImage" alt=""/>

              <div>
                <div class="hover-color" @click="goodsDetail(order.skuId, order.goodsId)">{{ order.goodsName }}</div>
                <div class="mt_10"> x {{ order.num }}
                </div>
              </div>
            </div>
            <div>
              <span @click="shopPage(order.shopId)">{{ order.storeName }}</span>
            </div>
            <div>
              <!-- 订单基础操作 -->
              <Button @click="goDetail(order.sn)" type="info" size="small">售后详情</Button>
              <Button @click="openModal(order)"
                      v-if="order.serviceStatus == 'PASS' &&
                order.serviceType != 'RETURN_MONEY'"
                      type="warning" size="small">提交物流
              </Button>
              <Button @click="cancel(order.sn)" type="error" v-if="order.afterSaleAllowOperationVO.cancel" size="small">
                取消售后
              </Button>
            </div>
          </div>
        </div>
      </template>
      <Spin size="large" fix v-if="spinShow"></Spin>
    </div>
    <!-- 分页 -->
    <div class="page-size">
      <Page :total="total" @on-change="changePageNum"
            @on-page-size-change="changePageSize"
            :page-size="params.pageSize"
            show-sizer>
      </Page>
    </div>
    <Modal v-model="logisticsShow" width="530">
      <p slot="header">
        <span>提交物流信息</span>
      </p>
      <div>
        <div class="goods-list modal-goods">
          <img @click="goodsDetail(singleOrder.skuId, singleOrder.goodsId)" class="hover-color"
               :src="singleOrder.goodsImage" alt=""/>
          <div>
            <div class="hover-color" @click="goodsDetail(singleOrder.skuId, singleOrder.goodsId)">
              {{ singleOrder.goodsName }}
            </div>
            <div class="mt_10">
              <span class="global_color"
              >{{ singleOrder.flowPrice | unitPrice("￥") }} </span
              >x {{ singleOrder.num }}
            </div>
          </div>
        </div>
        <Form ref="form" :model="form" label-position="left" :label-width="100" :rules="rules">
          <FormItem label="物流公司" prop="logisticsId">
            <Select v-model="form.logisticsId" placeholder="请选择物流公司">
              <Option v-for="item in companyList" :value="item.id" :key="item.id">{{ item.name }}</Option>
            </Select>
          </FormItem>
          <FormItem label="快递单号" prop="logisticsNo">
            <Input v-model="form.logisticsNo" placeholder="请填写快递单号"></Input>
          </FormItem>
          <FormItem label="发货时间" prop="mDeliverTime">
            <DatePicker type="date" style="width:100%" v-model="form.mDeliverTime" @on-change="changeTime"
                        format="yyyy-MM-dd" placeholder="选择发货时间"></DatePicker>
          </FormItem>
        </Form>
      </div>
      <div slot="footer" style="text-align: right">
        <Button @click="logisticsShow = false">关闭</Button>
        <Button type="primary" :loading="submitLoading" @click="submitDelivery">提交</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import {afterSaleList, cancelAfterSale} from '@/api/member.js';
import {afterSaleDelivery, getLogisticsCompany} from '@/api/order.js';
import {afterSaleStatusList} from '../enumeration.js'

export default {
  name: 'AfterSale',
  data() {
    return {
      orderList: [], // 订单列表
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 10,
        sn: '',
        sort: 'createTime',
        order: 'desc'
      },
      // 状态数组
      afterSaleStatusList,
      total: 0, // 订单总数
      spinShow: false, // 加载状态
      companyList: [], // 物流公司列表
      logisticsShow: false, // 物流信息modal
      singleOrder: {}, // 单独的售后信息
      form: { // 物流信息数据
        afterSaleSn: '',
        logisticsId: '',
        logisticsNo: '',
        mDeliverTime: ''
      },
      rules: { // 必填校验
        logisticsId: [{required: true, message: '请选择物流公司'}],
        logisticsNo: [{required: true, message: '请填写物流编号'}],
        mDeliverTime: [{required: true, message: '请选择发货时间'}]
      },
      submitLoading: false // 提交加载状态
    };
  },
  mounted() {
    this.getList();
  },
  methods: {
    goDetail(sn) { // 跳转售后详情
      this.$router.push({
        name: 'AfterSaleDetail',
        query: {sn}
      })
    },
    cancel(sn) { // 取消售后申请
      this.$Modal.confirm({
        title: '取消',
        content: '<p>确定取消此次售后申请吗？</p>',
        onOk: () => {
          cancelAfterSale(sn).then(res => {
            if (res.success) {
              this.$Message.success('取消售后申请成功')
              this.getList()
            }
          })
        },
        onCancel: () => {
        }
      });
    },
    goodsDetail(skuId, goodsId) {
      // 跳转商品详情
      let routeUrl = this.$router.resolve({
        path: '/goodsDetail',
        query: {skuId, goodsId}
      });
      window.open(routeUrl.href, '_blank');
    },
    // 跳转店铺首页
    shopPage(id) {
      let routeUrl = this.$router.resolve({
        path: '/Merchant',
        query: {id: id}
      });
      window.open(routeUrl.href, '_blank');
    },
    getList() { // 获取售后列表
      this.spinShow = true;
      let params = JSON.parse(JSON.stringify(this.params))
      afterSaleList(params).then(res => {
        this.spinShow = false
        if (res.success) {
          this.orderList = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    changePageNum(val) { // 修改页码
      this.params.pageNumber = val;
      this.getList()
    },
    changePageSize(val) { // 修改页数
      this.params.pageNumber = 1;
      this.params.pageSize = val;
      this.getList()
    },
    filterOrderStatus(status) { // 获取订单状态中文
      const ob = this.afterSaleStatusList.filter(e => {
        return e.status === status
      });
      return ob[0].name
    },
    // 获取物流公司列表
    getCompany() {
      getLogisticsCompany().then(res => {
        if (res.success) {
          this.companyList = res.result
        }
      })
    },
    // 提交物流信息
    submitDelivery() {
      this.submitLoading = true
      afterSaleDelivery(this.form).then(res => {
        if (res.success) {
          this.logisticsShow = false;
          this.$Message.success('提交成功')
          this.getList()
        }
        this.submitLoading = false
      }).catch(() => {
        this.submitLoading = false
      })
    },
    // 提交物流modal
    openModal(row) {
      console.log(row);
      this.singleOrder = row;
      this.form.afterSaleSn = row.sn
      this.logisticsShow = true;
      this.$refs.form.resetFields()
      if (!this.companyList.length) {
        this.getCompany()
      }
    },
    // 格式化时间
    changeTime(time) {
      this.form.mDeliverTime = time;
    }
  }
};
</script>

<style scoped lang="scss">
.wrapper {
  margin-bottom: 40px;
}

.box {
  overflow: hidden;
}

.page-size {
  margin: 15px 0px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

/** 订单列表 */
.order-list {
  border: 1px solid #ddd;
  border-radius: 3px;
  margin-bottom: 10px;

  &:hover {
    .del-btn {
      visibility: visible;
    }
  }

  .del-btn {
    visibility: hidden;
  }

  .order-header {
    display: flex;
    align-items: center;
    padding: 10px;
    justify-content: space-between;
    border-bottom: 1px solid #ddd;

    > div:nth-child(1) > div:nth-child(2) {
      font-size: 12px;
      color: #999;
      margin-top: 3px;
    }
  }

  .order-body {
    display: flex;
    justify-content: space-between;
    color: #999;
    padding: 10px;

    > div:nth-child(2) {
      width: 150px;
      text-align: center;

      span {
        color: #438cde;
        cursor: pointer;

        &:hover {
          color: $theme_color;
        }
      }

      .ivu-icon {
        color: #ff8f23;
        cursor: pointer;

        &:hover {
          color: $theme_color;
        }
      }
    }

    > div:nth-child(3) {
      width: 100px;

      .ivu-btn {
        margin-bottom: 10px;
      }
    }
  }
}

.goods-list {
  // width: 500px;
  display: flex;
  margin-bottom: 10px;

  img {
    width: 60px;
    height: 60px;
    margin-right: 10px;
  }

  > div {
    flex: 1;
  }
}

.modal-goods {
  padding: 5px;
}
</style>
