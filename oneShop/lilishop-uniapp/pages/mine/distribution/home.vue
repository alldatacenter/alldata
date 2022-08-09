<template>
  <view>
	
    <view class="nav-list">
      <view class="total">可提现金额</view>
      <view class="price">{{ distributionData.canRebate | unitPrice }}</view>
      <view class="frozen"
        >冻结金额{{ distributionData.commissionFrozen | unitPrice }}</view
      >
    </view>
    <view class="nav">
      <view class="nav-item">
        <u-icon
          size="50"
          @click="handleClick('/pages/mine/distribution/list?id='+distributionData.id+'&name='+distributionData.memberName)"
          color="#ff6b35"
          name="bag-fill"
        ></u-icon>
        <view>分销商品</view>
      </view>
      <view
        class="nav-item"
        @click="handleClick(`/pages/mine/distribution/history?type=0&id=${distributionData.id}&name=${distributionData.memberName}`)"
      >
        <u-icon size="50" color="#ff6b35" name="order"></u-icon>
        <view>分销业绩</view>
      </view>
      <view
        class="nav-item"
        @click="handleClick('/pages/mine/distribution/history?type=1')"
      >
        <u-icon size="50" color="#ff6b35" name="red-packet-fill"></u-icon>
        <view>提现记录</view>
      </view>
      <view
        class="nav-item"
        @click="handleClick('/pages/mine/distribution/withdrawal')"
      >
        <u-icon size="50" color="#ffc71c" name="rmb-circle-fill"></u-icon>
        <view>提现</view>
      </view>
  
     
    </view>

  </view>
</template>

<script>

import { distribution } from "@/api/goods";
export default {

  data() {
    return {
      distributionData: "",
    };
  },
  methods: {
    handleClick(url) {
      uni.navigateTo({
        url,
      });
    },
    queryGoods(src) {
      uni.navigateTo({
        url: `/pages/mine/distribution/${src}`,
      });
    },
    /**
     * 初始化推广商品
     */
    init() {
      uni.showLoading({
        title: "加载中",
      });
      distribution().then((res) => {
        if (res.data.result) {
          this.distributionData = res.data.result;
        }
        uni.hideLoading();
      });
    },
  },
  onShow() {
    this.init();
  },
};
</script>

<style lang="scss" scoped>
.nav {
  background: #fff;
  align-items: center;
  display: flex;
  flex-wrap: wrap;
}
.nav-list {
  color: #fff;
  padding: 40rpx 0;
  background: $aider-light-color;
}
.total {
  padding: 10rpx 0;
  text-align: center;
  font-size: 28rpx;
  opacity: 0.8;
}
.frozen {
  text-align: center;
  font-size: 24rpx;
  opacity: 0.8;
}
.price {
  text-align: center;
  color: #fff;
  font-size: 50rpx;
}
.nav-item {
  height: 240rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  > * {
    margin: 10rpx 0;
  }
  width: 33%;
  //   color: #fff;
}
</style>
