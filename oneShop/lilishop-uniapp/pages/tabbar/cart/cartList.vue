<template>
  <div class="wrapper">
    <!-- 空白页-->
    <view v-if="cartDetail.cartList == '' || cartDetail.cartList == [] || !cartDetail" class="empty">
      <image src="/static/emptyCart.png" mode="aspectFit"></image>
      <view class="empty-tips">
        空空如也
        <navigator class="navigator" url="/pages/tabbar/home/index" open-type="switchTab">随便逛逛></navigator>
      </view>
    </view>

    <!-- 店铺商品信息 -->
    <div class="content">
      <div class="box box2" :class="{ invalid: isInvalid(item) }" v-for="(item, index) in cartDetail.cartList"
        :key="index">
        <view class="tab">
          <view class="store-line">
            <u-checkbox-group class="store-line-check">
              <!-- #ifndef MP-WEIXIN -->
              <u-checkbox shape="circle" :active-color="lightColor" v-model="item.checked"
                @change="checkboxChangeDP(item)"></u-checkbox>
              <!-- #endif -->
              <!-- 微信小程序这里 v-model出现问题，改用:value -->
              <!-- #ifdef MP-WEIXIN -->
              <u-checkbox shape="circle" :active-color="lightColor" :value="item.checked"
                @change="checkboxChangeDP(item)"></u-checkbox>
              <!-- #endif -->
            </u-checkbox-group>
            <span class="store-name store-line-desc" @click.stop="navigateToStore(item)">{{
              item.storeName 
            }}</span>
            <u-icon @click="navigateToStore(item)"  size="24" style="margin-left:10rpx;"  name="arrow-right"></u-icon>
          </view>
          <view class="right-col" v-if="item.canReceiveCoupon" @click="navigateToConpon(item)">
            <div class="right-line"></div>
            <span>领劵</span>
          </view>
        </view>
        <u-swipe-action :show="skuItem.selected" @open="openAction(skuItem)" :options="options" bg-color="#fff"
          ref="swiperAction" class="cart-item" v-for="(skuItem, i) in item.skuList" :index="i" :key="skuItem.goodsSku.id"
          @click="changeActionTab(skuItem)" @longpress="changeActionTab(skuItem)">
          <!-- 满减活动 -->
          <div v-if="Object.keys(skuItem.promotionMap).length != 0">
            <div v-if="getPromotion(skuItem).includes('FULL_DISCOUNT')">
              <div class="promotion-notice" v-if="item.promotionNotice">
                <span class="tips">满减</span>
                <span style="flex:10;">{{item.promotionNotice}}</span>
              </div>
            </div>
          </div>
          <view class="goods-row" :class="{ invalid: isInvalid(skuItem) }">
            <view class="goods-config">
              <view>
                <u-checkbox-group v-if="skuItem.invalid == 0 && !skuItem.errorMessage">
                  <!-- #ifndef MP-WEIXIN -->
                  <u-checkbox shape="circle" :active-color="lightColor" class="c-left" v-model="skuItem.checked"
                    @change="checkboxChange(skuItem)"></u-checkbox>
                  <!-- #endif -->
                  <!-- 微信小程序这里 v-model出现问题，改用:value -->
                  <!-- #ifdef MP-WEIXIN -->
                  <u-checkbox shape="circle" :active-color="lightColor" class="c-left" :value="skuItem.checked"
                    @change="checkboxChange(skuItem)"></u-checkbox>
                  <!-- #endif -->
                </u-checkbox-group>
                <span class="invalid" v-else style="font-size: 24rpx">失效</span>
              </view>
              <u-image border-radius="10" :fade="true" @click="navigateToGoods(skuItem)" width="160rpx" height="160rpx"
                :src="skuItem.goodsSku.thumbnail" />
            </view>
            <view class="goods-content">
              <!-- 商品名称 -->
              <p class="sp-name" @click="navigateToGoods(skuItem)">
                {{ skuItem.goodsSku.goodsName }}
              </p>
              <!-- 规格 -->
              <p class="sp-type">{{skuItem.goodsSku.simpleSpecs}}</p>
              <p class="sp-type" v-if="skuItem.goodsSku.salesModel == 'WHOLESALE'">批发商品</p>
              <p class="sp-number">
                <view class="sp-price">
                  <div class="default-color" :class="{'main-color':Object.keys(skuItem.promotionMap).length ==0  }">
                    
                    ￥<span>{{ formatPrice(skuItem.goodsSku.price)[0] }}</span>
                    <span>.{{ formatPrice(skuItem.goodsSku.price)[1] }}</span>
                  </div>
                </view>
                <view>
                  <!-- #ifndef MP-WEIXIN -->
                  <u-number-box class="uNumber" :min="1" input-width="70" input-height="40" size="20"
                    v-model="skuItem.num" @change="numChange(skuItem)"></u-number-box>
                  <!-- #endif -->
                  <!-- #ifdef MP-WEIXIN -->
                  <u-number-box class="uNumber" :min="1" input-width="70" input-height="40" size="20"
                    :value="skuItem.num" @plus="numChange(skuItem, '1')" @change="numChange_WEIXIN" :skuItem="skuItem"
                    @minus="numChange(skuItem, '0')"></u-number-box>
                  <!-- #endif -->
                </view>
                <!-- 如果当有促销并且促销是 限时抢购 -->
                <!-- promotions -->
              <div class="promotions-list" v-if="Object.keys(skuItem.promotionMap).length != 0"
              >
                <div class="promotions-item-seckill" v-if="getPromotion(skuItem).includes('SECKILL')">
                  距秒杀结束: <u-count-down show-border :hide-zero-day="true" :color="$mainColor" border-color="#ededed"
                    font-size="24" :timestamp="getCountDownTime(skuItem)">
                  </u-count-down>
                </div>
              </div>

              <!-- 如果有活动 并且是选中的状态,显示预估到手价格 -->
              <div class="priceDetail-flowPrice" :class="{'main-color':skuItem.priceDetailDTO}"
                v-if="skuItem.priceDetailDTO && skuItem.invalid == 0  && Object.keys(skuItem.promotionMap).length != 0 && skuItem.checked && skuItem.checked">
                预估到手价 ￥<span>{{ formatPrice(skuItem.priceDetailDTO.flowPrice)[0]}}</span>
                <span>.{{ formatPrice(skuItem.priceDetailDTO.flowPrice)[1] }} </span>
              </div>
							<div style='margin-left: 20rpx;' v-if="!skuItem.checked && skuItem.errorMessage">
								{{skuItem.errorMessage}}
							</div>
            </view>
          </view>
        </u-swipe-action>
      </div>
    </div>
    <u-modal v-model="deleteShow" :confirm-style="{'color':lightColor}" @confirm="delectConfirm" show-cancel-button
      :content="deleteContent" :async-close="true"></u-modal>
    <!-- 结账 -->
    <div class="box box6">
      <view class="navL">
        <u-checkbox shape="circle" :active-color="lightColor" v-model="checkout" @change="checkOut()" label-size="24">全选
        </u-checkbox>
        <span class="price">
          <div class="prices">
            <div class="fullPrice">
              <span class="number" v-if="cartDetail && cartDetail.priceDetailDTO">
                总计:
                <span>¥{{ formatPrice(cartDetail.priceDetailDTO.flowPrice)[0] }}</span>.<span>{{ formatPrice(cartDetail.priceDetailDTO.flowPrice)[1] }}</span>
              </span>
              <span class="number" v-else>总计:0.00</span>
            </div>
            <div
              v-if="cartDetail.cartList && cartDetail.cartList.length!=0 && cartDetail.priceDetailDTO && cartDetail.priceDetailDTO.discountPrice!=0 "
              class="discountPrice">
              <span>优惠减:￥{{(cartDetail.priceDetailDTO.goodsPrice - cartDetail.priceDetailDTO.flowPrice) | unitPrice}}
              </span>
              <span class="discount-details" @click="discountDetails">优惠明细</span>
            </div>
          </div>
        </span>
      </view>
      <!-- 优惠详情 -->
      <u-popup z-index="3" close mode="bottom" height="50%" closeable v-model="discountDetailsFlag" border-radius="20">
        <div class="discount-list">
          <view class="discount-title">优惠明细</view>
          <div class="discount-way">
            <div class="discount-item" v-if="cartDetail.priceDetailDTO">
              <span>商品总额</span>
              <span>￥{{cartDetail.priceDetailDTO.goodsPrice | unitPrice}}</span>

            </div>
            <div class="discount-item" v-if="cartDetail.priceDetailDTO">
              <span>优惠券</span>
              <span>-￥{{cartDetail.priceDetailDTO.couponPrice | unitPrice}}</span>
            </div>
            <div class="discount-item" v-if="cartDetail.priceDetailDTO">
              <span>其他优惠</span>
              <span>-￥{{cartDetail.priceDetailDTO.discountPrice | unitPrice}}</span>
            </div>
          </div>
        </div>
      </u-popup>

      <view v-if="isEdit" @click="deleteGoods()">
        <div class="settlement">删除</div>
      </view>

      <view v-else @click="submitOrder()">
        <div class="settlement">去结算</div>
      </view>
    </div>
    <u-toast ref="uToast" />
  </div>
