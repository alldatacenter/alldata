<template>
  <div class="wrapper">
    <div class="shop">
      <h3>售后详情</h3>
      <div class="shop-item">
        <div class="label-item">
          <span>售后类型</span>
          <span>{{serviceTypeList[res.serviceType]}}</span>
        </div>
        <div class="label-item">
          <span>售后单状态</span>
          <span>{{serviceStatusList[res.serviceStatus]}}</span>
        </div>

        <div class="label-item">
          <span>退款时间</span>
          <span>{{res.refundTime ? (new Date(res.refundTime).getTime()/1000 | unixToDate) : '暂无'}}</span>
        </div>
        <div class="label-item">
          <span>申请退款金额</span>
          <span>{{res.applyRefundPrice || 0 | unitPrice('￥')}}</span>
        </div>
        <div class="label-item">
          <span>商家备注</span>
          <span>{{res.auditRemark || '暂无'}}</span>
        </div>
        <div class="label-item">
          <span>申请原因</span>
          <span>{{res.reason || '暂无'}}</span>
        </div>

      </div>
      <div class="shop-item">
        <div class="label-item">
          <span>用户名</span>
          <span>{{res.memberName}}</span>
        </div>
        <div class="label-item">
          <span>店铺名称</span>
          <span>{{res.storeName}}</span>
        </div>
        <div class="label-item">
          <span>创建时间</span>
          <span>{{res.createTime}}</span>
        </div>
      </div>
      <h3>商品详情</h3>
      <div class="shop-item">

        <div  @click="handleClickGoods(res.goodsId)" class="goods-item">
          <div class="goods-img">
            <img class="img" :src="res.goodsImage" alt="">
          </div>
          <div class="goods-title">
            <div>{{res.goodsName}}</div>
            <div>{{'x'+res.num}}</div>
            <div class="goods-price">{{res.flowPrice |  unitPrice('￥')}}</div>

          </div>

        </div>
      </div>
      <div class="count-price">
        <div class="label-item">
          <span>实际退款金额</span>
          <span class="flowPrice">{{res.flowPrice | unitPrice('￥')}}</span>
        </div>
      </div>
    </div>

  </div>
</template>
<script>
export default {
  data() {
    return {
      orderStatusList: { // 订单状态
        UNDELIVERED: "待发货",
        UNPAID: "未付款",
        PAID: "已付款",
        DELIVERED: "已发货",
        CANCELLED: "已取消",
        COMPLETED: "已完成",
        TAKE: "已完成",
      },
      // 售后类型
      serviceTypeList: {
        CANCEL: "取消",
        RETURN_GOODS: "退货",
        EXCHANGE_GOODS: "换货",
        RETURN_MONEY: "退款",
      },
      serviceStatusList: {
        APPLY: "申请售后",
        PASS: "通过售后",
        REFUSE: "拒绝售后",
        BUYER_RETURN: "买家退货，待卖家收货",
        SELLER_RE_DELIVERY: "商家换货/补发",
        SELLER_CONFIRM: "卖家确认收货",
        SELLER_TERMINATION: "卖家终止售后",
        BUYER_CONFIRM: "买家确认收货",
        BUYER_CANCEL: "买家取消售后",
        WAIT_REFUND: "等待平台退款",
        COMPLETE: "完成售后",
      },
    };
  },
  props: ["res"],
  methods: {
    handleClickGoods(id) {
      this.$router.push({
        query: { id },
        path: "/goods/goods-info/goodsDetail",
      });
    },
  }
};
</script>
<style lang="scss" scoped>
.shop {
  padding: 5px 0;
  background: #fff;
}
.shop-item {
  display: flex;
  flex-wrap: wrap;
}
h3 {
  margin: 20px 16px;
  font-size: 18px;
}
.goods-price {
  font-size: 18px;
  color: red;
}
.goods-item {
  display: flex;
  width: 100%;
  margin: 16px;
  cursor: pointer;
}
.count-price {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.flowPrice {
  font-size: 24px;
  color: red;
}
.goods-title {
  margin: 0 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-weight: bold;
}
.img {
  width: 100px;
  height: 100px;
  border-radius: 10px;
}
.label-item {
  margin: 10px 0;
  width: 33%;
  padding: 8px;
  align-items: center;
  font-weight: bold;
  display: flex;
  > span:nth-child(1) {
    width: 90px;
    color: #999;
  }
  > span:nth-child(2){
    text-align: left;
    white-space: wrap;
  }
}
</style>
