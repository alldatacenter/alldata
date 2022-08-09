<template>
  <view>
    <view class="withdrawal-list">
      <view class="title">提现金额</view>
      <view class="content">
        <view class="price">
          <span> ￥</span>
          <u-input v-model="price" placeholder="" type="number" />
        </view>

        <view class="all">
          <view @click="handleAll" :style="{ color: $mainColor }">全部</view>
          <view style="font-size: 24rpx; color: #999"
            >可提现金额<span>{{ distributionData.canRebate | unitPrice }}</span
            >元</view
          >
        </view>
      </view>
    </view>

    <view class="submit" @click="cashd">提现</view>
  </view>
</template>
<script>
import { distribution, cash } from "@/api/goods";
export default {
  data() {
    return {
      price: 0,
      distributionData: "",
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    cashd() {
      this.price = this.price + "";
    

      if (this.$u.test.amount(parseInt(this.price))) {
        cash({ price: this.price }).then((res) => {
          if(res.data.success){
            uni.showToast({
              title: '提现成功!',
              duration: 2000,
              icon:"none"
            });
          setTimeout(()=>{
            uni.navigateBack({
               delta: 1
            });
          },1000)
          }
        });
      } else {
        uni.showToast({
          title: "请输入正确金额",
          duration: 2000,
          icon: "none",
        });
      }
    },
    handleAll() {
      this.price = this.distributionData.canRebate;
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
};
</script>
<style lang="scss" scoped>
/deep/ .u-input__input,
.u-input {
  font-size: 80rpx !important;
  height: 102rpx !important;
 
}
/deep/ .u-input__input{
  height: 100%;
   font-size: 80rpx;
}
.content {
  display: flex;
  > .price {
    width: 60%;
    margin: 20rpx 0;
    font-size: 80rpx;
    display: flex;
  }
  > .all {
    justify-content: center;
    width: 40%;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }
}
.withdrawal-list {
  margin: 20rpx 0;
  background: #fff;
  padding: 16rpx 32rpx;
}
.title {
  font-size: 35rpx;
}
.submit {
  margin: 80rpx auto;
  width: 94%;
  background: $light-color;
  height: 90rpx;
  color: #fff;
  border-radius: 10rpx;
  text-align: center;
  line-height: 90rpx;
}
</style>