</template>
<script>
import * as API_Trade from "@/api/trade";
export default {
  data() {
    return {
      lightColor: this.$lightColor,
      discountDetailsFlag: false, //优惠明细开关
      // 商品栏右侧滑动按钮
      options: [
        {
          text: "删除",
          style: {
            backgroundColor: this.$lightColor, //高亮颜色
          },
        },
      ],
      isInvalid(val) {
        //是否无效商品/没库存商品
        if (val.invalid == 1 || (!val.checked && val.errorMessage)) {
          return true;
        } else {
          return false;
        }
      },
      deleteShow: false, //右滑删除
      deleteContent: "删除该商品？", //删除显示的信息
      cartDetail: "", //购物车详情
      goodsVal: "", //单个商品详情
      isEdit: false, // 是否是编辑
      checkout: false, //全选按钮
      WEIXIN_num: "", //购物车兼容微信步进器
    };
  },
  
  mounted() {
    // #ifdef MP-WEIXIN
    // 小程序默认分享
    uni.showShareMenu({ withShareTicket: true });
    // #endif
  },
  onPullDownRefresh(){
    console.log("132")
    this.getCardData();
  },
  /**
   * 初始化信息
   */
  onShow() {
    this.deleteShow ? (this.deleteShow = false) : true;
    if (this.$refs.swiperAction) {
      this.$refs.swiperAction.forEach((item, index) => {
        item.show = false;
      });

      this.getCardData();
    } else {
      this.getCardData();
    }
  },
  methods: {
    /**
			 * 倒数计时
			 */
    getCountDownTime(val) {
      if (val.promotionMap) {
        let key = Object.keys(val.promotionMap).find((child, index) => {
          return child.split("-")[0] == 'SECKILL'
        });
        return val.promotionMap[key].endTime / 1000 - (new Date().getTime() / 1000)
      }
    },

    /**
     * 优惠明细开关
     */
    discountDetails() {
      this.discountDetailsFlag = true;
    },

    /**
     *  格式化金钱  1999 --> [1999,00]
     */
    formatPrice(val) {
      if (typeof val == "undefined") {
        return val;
      }
      return val.toFixed(2).split(".");
    },

    /**
     * 左滑打开删除
     */
    openAction(skuItem) {
      /**循环父级有多少个店铺 */
      this.cartDetail.cartList.forEach((cartItem) => {
        if (cartItem.skuList) {
          cartItem.skuList.forEach((sku) => {
            this.$set(sku, "selected", false);
          });
        }
      });
      this.$set(skuItem, "selected", true);
    },

    /**
     * 滑动删除
     */
    changeActionTab(val) {
      this.deleteShow = true;
      this.goodsVal = val;
    },

    /**
     * 点击删除
     */
    delectConfirm() {
      API_Trade.deleteSkuItem(this.goodsVal.goodsSku.id).then((res) => {
        if (res.statusCode == 200) {
          uni.showToast({
            title: "此商品删除成功",
            duration: 2000,
          });
          this.deleteShow = false;
          this.getCardData();
        }
      });
    },

    /**
     * 删除商品
     */
    deleteGoods() {
      if (this.whetherChecked()) {
        var delGoodsData = [];
        this.cartDetail.cartList.forEach((item) => {
          item.skuList.forEach((goodsItem) => {
            if (goodsItem.checked) {
              delGoodsData.push(goodsItem.goodsSku.id);
            }
          });
        });
        if (delGoodsData && delGoodsData.length > 0) {
          // 执行删除
          API_Trade.deleteSkuItem(delGoodsData).then((res) => {
            if (res.data.success) {
              uni.showToast({
                title: "删除成功!",
                icon: "none",
              });
              this.getCardData();
            }
          });
        } else {
          uni.showToast({
            title: "请选择删除商品，如果商品失效，请左滑无效商品删除",
            icon: "none",
          });
        }
      }
    },

    /**
     * 跳转到店铺
     */
    navigateToStore(val) {
      uni.navigateTo({
        url: "/pages/product/shopPage?id=" + val.storeId,
      });
    },

    /**
     * 跳转到优惠券
     */
    navigateToConpon(val) {
      uni.navigateTo({
        url: "/pages/cart/coupon/couponCenter?storeId=" + val.storeId,
      });
    },

    /**
     * 跳转到商品
     */
    navigateToGoods(val) {
      uni.navigateTo({
        url:
          "/pages/product/goods?id=" +
          val.goodsSku.id +
          "&goodsId=" +
          val.goodsSku.goodsId,
      });
    },

    /**
     * 点击步进器微信回调
     */
    numChange_WEIXIN(callback) {
      this.WEIXIN_num = callback.value;
      this.numChange(callback.data, "3");
    },

    /**
     * 点击步进器回调
     */
    numChange(val, nums) {
      // #ifdef MP-WEIXIN
      if (nums && nums == "1") {
        val.num++;
      } else if (nums && nums == "0") {
        val.num--;
      } else if (nums && nums == "3") {
        val.num = this.WEIXIN_num;
      }
      // #endif
      this.updateSkuNumFun(val.goodsSku.id, val.num);
    },

    /**
     * 去结算
     */
    submitOrder() {
      if (this.whetherChecked()) {
        this.navigateTo("/pages/order/fillorder?way=CART");
      }
    },

    /**
     * 验证是否选中商品
     */
    whetherChecked() {
    this.$options.filters.forceLogin()

      let canBuy = false;
      this.cartDetail.cartList.forEach((item) => {
        if (item.checked) {
          canBuy = true;
        } else {
          item.skuList.forEach((skuItem) => {
            if (skuItem.checked) {
              canBuy = true;
            }
          });
        }
      });
      if (!canBuy) {
        uni.showToast({
          title: "您还没有选择商品",
          duration: 2000,
          icon: "none",
        });
        return false;
      } else {
        return true;
      }
    },

    /**
     * 跳转
     */
    navigateTo(url) {
      uni.navigateTo({
        url,
      });
    },

    /**
     * 全选
     */
    checkOut() {
      API_Trade.checkAll(this.checkout).then((result) => {
        if (result.data.success) {
          this.getCardData();
          return true;
        }
      });
    },

    /**
     * 获取店铺选中信息
     */
    checkStoreFun(skuId, num) {
      API_Trade.checkStore(skuId, num).then((result) => {
        if (result.data.success) {
          this.getCardData();
        }
      });
    },

    /**
     * 店铺点击
     */
    checkboxChangeDP(e) {
      // #ifdef MP-WEIXIN
      e.checked = !e.checked;
      // #endif
      this.checkStoreFun(e.storeId, e.checked);
    },

    /**
     * 获取购物车选中信息
     */
    updateSkuCheckedFun(skuId, num) {
      API_Trade.updateSkuChecked(skuId, num).then((result) => {
        if (result.data.success) {
          this.getCardData();
        }
      });
    },

    /**
     * 更新商品购物车数量
     */
    updateSkuNumFun(skuId, num) {
      API_Trade.updateSkuNum(skuId, num).then((result) => {
        if (result.statusCode == 200) {
          this.getCardData();
        } else {
          let _this = this;
          setTimeout(() => {
            _this.getCardData();
          }, 1000);
        }
      });
    },

    // 数据去重一下 
    getPromotion(item) {
        return Object.keys(item.promotionMap).map((child) => {
          return child.split("-")[0]
        });
    },

    /**
     * 获取购物车数据
     */
    getCardData() {
      if (this.$options.filters.isLogin("auth")) {
        uni.showLoading({
          title: "加载中",
        });
        API_Trade.getCarts()
          .then((result) => {
            uni.stopPullDownRefresh();
            if (result.data.success) {
              this.cartDetail = result.data.result;
              let checkouted = true;
              for (let i = 0; i < this.cartDetail.cartList.length; i++) {
                let item = this.cartDetail.cartList[i];
                console.log(item);
                // 循环出当前商品是否全选
                if (item.checked == 0) {
                  checkouted = false;
                }
                // 如果有拼团活动顺便删除
                item.skuList &&
                  item.skuList.forEach((sku) => {
                    if (sku.checked == 0) {
                      checkouted = false;
                    }
                    if(Object.keys(sku.promotionMap).length != 0)
                    {
                      Object.keys(sku.promotionMap).forEach((pro, proIndex) => {
                        pro = pro.split('-')[0]
                        if (pro == "PINTUAN" ) {
                           Object.keys(sku.promotionMap).splice(proIndex, 1);
                        }
                      });
                    }
                
                  });
              }
              this.checkout = checkouted;
              uni.stopPullDownRefresh();
            }
          })
          .catch((err) => {});
        uni.hideLoading();
      } else {
        uni.hideLoading();
      }
    },

    /**
     *  选中某个复选框时，由checkbox时触发
     */
    checkboxChange(e) {
      // #ifdef MP-WEIXIN
      e.checked = !e.checked;
      // #endif
      this.updateSkuCheckedFun(e.goodsSku.id, e.checked);
    },
  },
};
</script>

