<template>
  <div class="cart" @click="couponAvailable = false">
    <BaseHeader></BaseHeader>
    <!-- LOGO 搜索 -->
    <div class="width_1200 logo">
      <div>
        <router-link to="/"><img :src="$store.state.logoImg" /></router-link>
        <div>
          购物车(<span>{{ goodsTotal }}</span
          >)
        </div>
      </div>
      <Search :showTag="false" :showLogo="false"></Search>
    </div>
    <Divider />
    <!-- 购物车主体 -->
    <div class="cart-content width_1200 center">
      <div class="available-area">
        <div class="cart-steps">
          <span :class="stepIndex == 0 ? 'active' : ''">1.我的购物车</span>
          <Icon
            :class="stepIndex == 0 ? 'active-arrow' : ''"
            custom="icomoon icon-next"
          ></Icon>
          <span :class="stepIndex == 1 ? 'active' : ''">2.填写订单信息</span>
          <Icon
            :class="stepIndex == 1 ? 'active-arrow' : ''"
            custom="icomoon icon-next"
          ></Icon>
          <span :class="stepIndex == 2 ? 'active' : ''">3.成功提交订单</span>
        </div>
      </div>
      <!-- 购物车商品列表 -->
      <div class="cart-goods">
        <div class="cart-goods-title">
          <div class="width_60">
            <Checkbox
              v-model="allChecked"
              @on-change="changeChecked(allChecked, 'all')"
              >全选</Checkbox
            >
          </div>
          <div class="goods-title">商品</div>
          <div class="width_150">单价（元）</div>
          <div class="width_100">数量</div>
          <div class="width_150">小计</div>
          <div class="width_100">操作</div>
        </div>
        <div v-if="cartList.length === 0" class="cart-empty">
          <p>购物车空空如也</p>
          <router-link to="/">去选购&gt;</router-link>
        </div>
        <div
          v-else
          class="cart-goods-items"
          v-for="(shop, index) in cartList"
          :key="index"
        >
          <div class="shop-name">
            <div>
              <Checkbox
                v-model="shop.checked"
                @on-change="changeChecked(shop.checked, 'shop', shop.storeId)"
              ></Checkbox>
              <span class="go-shop-page" @click="goShopPage(shop.storeId)">{{
                shop.storeName
              }}</span>
            </div>
            <span
              class="shop-coupon"
              v-if="shop.couponList.length"
              :class="couponAvailable === index ? 'shop-coupon-show' : ''"
              @click.stop="showCoupon(shop.id, index)"
            >
              <!-- 优惠券模态框 -->
              <div v-if="couponAvailable === index">
                <div
                  class="coupon-item"
                  v-for="(item, index) in shop.couponList"
                  :key="index"
                >
                  <span v-if="item.couponType === 'PRICE'"
                    >￥{{ item.price }}</span
                  >
                  <span v-if="item.couponType === 'DISCOUNT'"
                    >{{ item.couponDiscount }}折</span
                  >
                  <span>满{{ item.consumeThreshold }}元可用</span>
                  <Button
                    class="coupon-btn"
                    size="small"
                    type="primary"
                    @click="receiveShopCoupon(item)"
                    :disabled="item.disabled"
                    >{{ item.disabled ? "已领取" : "领取" }}</Button
                  >
                </div>
              </div>
            </span>
            <div class="promotion-notice">{{ shop.promotionNotice }}</div>
          </div>
          <template v-for="(goods, goodsIndex) in shop.skuList">
            <div class="goods-item" :key="goodsIndex">
              <div class="width_60">
                <Checkbox
                  v-model="goods.checked"
                  @on-change="
                    changeChecked(goods.checked, 'goods', goods.goodsSku.id)
                  "
                ></Checkbox>
              </div>
              <div
                class="goods-title"
                @click="
                  goGoodsDetail(goods.goodsSku.id, goods.goodsSku.goodsId)
                "
              >
                <img
                  :src="
                    goods.goodsSku.thumbnail ||
                    '../assets/images/goodsDetail/item-detail-1.jpg'
                  "
                />
                <div>
                  <p>
                    {{ goods.goodsSku.goodsName }}
                  </p>
                  <p><Tag
                    v-if="goods.goodsSku.salesModel === 'WHOLESALE'"
                    class="goods-show-tag"
                    color="purple"
                  >
                    批发商品
                  </Tag></p>
                  <template
                    v-for="(promotion, promotionIndex) in goods.promotions"
                  >
                    <div
                      class="promotion"
                      :key="promotionIndex"
                      v-if="promotion.promotionType === 'SECKILL'"
                    >
                      <span>秒杀</span>
                      <promotion
                        :time="promotion.endTime"
                        type="cart"
                      ></promotion>
                    </div>
                  </template>
                  <template
                    v-for="(promotion, promotionIndex) in goods.promotions"
                  >
                    <div
                      class="promotion"
                      :key="promotionIndex"
                      v-if="promotion.promotionType === 'FULL_DISCOUNT'"
                    >
                      <span>满优惠活动</span>
                      <promotion
                        :time="promotion.endTime"
                        type="cart"
                      ></promotion>
                    </div>
                  </template>
                </div>
              </div>
              <div class="width_150">
                {{ goods.purchasePrice | unitPrice("￥") }}
              </div>
              <div class="width_100">
                <InputNumber
                  :min="1"
                  size="small"
                  v-model="goods.num"
                  @on-change="changeNum(goods.num, goods.goodsSku.id)"
                ></InputNumber>
                <div class="fontsize_12">
                  {{ goods.goodsSku.quantity > 0 ? "有货" : "无货" }}
                </div>
              </div>
              <div class="width_150">
                {{ goods.subTotal | unitPrice("￥") }}
              </div>
              <div class="width_100">
                <Button
                  v-if="!goods.errorMessage"
                  size="small"
                  type="primary"
                  @click="delGoods(goods.goodsSku.id)"
                  >删除</Button
                >
                <Button
                  v-if="!goods.errorMessage"
                  size="small"
                  type="info"
                  @click="collectGoods(goods.goodsSku.id)"
                  style="margin-left: 10px"
                  >收藏</Button
                >
              </div>
              <div class="error-goods" v-if="goods.errorMessage">
                <div style="margin-top: 20px">{{ goods.errorMessage }}</div>
                <Button type="primary" @click="delGoods(goods.goodsSku.id)"
                  >删除</Button
                >
              </div>
            </div>
          </template>
        </div>
        <!-- 底部支付栏 -->
        <div class="cart-goods-footer">
          <div>
            <div class="width_60">
              <Checkbox
                v-model="allChecked"
                @on-change="changeChecked(allChecked, 'all')"
                >全选</Checkbox
              >
            </div>
            <div class="width_100 handle-btn" @click="delGoods()">
              删除选中商品
            </div>
            <!-- <div class="width_100 handle-btn" @click="collectGoods">移到我的收藏</div> -->
            <div class="width_100 handle-btn" @click="clearCart">
              清空购物车
            </div>
          </div>
          <div>
            <div class="selected-count">
              已选择<span>{{ checkedNum }}</span
              >件商品
            </div>
            <div class="ml_20 save-price">
              已节省<span>{{
                priceDetailDTO.discountPrice | unitPrice("￥")
              }}</span>
            </div>
            <div class="ml_20 total-price">
              总价（不含运费）:
              <div>{{ priceDetailDTO.flowPrice | unitPrice("￥") }}</div>
            </div>
            <div class="pay ml_20" @click="pay">去结算</div>
          </div>
        </div>
      </div>
      <Spin size="large" fix v-if="loading"></Spin>
    </div>
    <BaseFooter></BaseFooter>
  </div>
