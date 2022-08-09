<template>
  <view>
    <view
      v-for="(promotionItem, promotionIndex) in promotion"
      :key="promotionIndex"
      class="promotion_row"
      @click="shutMask(1)"
    >
      <view v-if="res != null" v-for="(item, index) in Object.keys(res)" :key="index">
        <div
          class="promotion_col"
          v-if="
            item.split('-')[0] == promotionItem.value &&
            item.split('-')[0] == 'FULL_DISCOUNT'
          "
        >
          <!-- 满减，折扣 -->
          <div class="flex">
            <view class="deg_tag">{{ promotionItem.title }}</view>
            <div class="text proText">满{{ res[item].fullMoney }}元，立享优惠</div>
          </div>
        </div>
        <div
          class="promotion_col"
          v-if="
            item.split('-')[0] == promotionItem.value && item.split('-')[0] == 'PINTUAN'
          "
        >
          <!-- 拼团 -->
          <div class="flex">
            <view class="deg_tag">{{ promotionItem.title }}</view>
            <div class="text proText">{{ res[item].promotionName }}</div>
          </div>
        </div>
        <div
          class="promotion_col"
          v-if="
            item.split('-')[0] == promotionItem.value && item.split('-')[0] == 'SECKILL'
          "
        >
          <!-- 限时抢购 -->
          <div class="flex">
            <view class="deg_tag">{{ promotionItem.title }}</view>
            <div class="text proText">{{ res[item].promotionName }}</div>
          </div>
        </div>
      </view>

      <view class="promotion_row" style="display: inline">
        <view>
          <div class="promotion_col coupon" v-if="couponList && promotionIndex == 1">
            <!-- 优惠券 -->

            <div>
              <view class="deg_tag">优惠券</view>
            </div>
          </div>
        </view>
      </view>
    </view>

    <view v-if="this.res != null && Object.keys(res).length == 0"> 暂无促销信息 </view>
  </view>
</template>

<script>
import promotion from "./promotion_type";
export default {
  data() {
    return {
      promotion,
      couponList: "",
    };
  },
  props: {
    // 父组件传递回来的数据
    res: {
      type: null,
      default: {},
    },
  },
  watch: {
    res: {
      handler() {
        if (this.res && this.res.length != 0 && this.res != null) {
          Object.keys(this.res).forEach((item) => {
            let key = item.split("-")[0];
            this.res[item].__key = key;

            if (item.split("-")[0] == "COUPON") {
              this.couponList = "COUPON";
            }
          });
        }
      },
      immediate: true,
    },
  },
  mounted() {},
  methods: {
    // 此方法条用父级方法
    shutMask(val) {
      this.$emit("shutMasks", val);
    },
  },
};
</script>

<style lang="scss" scoped>
.deg_tag {
  color: $price-color;
  padding: 0 4rpx;
  border: 2rpx solid $price-color;
  font-size: 22rpx;
}

.promotion_col {
  /**/
  // margin: 0 0 17rpx 0;

  padding: 0 !important;

  margin: 10rpx 0;
}
.promotion_row {
  display: flex;
  align-items: center;
}
.flex {
  display: flex;
}
.proText {
  font-size: 26rpx;
  font-family: PingFang SC, PingFang SC-Regular;
  font-weight: 400;
  text-align: left;
  color: #333333;
  margin-left: 20rpx;
}
/deep/ .u-mode-light-error {
  border: none;
}
</style>