<style lang="scss">
page {
  background: #f2f2f2;
}
</style>
<style scoped lang="scss">
// #ifdef MP-WEIXIN
@import "./mp-carui.scss";
// #endif
.u-image {
  box-shadow: 0 4rpx 12rpx 0 rgba(0, 0, 0, 0.05);
}
.promotion-notice {
  margin-top: 10px;
  margin-left: 68rpx;
  font-size: 24rpx;
  color: #333;
  font-weight: bold;
  display: flex;
  align-items: center;
  /deep/ .tips {
    margin: 0 8rpx 0 0;
    background: $main-color;
    border-radius: 100px;
    display: block;
    flex: 1;
    padding: 2rpx 12rpx;
    color: #fff;
  }
}
.default-color {
  color: #333;
}
.goods-row {
  padding: 30rpx 0;

  display: flex;
  align-items: center;
}

.store-name {
  font-weight: bold;
  font-size: 28rpx;
}
.invalid {
  filter: grayscale(1);
}

.cart-item {
  border-radius: 0.4em;
  transition: 0.35s;
}

/* 空白页 */
/deep/ .u-number-input {
  background: #fff !important;
  border: 1px solid #ededed;
  margin: 0 !important;
}

/deep/ .u-icon-minus,
/deep/ .u-icon-plus {
  background: #ffffff !important;
  border: 1px solid #ededed;
  color: #333 !important;
  width: 40rpx;
}

