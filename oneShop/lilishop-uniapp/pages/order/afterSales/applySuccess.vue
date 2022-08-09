<template>
  <view>
    <view class="header">
      <u-icon name='checkmark' size="150" color="#ff6b35"></u-icon>
      <view class="success-text">售后申请提交成功</view>
      <view class="btn-view">
        <view class="btn-item">
          <u-button ripple class="btn1" shape="circle" @click="navigateToAfterSales()">查看记录</u-button>
        </view>
        <view class="btn-item">
          <u-button ripple class="btn2" shape="circle" @click="navigateToHome()">回到主页</u-button>
        </view>
      </view>
    </view>
    <view class="text-view">
      <view style="display: flex; margin-bottom: 20rpx; align-items: center">温馨提示：</view>
      <view>• 商品寄回地址将在审核通过后在申请记录中查询。</view>
      <view>• 提交服务单后，售后专员可能与您电话沟通，请保持手机畅通。</view>
      <view>• 退货处理成功后退款金额将原路返回到您的支持账户中；</view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {};
  },
  methods: {
    /**
     * 跳转到售后服务
     */
    navigateToAfterSales() {
      uni.redirectTo({
        url: "/pages/order/afterSales/afterSales",
      });
    },

    /**
     * 跳转到首页
     */
    navigateToHome() {
      uni.switchTab({
        url: "/pages/tabbar/home/index",
      });
    },
  },
};
</script>

<style lang="scss" scoped>
page {
  background: $page-color-base;
  padding-bottom: 100rpx;
}
.btn1 {
  background: $light-color;
  color: #fff;
}
.btn2 {
  color: $light-color;
}
.header {
  background-color: #fff;
  height: 500rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .success-text {
    font-size: 40rpx;
    font-weight: 400;
    margin: 20rpx 0;
  }
  .btn-view {
    display: flex;
    flex-direction: row;
    margin: 40rpx 0rpx;
    .btn-item {
      margin: 0rpx 20rpx;
    }
  }
}
.text-view {
  margin: 40rpx;
  color: #909399;
}
</style>
