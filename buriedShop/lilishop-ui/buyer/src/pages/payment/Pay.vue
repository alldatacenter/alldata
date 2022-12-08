<template>
  <div class="pay-order">
    <BaseHeader></BaseHeader>
    <!-- LOGO 步骤条 -->
    <div class="width_1200 logo">
      <div>
        <router-link to="/"><img :src="$store.state.logoImg" /></router-link>
        <div>结算页</div>
      </div>
      <div class="cart-steps">
        <span :class="stepIndex == 1 ? 'active' : ''">1.我的购物车</span>
        <Icon
          :class="stepIndex == 1 ? 'active-arrow' : ''"
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
    <Divider />
    <div class="content width_1200">
      <!-- 收货地址 -->
      <div class="address">
        <div class="card-head">
          <span>收货人信息</span>
          <span @click="goAddressManage">管理收货人地址</span>
        </div>
        <div class="address-manage">
          <div
            class="address-item"
            v-show="moreAddr ? true : index < 3"
            :class="selectedAddress.id === item.id ? 'border-red' : ''"
            @mouseenter="showEditBtn = index"
            @mouseleave="showEditBtn = ''"
            @click="selectAddress(item)"
            v-for="(item, index) in addressList"
            :key="index"
          >
            <div>
              <span>{{ item.name }}</span>
              <Tag class="ml_10" v-if="item.isDefault" color="red">默认</Tag>
              <Tag class="ml_10" v-if="item.alias" color="warning"
                >{{ item.alias }}
              </Tag>
            </div>
            <div>{{ item.mobile }}</div>
            <div>
              {{ item.consigneeAddressPath | unitAddress }} {{ item.detail }}
            </div>
            <div class="edit-btn" v-show="showEditBtn === index">
              <span @click.stop="editAddress(item.id)">修改</span>
              <span
                class="ml_10"
                v-if="!item.isDefault"
                @click.stop="delAddress(item)"
                >删除</span
              >
            </div>
            <div class="corner-icon" v-show="selectedAddress.id === item.id">
              <div></div>
              <Icon type="md-checkmark" />
            </div>
          </div>
          <div class="add-address" @click="editAddress('')">
            <Icon type="ios-add-circle-outline" />
            <div>添加新地址</div>
          </div>
        </div>

        <div
          class="more-addr"
          @click="moreAddr = !moreAddr"
          v-if="addressList.length > 3"
        >
          {{ moreAddr ? "收起地址" : "更多地址" }}
          <Icon v-show="!moreAddr" type="md-arrow-dropdown" />
          <Icon v-show="moreAddr" type="md-arrow-dropup" />
        </div>
      </div>
      <!-- 商品信息 -->
      <div class="goods-content">
        <div class="card-head mt_20 mb_20">
          <span>商品信息</span>
          <span @click="$router.push('/cart')">返回购物车</span>
        </div>
        <div
          class="goods-msg"
          v-for="(shop, shopIndex) in goodsList"
          :key="shopIndex"
        >
          <div v-if="shop.checked">
            <div class="shop-name">
              <span>
                <span class="hover-color" @click="goShopPage(shop.storeId)">{{
                  shop.storeName
                }}</span
                >&nbsp;&nbsp;
              </span>
            </div>
            <div class="goods-list">
              <div
                class="goods-item"
                v-for="(goods, goodsIndex) in shop.checkedSkuList"
                :key="goodsIndex"
              >
                <span
                  class="hover-color"
                  @click="
                    goGoodsDetail(goods.goodsSku.id, goods.goodsSku.goodsId)
                  "
                >
                  <img :src="goods.goodsSku.thumbnail" alt="" />
                  <span style="vertical-align: top">{{
                    goods.goodsSku.goodsName
                  }}</span>
                </span>
                <span class="goods-price">{{
                  goods.purchasePrice | unitPrice("￥")
                }}</span>
                <span>x{{ goods.num }}</span>
                <span>{{ goods.goodsSku.quantity > 0 ? "有货" : "无货" }}</span>
                <span class="goods-price">{{
                  goods.subTotal | unitPrice("￥")
                }}</span>
              </div>
            </div>
            <div class="order-mark">
              <Input
                type="textarea"
                maxlength="60"
                v-model="shop.remark"
                show-word-limit
                placeholder="订单备注"
              />
              <span style="font-size: 12px; color: #999"
                >提示：请勿填写有关支付、收货、发票方面的信息</span
              >
            </div>
          </div>
        </div>
      </div>
      <!-- 发票信息 -->
      <div class="invoice">
        <div class="card-head mt_20 mb_20">
          <span class="relative"
            >发票信息<span class="inv-tips">
              <Icon
                type="ios-alert-outline"
              />开企业抬头发票须填写纳税人识别号，以免影响报销
            </span></span
          >
        </div>
        <div class="inovice-content">
          <span>{{ invoiceData.receiptTitle }}</span>
          <span>{{ invoiceData.receiptContent }}</span>
          <span @click="editInvoice">编辑</span>
        </div>
      </div>
      <!-- 优惠券 -->
      <div class="invoice">
        <div class="card-head mt_20 mb_20">
          <span class="relative">优惠券</span>
        </div>
        <div v-if="couponList.length === 0">无可用优惠券</div>
        <ul v-else class="coupon-list">
          <li
            v-for="(item, index) in couponList"
            class="coupon-item"
            :key="index"
          >
            <div class="c-left">
              <div>
                <span
                  v-if="item.couponType === 'PRICE'"
                  class="fontsize_12 global_color"
                  >￥<span class="price">{{
                    item.price | unitPrice
                  }}</span></span
                >
                <span
                  v-if="item.couponType === 'DISCOUNT'"
                  class="fontsize_12 global_color"
                  ><span class="price">{{ item.discount }}</span
                  >折</span
                >
                <span class="describe"
                  >满{{ item.consumeThreshold }}元可用</span
                >
              </div>
              <p>使用范围：{{ useScope(item.scopeType) }}</p>
              <p>有效期：{{ item.endTime }}</p>
            </div>
            <img
              class="used"
              v-if="usedCouponId.includes(item.id)"
              src="../../assets/images/geted.png"
              alt=""
            />
            <b></b>
            <a class="c-right" @click="useCoupon(item.id, true)">立即使用</a>
            <a
              class="c-right"
              v-if="usedCouponId.includes(item.id)"
              @click="useCoupon(item.id, false)"
              >放弃优惠</a
            >
            <i class="circle-top"></i>
            <i class="circle-bottom"></i>
          </li>
        </ul>
      </div>
      <!-- 订单价格 -->
      <div class="order-price">
        <div>
          <span>{{ totalNum }}件商品，总商品金额：</span
          ><span>{{ priceDetailDTO.goodsPrice | unitPrice("￥") }}</span>
        </div>
        <div v-if="priceDetailDTO.freightPrice > 0">
          <span>运费：</span
          ><span>{{ priceDetailDTO.freightPrice | unitPrice("￥") }}</span>
        </div>
        <div v-if="priceDetailDTO.discountPrice > 0">
          <span>优惠金额：</span
          ><span>-{{ priceDetailDTO.discountPrice | unitPrice("￥") }}</span>
        </div>
        <div v-if="priceDetailDTO.couponPrice > 0">
          <span>优惠券金额：</span
          ><span>-{{ priceDetailDTO.couponPrice | unitPrice("￥") }}</span>
        </div>

        <div v-if="$route.query.way === 'POINTS'">
          <span>应付积分：</span
          ><span class="actrual-price">{{ priceDetailDTO.payPoint }}</span>
        </div>
        <div v-else>
          <span>应付金额：</span
          ><span class="actrual-price">{{
            priceDetailDTO.flowPrice | unitPrice("￥")
          }}</span>
        </div>
      </div>
    </div>
    <!-- 底部支付栏 -->
    <div class="order-footer width_1200">
      <div class="pay ml_20" @click="pay">提交订单</div>
      <div class="pay-address" v-if="addressList.length">
        配送至：{{ selectedAddress.consigneeAddressPath | unitAddress }}
        {{ selectedAddress.detail }}&nbsp;&nbsp;收货人：{{
          selectedAddress.name
        }}&nbsp;&nbsp;{{ selectedAddress.mobile }}
      </div>
    </div>
    <BaseFooter></BaseFooter>
    <!-- 添加发票模态框 -->
    <invoice-modal
      ref="invModal"
      :invoiceData="invoiceData"
      @change="getInvMsg"
    />
    <!-- 选择地址模态框 -->
    <address-manage
      ref="address"
      :id="addrId"
      @change="addrChange"
    ></address-manage>
  </div>