.empty {
  position: fixed;
  left: 0;
  top: 0;
  width: 100%;
  height: 100vh;
  z-index: 99;
  padding-bottom: var(--window-bottom);
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  background: #fff;

  image {
    width: 240rpx;
    height: 160rpx;
    margin-bottom: 30rpx;
  }

  .empty-tips {
    display: flex;
    font-size: $font-sm + 2rpx;
    color: $font-color-disabled;

    .navigator {
      color: $uni-color-primary;
      margin-left: 16rpx;
    }
  }
}

.settlement {
  width: 180rpx;
  height: 70rpx;
  line-height: 70rpx;
  background: linear-gradient(91deg, $light-color 1%, $aider-light-color 99%);
  border-radius: 900rpx;
  text-align: center;
  color: #fff;
  margin-right: 10rpx;
}

.price {
  display: flex;
  align-items: center;

  /deep/ .number {
    line-height: 1;
    font-size: 30rpx;
    > span {
      font-weight: bold;
    }
  }
}

.box2 {
  border-radius: 20rpx;
  padding: 0 16rpx 0;
  margin: 0 16rpx 20rpx;
  .u-checkbox {
    display: flex;
    align-items: center;
    text-align: center;
  }
  background: #fff;

}

.wrapper {
  height: 100%;
}