</template>

<script>
import Promotion from "@/components/goodsDetail/Promotion";
import Search from "@/components/Search";
import * as APICart from "@/api/cart";
import * as APIMember from "@/api/member";
export default {
  name: "Cart",
  beforeRouteEnter(to, from, next) {
    window.scrollTo(0, 0);
    next();
  },
  components: {
    Search,
    Promotion,
  },
  data() {
    return {
      couponAvailable: false, // 展示优惠券
      stepIndex: 0, // 当前处于哪一步，购物车==0，填写订单信息==1，成功提交订单==2
      goodsTotal: 0, // 商品数量
      checkedNum: 0, // 选中数量
      allChecked: false, // 全选
      loading: false, // 加载状态
      cartList: [], // 购物车列表
      couponList: [], // 优惠券列表
      priceDetailDTO: {}, // 价格明细
      skuList: [], // sku列表
    };
  },
  methods: {
    // 跳转商品详情
    goGoodsDetail(skuId, goodsId) {
      let routeUrl = this.$router.resolve({
        path: "/goodsDetail",
        query: { skuId, goodsId },
      });
      window.open(routeUrl.href, "_blank");
    },
    // 跳转店铺首页
    goShopPage(id) {
      let routeUrl = this.$router.resolve({
        path: "/Merchant",
        query: { id },
      });
      window.open(routeUrl.href, "_blank");
    },
    // 收藏商品
    collectGoods(id) {
      this.$Modal.confirm({
        title: "收藏",
        content: "<p>商品收藏后可在个人中心我的收藏查看</p>",
        onOk: () => {
          APIMember.collectGoods("GOODS", id).then((res) => {
            if (res.success) {
              this.$Message.success("收藏商品成功");
              this.getCartList();
            }
          });
        },
        onCancel: () => {},
      });
    },
    // 删除商品
    delGoods(id) {
      const idArr = [];
      if (!id) {
        const list = this.cartList;
        list.forEach((shop) => {
          shop.skuList.forEach((goods) => {
            if (goods.checked) {
              idArr.push(goods.goodsSku.id);
            }
          });
        });
      } else {
        idArr.push(id);
      }
      this.$Modal.confirm({
        title: "删除",
        content: "<p>确定要删除该商品吗？</p>",
        onOk: () => {
          APICart.delCartGoods({ skuIds: idArr.toString() }).then((res) => {
            if (res.success) {
              this.$Message.success("删除成功");
              this.getCartList();
            } else {
              this.$Message.error(res.message);
            }
          });
        },
      });
    },
    // 清空购物车
    clearCart() {
      this.$Modal.confirm({
        title: "提示",
        content: "<p>确定要清空购物车吗？清空后不可恢复</p>",
        onOk: () => {
          APICart.clearCart().then((res) => {
            if (res.success) {
              this.$Message.success("清空购物车成功");
              this.getCartList();
            } else {
              this.$Message.error(res.message);
            }
          });
        },
      });
    },
    // 跳转支付页面
    pay() {
      if (this.checkedNum) {
        this.$router.push({ path: "/pay", query: { way: "CART" } });
      } else {
        this.$Message.warning("请至少选择一件商品");
      }
    },
    // 展示优惠券
    showCoupon(storeId, index) {
      this.couponAvailable = index;
    },
    // 设置购买数量
    changeNum(val, id) {
      if (val) {
        APICart.setCartGoodsNum({ skuId: id, num: val }).then((res) => {
          if (res.success) {
            this.getCartList();
          }
        });
      }
    },
    // 设置商品选中状态
    async changeChecked(status, type, id) {
      const check = status ? 1 : 0;
      if (type === "all") {
        // 全选
        await APICart.setCheckedAll({ checked: check });
      } else if (type === "shop") {
        // 选中店铺所有商品
        await APICart.setCheckedSeller({ checked: check, storeId: id });
      } else {
        // 单个商品
        await APICart.setCheckedGoods({ checked: check, skuId: id });
      }

      this.getCartList();
    },
    // 领取优惠券
    async receiveShopCoupon(item) {
      let res = await APIMember.receiveCoupon(item.id);
      if (res.success) {
        this.$set(item, "disabled", true);
        this.$Message.success("领取成功");
      } else {
        this.$Message.error(res.message);
      }
    },
    // 购物车列表
    async getCartList() {
      this.loading = true;
      try {
        let res = await APICart.cartGoodsAll();
        this.loading = false;
        if (res.success) {
          this.cartList = res.result.cartList;
          this.priceDetailDTO = res.result.priceDetailDTO;
          this.skuList = res.result.skuList;
          this.checkedNum = 0;
          let allChecked = true;
          for (let k = 0; k < this.cartList.length; k++) {
            let shop = this.cartList[k];
            let list = await APIMember.couponList({ storeId: shop.storeId });
            shop.couponList.push(...list.result.records);
          }
          for (let i = 0; i < this.skuList.length; i++) {
            if (this.skuList[i].checked) {
              this.checkedNum += this.skuList[i].num;
            } else {
              allChecked = false;
            }
          }
          this.$forceUpdate();
          this.allChecked = allChecked;
        }
      } catch (error) {
        this.loading = false;
      }
    },
  },
  mounted() {
    this.getCartList();
    APICart.cartCount().then((res) => {
      // 购物车商品数量
      if (res.success) this.goodsTotal = res.result;
    });
  },
};
</script>