</template>

<script>
import invoiceModal from "@/components/invoiceModal";
import addressManage from "@/components/addressManage";
import { memberAddress, delMemberAddress } from "@/api/address";
import {
  cartGoodsPay,
  createTrade,
  selectAddr,
  selectCoupon,
  couponNum,
} from "@/api/cart";
import { canUseCouponList } from "@/api/member.js";

export default {
  name: "Pay",
  components: { invoiceModal, addressManage },
  data() {
    return {
      stepIndex: 1, // 顶部步骤条状态
      invoiceAvailable: false, // 发票编辑按钮
      showEditBtn: "", // 鼠标移入显示编辑按钮
      orderMark: "", // 订单备注
      invoiceData: {
        // 发票数据
        receiptTitle: "个人",
        receiptContent: "不开发票",
      },
      addressList: [], // 地址列表
      selectedAddress: {}, // 所选地址
      goodsList: [], // 商品列表
      priceDetailDTO: {}, // 商品价格
      totalNum: 0, // 购买数量
      addrId: "", // 编辑地址传入的id
      moreAddr: false, // 更多地址
      canUseCouponNum: 0, // 可用优惠券数量
      couponList: [], // 可用优惠券列表
      usedCouponId: [], // 已使用优惠券id
      selectedCoupon: {}, // 已选优惠券对象
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    // 初始化数据
    init() {
      this.getGoodsDetail();
    },
    goAddressManage() {
      // 跳转地址管理页面
      this.$router.push("/home/MyAddress");
    },
    getAddress() {
      // 获取收货地址列表
      memberAddress().then((res) => {
        if (res.success) {
          this.addressList = res.result.records;
          this.addressList.forEach((e, index) => {
            if (e.id === this.selectedAddress.id && index > 2) {
              this.moreAddr = true;
            }
          });
        }
      });
    },
    getGoodsDetail() {
      // 订单商品详情
      this.$Spin.show();
      cartGoodsPay({ way: this.$route.query.way })
        .then((res) => {
          this.$Spin.hide();
          if (res.success) {
            if (
              !res.result.checkedSkuList ||
              res.result.checkedSkuList.length === 0
            ) {
              if (res.result.skuList && res.result.skuList[0]) {
                this.$Modal.warning({
                  title: "购物车存在无效商品！",
                  content:
                    "[" +
                    res.result.skuList[0].goodsSku.goodsName +
                    "]" +
                    res.result.skuList[0].errorMessage,
                });
              }
              this.$router.push({
                path: "/cart",
                replace: true,
              });
            }
            this.goodsList = res.result.cartList;
            this.priceDetailDTO = res.result.priceDetailDTO;
            this.skuList = res.result.skuList;

            if (res.result.receiptVO) {
              this.invoiceData = res.result.receiptVO;
            }
            let notSupArea = res.result.notSupportFreight;
            this.selectedCoupon = {};
            if (res.result.platformCoupon)
              this.selectedCoupon[res.result.platformCoupon.memberCoupon.id] =
                res.result.platformCoupon;
            if (
              res.result.storeCoupons &&
              Object.keys(res.result.storeCoupons)[0]
            ) {
              let storeMemberCouponsId = Object.keys(
                res.result.storeCoupons
              )[0];
              let storeCouponId =
                res.result.storeCoupons[storeMemberCouponsId].memberCoupon.id;
              this.selectedCoupon[storeCouponId] =
                res.result.storeCoupons[storeMemberCouponsId];
            }
            if (notSupArea) {
              let content = [];
              let title = "";
              notSupArea.forEach((e) => {
                title = e.errorMessage;
                content.push(e.goodsSku.goodsName);
              });
              this.$Modal.warning({
                title: "以下商品超出配送区域" || title,
                content: content.toString(),
              });
            }
            if (res.result.memberAddress) {
              this.selectedAddress = res.result.memberAddress;
            }
            this.getAddress();
            this.totalNum = 0;
            for (let i = 0; i < this.skuList.length; i++) {
              this.totalNum += this.skuList[i].num;
            }
            this.usedCouponId = [];
            this.couponList = res.result.canUseCoupons;
            const couponKeys = Object.keys(this.selectedCoupon);
            if (couponKeys.length) {
              this.couponList.forEach((e) => {
                if (
                  this.selectedCoupon[e.id] &&
                  e.id === this.selectedCoupon[e.id].memberCoupon.id
                ) {
                  this.usedCouponId.push(e.id);
                }
              });
              this.$nextTick(() => {
                this.$forceUpdate();
              });
            }
          }
        })
        .catch(() => {
          this.$Spin.hide();
        });
    },
    getCouponNum() {
      // 获取可用优惠券数量
      couponNum({ way: this.$route.query.way }).then((res) => {
        this.canUseCouponNum = res.result;
        if (res.result) {
          let storeArr = [];
          let skuArr = [];
          this.goodsList.forEach((e) => {
            storeArr.push(e.storeId);
            e.skuList.forEach((i) => {
              skuArr.push(i.goodsSku.id);
            });
          });
          let params = {
            pageNumber: 1,
            pageSize: 100,
            memberCouponStatus: "NEW",
            scopeId: skuArr.toString(),
            storeId: storeArr.toString(),
            totalPrice: this.priceDetailDTO.goodsPrice,
          };
          canUseCouponList(params).then((res) => {
            // 可用优惠券列表
            if (res.success) this.couponList = res.result.records;
            const couponKeys = Object.keys(this.selectedCoupon);
            this.usedCouponId = [];
            if (couponKeys.length) {
              this.couponList.forEach((e) => {
                if (e.id === this.selectedCoupon[couponKeys].memberCoupon.id) {
                  this.usedCouponId.push(e.id);
                }
              });
              this.$nextTick(() => {
                this.$forceUpdate();
              });
            }
          });
        }
      });
    },
    selectAddress(item) {
      // 选择地址
      let params = {
        way: this.$route.query.way,
        shippingAddressId: item.id,
      };
      selectAddr(params).then((res) => {
        if (res.success) {
          this.$Message.success("选择收货地址成功");
          this.selectedAddress = item;
          this.getGoodsDetail();
        }
      });
    },
    editAddress(id) {
      // 编辑地址
      this.addrId = id;
      this.$refs.address.show();
    },
    addrChange() {
      // 添加，编辑地址回显
      this.getAddress();
    },
    delAddress(item) {
      // 删除地址
      this.$Modal.confirm({
        title: "提示",
        content: "你确定删除这个收货地址",
        onOk: () => {
          delMemberAddress(item.id).then((res) => {
            if (res.success) {
              this.$Message.success("删除成功");
              this.getAddress();
            }
          });
        },
        onCancel: () => {},
      });
    },
    goGoodsDetail(skuId, goodsId) {
      // 跳转商品详情
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
        query: { id: id },
      });
      window.open(routeUrl.href, "_blank");
    },
    useCoupon(id, used) {
      // 使用优惠券
      let params = {
        way: this.$route.query.way,
        memberCouponId: id,
        used: used, // true 为使用， false为弃用
      };
      selectCoupon(params).then((res) => {
        if (res.success) this.init();
      });
    },
    editInvoice() {
      // 编辑发票信息
      this.$refs.invModal.invoiceAvailable = true;
    },
    getInvMsg(item) {
      // 获取发票信息
      if (item) {
        this.init();
        this.$refs.invModal.invoiceAvailable = false;
      }
    },

    pay() {
      // 结算
      const params = {
        client: "PC",
        remark: [],
        way: this.$route.query.way,
      };
      this.goodsList.forEach((e) => {
        if (e.remark) {
          params.remark.push({
            remark: e.remark,
            storeId: e.storeId,
          });
        }
      });

      if (!params.remark.length) delete params.remark;

      this.$Spin.show();
      createTrade(params)
        .then((res) => {
          this.$Spin.hide();
          if (res.success) {
            if (params.way === "POINTS") {
              // 积分支付不需要跳转支付页面
              this.$router.push("/payDone");
            } else {
              this.$router.push({
                path: "/payment",
                query: { orderType: "TRADE", sn: res.result.sn },
              });
            }
          }
        })
        .catch(() => {
          this.$Spin.hide();
        });
    },
    // 优惠券可用范围
    useScope(type) {
      let goods = "全部商品";
      switch (type) {
        case "ALL":
          goods = "全部商品";
          break;
        case "PORTION_GOODS":
          goods = "部分商品";
          break;
        case "PORTION_GOODS_CATEGORY":
          goods = "部分分类商品";
          break;
      }
      return `${goods}可用`;
    },
  },
};
</script>

