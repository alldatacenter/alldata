<template>
  <div class="wrapper">
    <u-navbar :custom-back="back" title="余额"></u-navbar>
    <div class="box">
      <div class="deposit">预存款金额</div>
      <div class="money">￥{{walletNum | unitPrice }}</div>
      <div class="operation-btns">
        <div class="operation-btn light" @click="navgition('/pages/mine/deposit/withdrawal')">提现</div>
        <div class="operation-btn" @click="navgition('/pages/mine/deposit/recharge')">充值</div>
      </div>
    </div>
    <div class="box list" @click="navgition('/pages/mine/deposit/index')">
      <div class="list-left">预存款明细</div>
      <div class="list-right">
        <u-icon name="arrow-right"></u-icon>
      </div>
    </div>
  </div>
</template>

<script>
import { getUserWallet } from "@/api/members";
export default {
  data() {
    return {
      walletNum: 0,
    };
  },
  async onShow() {
    if (this.$options.filters.isLogin("auth")) {
      let result = await getUserWallet(); //预存款
      this.walletNum = result.data.result.memberWallet;
    } else {
      uni.showToast({
        icon: "none",
        duration: 3000,
        title: "请先登录！",
      });

      this.$options.filters.navigateToLogin("redirectTo");
    }
  },
  methods: {
    back() {
      uni.switchTab({
        url: "/pages/tabbar/user/my",
      });
    },
    /**
     * 跳转
     */
    navgition(url) {
      uni.navigateTo({
        url,
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.list {
  display: flex;
  justify-content: center;
  align-items: center;
}
.list-left {
  flex: 8;
}
.list-right {
  flex: 2;
  text-align: right;
}
.wrapper {
  width: 94%;
  margin: 0 3%;
}
.box {
  margin: 20rpx 0;
  background: #fff;
  border-radius: 20rpx;

  padding: 40rpx;
}
.operation-btns {
  display: flex;
  justify-content: center;
  align-items: center;
}
.money {
  text-align: center;
  color: #333;
  font-size: 50rpx;
  margin: 20rpx 0 40rpx 0;
  letter-spacing: 2rpx;
}
.deposit {
  margin-top: 50rpx;
  text-align: center;
  color: #999;
  font-size: 28rpx;

  letter-spacing: 2rpx;
}
.operation-btn {
  background: #ee6d41;
  color: #fff;
  height: 90rpx;
  width: 240rpx;
  margin: 0 20rpx;
  border-radius: 10rpx;
  text-align: center;
  line-height: 90rpx;
  font-size: 32rpx;
}
.light {
  background: #fdf2ee;
  color: #ee6d41;
}
</style>