<style scoped lang="scss">
/** logo 搜索 start **/
.logo {
  height: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px auto 0;
  div:nth-child(1) {
    display: flex;
    justify-content: space-between;
    align-items: center;
    img {
      width: 150px;
      height: auto;
      cursor: pointer;
    }
    div:nth-child(2) {
      width: 200px;
      color: #999;
      font-size: 16px;
      margin: 0 20px;
      span {
        color: $theme_color;
      }
    }
  }
}
.cart-content {
  margin: 0 auto;
  width: 1200px;
  position: relative;
}
/** logo end */
/** step步骤条 */
.cart-steps {
  height: 30px;
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  span {
    @include content_color($light_content_color);
    height: 30px;
    text-align: center;
    line-height: 30px;
    display: inline-block;
    padding: 0 15px;
  }

  .ivu-icon {
    @include content_color($light_content_color);
    font-size: 20px;
    margin: 0 15px;
  }

  .active {
    border-radius: 50px;
    background-color: #ff8f23;
    color: #fff;
  }

  .active-arrow {
    color: #ff8f23;
  }
}
/** 步骤条和配送区域总体 */
.available-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}
/** 商品列表 */
.cart-goods {
  &-title {
    height: 50px;
    @include background_color($light_white_background_color);
    @include title_color($title_color);
    display: flex;
    align-items: center;
    padding: 0 20px;
    div {
      text-align: center;
    }
    .goods-title {
      flex: 1;
    }
  }
  .cart-empty {
    width: 100%;
    text-align: center;
    height: 300px;
    padding-top: 100px;
  }
  &-items {
    .shop-name {
      height: 50px;
      display: flex;
      align-items: center;
      padding: 0 20px;
      position: relative;
      @include title_color($light_title_color);
      > * {
        width: 50%;
      }
      .go-shop-page:hover {
        color: $theme_color;
        cursor: pointer;
      }
      .customer-service {
        margin-left: 5px;
        color: #fcc217;
        cursor: pointer;
        &:hover {
          color: $theme_color;
        }
      }
      /** 优惠券 */
      .shop-coupon {
        width: 80px;
        height: 24px;
        position: relative;
        background: url(../assets/images/cart-coupon-icons02.png) 0 0 no-repeat;
        > div {
          position: absolute;
          top: 35px;
          left: 0;
          width: 300px;
          height: 300px;
          background-color: #fff;
          border: 1px solid $theme_color;
          // border-radius: 3px;
          z-index: 1;
          padding: 10px 20px;
          &::before {
            content: "";
            display: block;
            background: url(../assets/images/cart-coupon-icons02.png) 0 -58px no-repeat;
            width: 80px;
            height: 12px;
            position: absolute;
            top: -12px;
            left: 0;
          }
          .coupon-item {
            margin-bottom: 10px;
            span:nth-child(1) {
              border: 1px solid #e33937;
              display: inline-block;
              padding: 3px 10px;
              color: $theme_color;
              border-radius: 3px;
            }
            span:nth-child(2) {
              font-size: 12px;
              margin-left: 5px;
              color: #999;
            }
            .coupon-btn {
              height: 26px;
              float: right;
              font-size: 12px;
            }
            &::after {
              display: block;
              content: "";
              clear: right;
            }
          }
        }
      }
      .promotion-notice {
        text-align: right;
        font-size: 12px;
      }
      .shop-coupon-show {
        background-position-y: -34px;
      }
    }
    .goods-item {
      position: relative;
      @extend .cart-goods-title;
      @include background_color($light_white_background_color);
      padding: 10px 20px;
      height: auto;
      > div:nth-child(1) {
        padding-left: 15px;
        width: 30px;
      }
      > div:nth-child(2) {
        cursor: pointer;
        display: flex;
        box-sizing: border-box;
        padding-left: 20px;
        position: relative;
        img {
          width: 70px;
          height: 70px;
        }
        > div > p {
          @include content_color($light_content_color);
          font-size: 13px;
          text-align: left;
          margin-left: 10px;
          &:hover {
            color: $theme_color;
          }
        }
      }
      > div:nth-child(5) {
        font-weight: bold;
      }
      .num-input {
        width: 60px;
        border: 1px solid #999;
        border-radius: 5px;
        padding: 0 5px;
        &:focus {
          outline-color: $theme_color;
        }
      }
    }
    .error-goods {
      position: absolute;
      width: 100%;
      height: 100%;
      margin-left: -20px;
      background-color: rgba($color: #999, $alpha: 0.5);
      z-index: 10;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-left: 150px;
      color: #000;
      padding-right: 30px;
    }
  }
  &-footer {
    @extend .cart-goods-title;
    position: sticky;
    bottom: 0;
    border-top: 1px solid #ddd;
    margin-top: 10px;
    padding: 0 0 0 20px;
    line-height: 50px;
    justify-content: space-between;
    > div {
      display: flex;
    }
    .selected-count {
      span {
        color: $theme_color;
      }
    }

    .save-price span {
      color: #000;
    }
    .total-price div {
      color: $theme_color;
      font-size: 20px;
    }
    .pay {
      background-color: $theme_color;
      width: 150px;
      font-size: 20px;
      color: #fff;
      height: 100%;
      line-height: 50px;
      cursor: pointer;
    }
  }
  .handle-btn {
    font-size: 12px;
    color: $handle-btn-color;
    cursor: pointer;
    &:hover {
      color: $theme_color;
    }
  }
}
.like {
  width: 1200px;
  margin: 10px auto;
  // padding: 20px 0;
  @include white_background_color();
}
.likeGoods,
.shop-nav-container {
  width: 1200px;
  margin: 0 auto;
}
.ivu-divider {
  background: $theme_color;
  height: 2px;
}
.width_150 {
  width: 150px;
}
.width_60 {
  width: 60px;
}
.promotion {
  display: flex;
  margin-top: 5px;
  margin-left: 5px;
  > span {
    border: 1px solid $theme_color;
    color: $theme_color;
    font-size: 12px;
    border-radius: 2px;
    padding: 0 2px;
  }
  > p {
    font-size: 12px;
    margin-left: 10px;
    color: #999;
  }
}
.cart-goods-footer > div {
  display: flex;
  align-items: center;
  overflow: hidden;
}
.total-price {
  display: flex;
  align-items: center;
}
</style>
<style>
.ivu-input-number-input {
  text-align: center;
}
</style>
