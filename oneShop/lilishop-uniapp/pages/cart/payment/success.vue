<template>
  <div class="wrapper">
    <div class="pay-wrapper">
      <div class="pay-money">
        ￥{{ Number(payPrice) | unitPrice }}
      </div>
      <div class="pay-btns">
        <div v-show="!from" @click="checkOrder">查看{{this.orderType == "RECHARGE" ? '余额' : '订单'}}</div>
        <div @click="navigateTo('/pages/tabbar/home/index', 'switch')">回到首页</div>
      </div>
    </div>
    <div class="pay-box">
      <div class="pay-tag-box">
        <h2>订单支付成功!</h2>
        <div class="pay-item">
          <div>
            支付方式：
          </div>
          <div>{{paymentMethod | paymentTypeFilter}}</div>
        </div>
      </div>
    </div>
    <goodsRecommend />
  </div>

</template>
<script>
import goodsRecommend from "@/components/m-goods-recommend";
export default {
  data() {
    return {
      checked: false,
      paymentMethod: "",

      from: "",
      payPrice: 0,
      goodsList: [],
      activeColor: this.$mainColor,
    };
  },
  components: {
    goodsRecommend,
  },
  filters: {
    paymentTypeFilter(val) {
      switch (val) {
        case "WECHAT":
          return "微信";
        case "ALIPAY":
          return "支付宝";
        case "WALLET":
          return "余额支付";
        default:
          return "";
      }
    },
  },
  onLoad(options) {
    this.paymentMethod = options.paymentMethod || "";
    this.from = options.from || "";
    this.payPrice = options.payPrice || 0;
    this.orderType = options.orderType;
    // this.sendMessage()
  },
  methods: {
    checkOrder() {
      /**
       * 查看订单
       * 1.充值跳转到明细里面
       * 2.支付跳转到订单详情
       */
      if (this.orderType == "RECHARGE") {
        uni.reLaunch({
          url: `/pages/mine/deposit/operation`,
        });
      } else {
        this.navigateTo("/pages/order/myOrder?status=0");
      }
    },
    changeStatus(val) {
      if (val) {
        this.sendMessage();
      }
    },

    navigateTo(url, type) {
      if (type === "switch") {
        uni.switchTab({
          url,
        });
      } else {
        uni.redirectTo({
          url,
        });
      }
    },
  },
};
</script>
<style scoped lang="scss">
.subscribe {
  justify-content: space-between;
  align-items: center;
  margin: 0 auto 40rpx auto;
  padding: 0 20rpx 20rpx;
  width: 80%;
}
.pay-btns {
  display: flex;
  width: 50%;
  justify-content: space-between;
  margin: 0 auto;
  color: #fff;

  > div {
    padding: 6px 12px;
    border: 1px solid #fff;
    border-radius: 100px;
  }
}

.pay-money {
  line-height: 1;
  font-size: 50rpx;
  color: #fff;
  margin-bottom: 100rpx;
}

.pay-item {
  font-weight: bold;
  margin: 32rpx 0;
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: rgba($color: $main-color, $alpha: 0.8);
}

.pay-box {
  overflow: hidden;
}

.pay-tag-box {
  width: 80%;
  margin: 80rpx auto 40rpx auto;
  padding: 20rpx;
  border-radius: 20rpx;
  background: rgba($color: $main-color, $alpha: 0.2);

  > h2 {
    margin-top: 20rpx;
    font-size: 40rpx;
    color: $main-color;
  }
}

.pay-wrapper {
  background-image: linear-gradient(90deg, #fa123b, #ff6b35, #ff9f28, #ffcc03);
  height: 480rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.pay-box {
  transform: translateY(-100rpx);
  width: 100%;
  background: #fff;
  border-top-right-radius: 100rpx;
}
</style>