/deep/ .u-col {
  padding: 24rpx 0 !important;
}

.goods-content {
  width: 100%;
  height: 100%;
  overflow: hidden;
  > p {
    padding-left: 20rpx;
  }
}

.content {
  padding: 20rpx 0 20rpx 0;
  margin-bottom: 80rpx;
}

.line {
  float: left;
  width: 1px;
  height: 100%;
  border-right: 1px solid $light-color;
}

.store-line-check,
.store-line-img,
.store-line-desc {
  // #ifdef MP-WEIXIN
  float: left;
  // #endif
}

.store-line {
  // #ifndef MP-WEIXIN
  display: flex;
  // #endif
  overflow: hidden;
}

.goods-config {
  display: flex;
  align-items: center;
  /deep/ .invalid {
    display: block;
    width: 80rpx !important;
  }
}
.tab {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0 0 0;
}

.right-col {
  color: $light-color;
  font-size: 26rpx;

  > span {
    margin-left: 20rpx;
  }
}

.right-line {
  width: 3px;
  float: left;
  height: 40rpx;
  border-left: 1px solid #eeeeee;

  /deep/ span {
    margin-left: 20rpx;
  }
}

.box6 {
  justify-content: space-between;
  position: fixed;
  // #ifdef APP-PLUS || MP-WEIXIN
  bottom: 0;
  // #endif
  // #ifdef H5
  bottom: var(--window-bottom);
  // #endif
  left: 0;
  border-top: 1px solid #ededed;
  display: flex;
  height: 100rpx;
  overflow: hidden;
  align-items: center;
  width: 100%;
  background: rgba(255, 255, 255, 1);
  color: #333;
  z-index: 99;
  > .navL {
    padding: 0 32rpx;
    display: flex;
    align-items: center;
  }
}