<style scoped lang="scss">
@import "../../assets/styles/coupon.scss";
.goods-msg {
  overflow: hidden;
}
/** logo start */
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

.cart-steps {
  height: 30px;
  display: flex;
  align-items: center;

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

/** logo end */
/** content start */
.content {
  margin: 20px auto;
  background-color: #fff;
  min-height: 200px;
  padding: 15px 25px;
}

/** 地址管理 */
.address-manage {
  display: flex;
  flex-wrap: wrap;

  > div {
    border: 1px dotted #949494;
    width: 265px;
    height: 120px;
    margin: 20px 20px 0 0;
    padding: 10px;
    cursor: pointer;
    color: #999;
  }

  .add-address {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;

    .ivu-icon {
      font-size: 24px;
    }
  }

  .address-item {
    position: relative;
    font-size: 12px;

    > div:nth-child(1) {
      margin-bottom: 10px;

      span {
        margin-right: 10px;
      }

      > span:nth-child(1) {
        color: #000000;
        font-size: 14px;
      }
    }

    .edit-btn {
      font-size: 12px;
      position: absolute;
      top: 15px;
      right: 20px;
      color: $theme_color;

      span:hover {
        border-bottom: 1px solid $theme_color;
      }
    }

    .corner-icon {
      position: absolute;
      right: -1px;
      bottom: -1px;

      div {
        width: 0;
        border-top: 20px solid transparent;
        border-right: 20px solid $theme_color;
      }

      .ivu-icon {
        font-size: 12px;
        position: absolute;
        bottom: 0;
        right: 1px;
        transform: rotate(-15deg);
        color: #fff;
      }
    }
  }

  .border-red {
    border-color: $theme_color;
  }
}

/** 购买商品列表 start */
.shop-name {
  display: flex;
  justify-content: space-between;

  > span:nth-child(1) {
    font-weight: bold;

    .ivu-icon {
      color: #ff8f23;

      &:hover {
        color: $theme_color;
      }
    }
  }

  > span:nth-child(2) {
    color: #999;
    position: relative;
    display: flex;
    width: 200px;
  }

  .delivery-list {
    position: absolute;
    right: 0;
    top: 20px;
    background-color: #f3fafe;
    box-shadow: 0px 0px 5px #b9b2b2;
    display: flex;
    flex-wrap: wrap;
    width: 200px;
    min-height: 100px;
    padding: 10px;

    li {
      width: 90px;
      height: 30px;
      text-align: center;

      &:hover {
        cursor: pointer;
      }
    }
  }
}

.goods-list {
  width: 1150px;
  background-color: #f8f8f8;
  margin: 10px 0 20px 0;

  .goods-item {
    display: flex;
    width: 100%;
    align-items: center;
    justify-content: space-between;
    padding: 20px 0;
    margin: 0 20px;
    border-bottom: 1px dotted #999;

    &:last-child {
      border: none;
    }

    img {
      width: 48px;
      height: 48px;
    }

    > span {
      text-align: center;
      width: 100px;
    }

    > span:nth-child(1) {
      font-size: 12px;

      flex: 1;
      text-align: left;
      > span {
        margin-left: 10px;
      }
    }

    > span:last-child {
      color: $theme_color;
      font-weight: bold;
    }

    .goods-price {
      font-size: 16px;
    }
  }
}

.order-mark {
  width: 500px;
}

/** 购买商品列表 end */
/** 发票信息 start */
.invoice {
  .inv-tips {
    position: absolute;
    border: 1px solid #ddd;
    width: 310px;
    padding: 3px;
    margin: 0 0 0 10px;
    font-size: 12px !important;
    box-shadow: 0 0 3px rgba(0, 0, 0, 0.15);

    &::before {
      content: "";
      display: inline-block;
      width: 12px;
      height: 17px;
      background: url(../../assets/images/arrow-left.png) 0 0 no-repeat;
      background-color: #fff;
      position: absolute;
      left: -9px;
    }

    .ivu-icon {
      color: #ff8f23;
      margin-right: 3px;
      font-size: 16px;
      font-weight: bold;
    }
  }

  .inovice-content {
    > span {
      margin-right: 10px;
    }

    > span:last-child {
      color: $theme_color;
      cursor: pointer;

      &:hover {
        border-bottom: 1px solid $theme_color;
      }
    }
  }
}

/** 发票信息 end */

/** 订单价格 */
.order-price {
  text-align: right;
  margin-top: 30px;
  font-size: 16px;
  color: #999;

  > div > span:nth-child(2) {
    width: 130px;
    text-align: right;
    display: inline-block;
    margin-top: 10px;
  }

  .actrual-price {
    color: $theme_color;
    font-weight: bold;
    font-size: 20px;
  }
}

/** content end */
/** 底部支付栏 */
.order-footer {
  z-index: 20;
  height: 50px;
  @include background_color($light_white_background_color);
  @include title_color($title_color);
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  border-top: 1px solid #ddd;
  margin: 10px auto;

  div {
    text-align: center;
  }

  position: sticky;
  bottom: 0;

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

/** 公共表头 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #dddddd;
  height: 40px;

  span:nth-child(1) {
    font-size: 18px;
  }

  span:nth-child(2) {
    font-size: 12px;
    color: #438cde;
    cursor: pointer;

    &:hover {
      color: $theme_color;
    }
  }
}

.ivu-divider {
  background: $theme_color;
  height: 2px;
}

.pay-address {
  font-size: 12px;
}

.more-addr {
  cursor: pointer;
  margin-top: 10px;
  display: inline-block;
}

.coupon-item {
  width: 260px;
  height: 125px;
  margin-right: 10px;
  margin-bottom: 10px;

  .c-right {
    width: 30px;
    padding: 10px 7px;
  }

  b {
    background: url("../../assets/images/small-circle.png") top left repeat-y;
    right: 28px;
  }

  .circle-top,
  .circle-bottom {
    right: 22px;
  }

  .used {
    position: absolute;
    top: 60px;
    right: 40px;
    width: 50px;
    height: 50px;
  }
}

.coupon-list {
  max-height: 260px;
  overflow: scroll;
}
</style>
