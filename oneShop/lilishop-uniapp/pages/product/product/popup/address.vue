<template>
  <u-popup class="popup" v-model="addressFlag" :height="setup.height" :mode="setup.mode" :border-radius="setup.radius" @close="closeAddress()"   closeable>
    <view class="header-title">选择地址</view>
    <view class="view-box" v-if="addressDetail">
      <view class="view-item" v-for="(item, index) in addressDetail" :key="index" @click="clickAddress(item)">
        <view class="view-box-checkbox">
          <view class="checkbox" :class="{ checked: item.isDefault }">
            <u-icon v-if="item.isDefault" :class="{ active: item.isDefault }" name="checkmark" size="12"></u-icon>
          </view>
        </view>
        <view class="view-box-dress" :class="{ 'box-dress-blod': item.isDefault }">{{ item.consigneeAddressPath | clearStrComma }}</view>
      </view>
    </view>
    <view class="view-box" v-else>
      <view class="noMore">
        <u-empty text="暂无收货地址" mode="address"></u-empty>
      </view>
    </view>

    <!-- 按钮 -->

    <view class="btns">
      <view class="box-btn light" @click="getpicker">选择其他地址</view>
      <view class="box-btn" @click="closeAddress()">确定</view>
    </view>
    <m-city :provinceData="cityList" headTitle="区域选择" ref="cityPicker"  pickerSize="4"></m-city>
  </u-popup>
</template>
<script>
import setup from "@/components/m-buy/popup.js";
/************请求存储***************/

import * as API_Address from "@/api/address.js";
export default {
  data() {
    return {
      checked: "",
      setup,
      addressDetail: "",
      cityList: [
        {
          id: "",
          localName: "请选择",
          children: [],
        },
      ],
    };
  },
  filters: {},
  watch: {},
  mounted() {
    this.addressFlag = false;
    if( this.$options.filters.isLogin("auth") ){
      this.getShippingAddress()
    }
    else{
      uni.navigateTo({
         url: 'pages/passport/login'
      });
    }
 
  },
  props: ["goodsId", "addressFlag"],

  methods: {
    /**关闭地址 */
    closeAddress() {
      this.$emit("closeAddress", false);
      this.$emit("deliveryData", this.checked);
    },

    getpicker() {
      // this.$refs.cityPicker.show();
      uni.navigateTo({
        url: "/pages/mine/address/add",
      });
      this.closeAddress();
    },

    /**获取地址 */
    getShippingAddress() {
      if (this.$options.filters.isLogin("auth")) {
        API_Address.getAddressList(1, 50).then((res) => {
          if (res.data.success) {
            this.addressDetail = res.data.result.records;
            let addr = res.data.result.records.filter((item) => {
              return item.isDefault == 1;
            });

            if (addr[0]) {
              this.checked = addr[0];
              this.$emit("deliveryData", this.checked);
            }
            // addr[0] ? "" : (addr = res.data);

            // /**获取默认地址是否有货 */
            // this.clickAddress(addr[0]);
          }
        });
      }
    },

    /**点击地址返回父级商品状态 */
    clickAddress(val) {
      this.checked = val;

      this.addressDetail.forEach((item) => {
        item.isDefault = false;
      });
      val.isDefault = !val.isDefault;
      this.$emit("deliveryData", this.checked);
    },
  },
};
</script>
<style lang="scss" scoped>
.light {
  background-image: linear-gradient(
    135deg,
    #ffba0d,
    #ffc30d 69%,
    #ffcf0d
  ) !important;
  box-shadow: 0 2px 6px 0 rgba(255, 65, 66, 0.2);
}
.noMore {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.view-item {
  display: flex;
  align-items: center;
  padding: 22rpx 0;
}
.view-box-dress {
  letter-spacing: 1rpx;
  margin-left: 20rpx;
  line-height: 42rpx;
  color: #333;
  font-size: 28rpx;
}
.checked {
  background: $price-color;
}
.active {
  color: #fff;
}
.checkbox {
  text-align: center;
  line-height: 40rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid #ededed;
}
@import "@/components/m-buy/popup.scss";
.view-box {
  height: 810rpx;
  // #ifdef MP-WEIXIN
  height: 770rpx;
  // #endif
  padding: 0 20rpx;
  overflow-y: auto;
}
.header-title {
  font-weight: bold;
  color: #333;
  text-align: center;
  height: 90rpx;
  line-height: 90rpx;
  font-size: 34rpx;
}
</style>