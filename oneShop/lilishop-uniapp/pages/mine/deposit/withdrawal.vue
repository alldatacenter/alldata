<template>
  <view>
    <view class="-list">
      <view class="title">提现金额</view>
      <view class="content">
        <view class="price">
          <span> ￥</span>
          <u-input v-model="price" placeholder="" type="number" />
        </view>

        <view class="all">
          <view @click="handleAll" :style="{ color: $mainColor }">全部</view>
          <view style="font-size: 24rpx; color: #999">可提现金额<span>{{ walletNum | unitPrice }}</span>元</view>
        </view>

      </view>
      <view class="tips">
        最低提现金额为1.00元
      </view>
    </view>

    <view class="submit" @click="cashd">提现</view>
  </view>
</template>
<script>
import { getUserWallet, withdrawalApply } from "@/api/members";
export default {
  data() {
    return {
      price: 0,
      walletNum: 0,
    };
  },
  async mounted() {
    let result = await getUserWallet(); //预存款
    this.walletNum = result.data.result.memberWallet;
  },

  methods: {
    cashd() {
      this.price = this.price + "";

      if (this.$u.test.amount(parseInt(this.price))) {
        withdrawalApply({ price: this.price }).then((res) => {
          if (res.data.success) {
            uni.showToast({
              title: "提现成功!",
              duration: 2000,
              icon: "none",
            });
            setTimeout(() => {
              uni.navigateBack({
                delta: 1,
              });
            }, 1000);
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
      this.price = this.walletNum;
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./style.scss";
.tips {
  font-size: 24rpx;
  color: #999;
}
</style>