.sp-type {
  color: $u-light-color;
  padding: 10rpx 0;
  font-size: 24rpx;
  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;
}


.sp-number {
  font-weight: bold;

  display: flex;
  justify-content: space-between;
  > .sp-price {
    /deep/ span:nth-of-type(1) {
      font-size: 38rpx;
    }
    /deep/ span:nth-of-type(2) {
      font-size: 24rpx;
    }
  }
}
.priceDetail-flowPrice {
  font-weight: bold;
  padding-left: 20rpx;
  > span:nth-of-type(1) {
    font-size: 38rpx;
  }
}

.prices {
  display: flex;
  flex-direction: column;

  > .discountPrice {
    align-items: center;
    display: flex;
    font-size: 24rpx;
    color: rgb(201, 199, 199);
  }
}
.discount-details {
  margin-left: 10px;
  color: #666;
  padding: 4rpx 10rpx;
  border-radius: 100px;
  background: rgba(201, 199, 199, 0.3);
}
.discount-item {
  display: flex;
  margin: 40rpx 0;
  justify-content: space-between;
  > span:nth-of-type(1) {
    color: #666;
  }
  > span:nth-of-type(2) {
    color: #333;
    font-weight: bold;
  }
}
.discount-title {
  font-size: 36rpx;
  margin-top: 20rpx;
  text-align: center;
}
.discount-way {
  width: 94%;
  margin: 0 3%;
}
.discount-list {
  width: 100%;
}
.promotions-list {
  margin-left: 20rpx;
  > .promotions-item-seckill {
    background: rgba($color: $main-color, $alpha: 0.1);
    font-size: 24rpx;
    color: $main-color;
    display: inline;
    padding: 0rpx 10rpx;
    border-radius: 100px;
  }
}
</style>
