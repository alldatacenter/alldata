<template>
  <div>
    <div class="shop">
      <h3>订单详情</h3>
      <div class="shop-item">
        <div class="label-item">
          <span>订单来源</span>
          <span>{{res.clientType}}</span>
        </div>
        <div class="label-item">
          <span>订单状态</span>
          <span>{{orderStatusList[res.orderStatus]}}</span>
        </div>
        <div class="label-item">
          <span>付款状态</span>
          <span>{{res.payStatus == "UNPAID"
                ? "未付款"
                : res.payStatus == "PAID"
                ? "已付款"
                : ""}}</span>
        </div>
        <div class="label-item">
          <span>支付时间</span>
          <span>{{res.paymentTime || '暂无'}}</span>
        </div>
        <div class="label-item">
          <span>支付方式</span>
          <span>{{res.paymentMethod == "ONLINE" ? "在线支付" : ""
            }}{{ res.paymentMethod == "ALIPAY" ? "支付宝" : res.paymentMethod == "BANK_TRANSFER" ? "银行卡" : "" || '暂无'}}</span>
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

        <div class="goods-item" v-for="(item,index) in res.orderItems" :key="index">
          <div class="goods-img">
            <img class="img" :src="item.image" alt="">
          </div>
          <div class="goods-title">
            <div>{{item.name}}</div>
            <div>{{'x'+item.num}}</div>
            <div class="goods-price">{{res.flowPrice |  unitPrice('￥')}}</div>

          </div>

        </div>
      </div>
      <div class="count-price">
        <div class="label-item">
          <span>总价格</span>
          <span class="flowPrice">{{res.flowPrice |  unitPrice('￥')}}</span>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      orderStatusList: {
        UNDELIVERED: "待发货",
        UNPAID: "未付款",
        PAID: "已付款",
        DELIVERED: "已发货",
        CANCELLED: "已取消",
        COMPLETED: "已完成",
        TAKE: "已完成",
      },
    };
  },
  props: ["res"],
};
</script>
<style lang="scss" scoped>
.shop {
  padding: 10px 0;
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
  width: 20%;
  padding: 8px;
  align-items: center;
  font-weight: bold;
  display: flex;
  > span {
    padding: 8px;
  }
}
</style>
