<template>
  <view class="wrapper" v-if="res">
    <view v-for="(prom, index) in Object.keys(res)" :key="index">
      <view>
        <view v-if="prom.split('-')[0] == 'FULL_DISCOUNT'">
          <div class="res_prom_item" v-if="res[prom].fullMinus">
            <u-tag text="满减" type="error"></u-tag>
            <!-- TODO 后续将优化为可点击的商品以及优惠券显示明细 -->
            <span class="pro-text"
              >满{{ res[prom].fullMoney }}元 立减现金
              <span class="price">{{ res[prom].fullMinus }}元</span>
              <span v-if="res[prom].couponFlag"> 赠送<span>优惠券</span></span>
              <span v-if="res[prom].pointFlag"> 赠送{{ res[prom].point }}积分</span>
              <span v-if="res[prom].giftFlag"> 赠送商品</span>
              <span v-if="res[prom].freeFreightFlag">赠送包邮服务</span>
            </span>
          </div>
          <div class="res_prom_item" v-if="res[prom].fullRate && res[prom].fullRateFlag">
            <u-tag text="打折" type="error"></u-tag>
            <span class="pro-text"
              >满{{ res[prom].fullMoney }}元，立享<span class="price"
                >{{ res[prom].fullRate }}折</span
              >优惠</span
            >
          </div>
        </view>

        <view v-if="prom.split('-')[0] == 'PINTUAN'">
          <div class="res_prom_item" v-if="res[prom].requiredNum">
            <u-tag text="拼团" type="error"></u-tag>
            <span class="pro-text"
              >{{ res[prom].requiredNum }}人拼团 限购<span class="price"
                >{{ res[prom].limitNum }}件</span
              ></span
            >
          </div>
        </view>

        <view v-if="prom.split('-')[0] == 'SECKILL'">
          <div class="res_prom_item">
            <u-tag text="限时抢购" type="error"></u-tag>
            <span class="pro-text">限时抢购</span>
          </div>
        </view>
      </view>
    </view>
    <view v-if="!res">暂无促销活动</view>
  </view>
</template>
<script>
export default {
  data() {
    return {};
  },
  watch: {
    res: {
      handler() {
        if (this.res && this.res.length != 0) {
          Object.keys(this.res).forEach((item) => {
            if (item != "COUPON") {
              let key = item.split("-")[0];
              this.res[item]._key = key;
            }
          });
        }
      },

      immediate: true,
    },
  },

  props: {
    // 父组件传递回来的数据
    res: {
      type: null,
      default: "",
    },
  },
  mounted() {},
  methods: {},
};
</script>
<style lang="scss" scoped>
.pro-text {
  font-size: 26rpx;
  font-family: PingFang SC, PingFang SC-Regular;
  font-weight: 400;
  text-align: left;
  color: #333333;
  margin-left: 20rpx;
  > span {
    margin-right: 15rpx;
  }
}

.wrapper {
  display: block;
}

/deep/ .u-mode-light-error {
  border: none;
}

.res_prom_item {
  margin: 20rpx 0;
}

.price_image {
  display: block;
}
</style>
