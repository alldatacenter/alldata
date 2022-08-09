<template>
  <view class="wrapper" v-if="flage">
    <div class='goods' v-if="selectedGoods">
      <image class="goods-image" :src="selectedGoods.thumbnail" alt="">
        <p class="goodsName">{{selectedGoods.goodsName}}</p>
        <div class="goodsPrice">{{(selectedGoods.promotionPrice || selectedGoods.price  ) | unitPrice('￥')}}</div>
    </div>
    <div>
      <div class="tips">

        <span v-if="master.toBeGroupedNum">
          还差<span class="num">{{master.toBeGroupedNum || 0}}</span>人，赶快邀请好友拼单吧
        </span>
        <span v-if="isBuy &&!master.toBeGroupedNum >0">
          已成功拼团
        </span>
      </div>

      <div v-if="isMaster && !isOver">
        <div class="share-user" v-if="master.toBeGroupedNum" @click="share()">
          邀请好友拼团
        </div>
        <div class="home" @click="handleClickHome()">
          去首页逛逛
        </div>
      </div>
      <div v-if="!isMaster && !isOver && !isBuy">
        <div class="share-user" @click="toBuy">
          参与拼团
        </div>
      </div>
      <div v-if="!isMaster && !isOver && isBuy">
        <div class="share-user disabled">
          已购买该商品
        </div>
      </div>
      <div v-if="isOver">
        <!-- <div class="share-user disabled">
          拼团已结束
        </div> -->
        <div class="home" @click="handleClickHome()">
          去首页逛逛
        </div>
      </div>
    </div>

    <!-- 倒计时 -->
    <div class="count-down" v-if="!isOver">
      <u-count-down bg-color="#ededed" :hide-zero-day="true" @end="isOver" :timestamp="timeStamp"></u-count-down>
    </div>

    <div class="user-list" v-if="data.pintuanMemberVOS">
      <div class="user-item" v-for="(item,index) in data.pintuanMemberVOS" :key="index">
        <span class="master" v-if="item.orderSn == ''">团长</span>
        <image class="img" :src="item.face" alt="">
      </div>
    </div>

    <popupGoods :addr="addr" ref="popupGoods" :buyMask="maskFlag" @closeBuy="closePopupBuy" :goodsDetail="goodsDetail" :goodsSpec="goodsSpec" v-if="goodsDetail.id " @handleClickSku="getGoodsDetail" />
    <shares @close="closeShare" :link="'/pages/cart/payment/shareOrderGoods?sn='+this.routers.sn+'&sku='+this.routers.sku+'&goodsId='+this.routers.goodsId" type="pintuan"
      :thumbnail="data.promotionGoods.thumbnail" :goodsName="data.promotionGoods.goodsName" v-if="shareFlage " />
  </view>
</template>

<script>
import { getGoods } from "@/api/goods.js";
import { getPinTuanShare } from "@/api/order";
import shares from "@/components/m-share/index";
import storage from "@/utils/storage.js";
import popupGoods from "@/components/m-buy/goods"; //购物车商品的模块

export default {
  data() {
    return {
      flage: false, //判断接口是否正常请求
      addr: {
        id: "",
      },
      maskFlag: false, //商品弹框
      timeStamp: 0,
      shareFlage: false,
      data: "",
      isMaster: true,
      selectedGoods: "", //选择的商品规格昵称
      routers: "", //传参数据
      goodsDetail: "", //商品详情
      goodsSpec: "",
      master: "", // 团长
      PromotionList: "", //优惠集合
      isGroup: false, //是否拼团
      isOver: false, //是否结束活动
      isBuy: false, //当前用户是是否购买
    };
  },
  components: {
    shares,
    popupGoods,
  },
  watch: {
    isGroup(val) {
      if (val) {
        let timer = setInterval(() => {
          this.$refs.popupGoods.buyType = "PINTUAN";
          clearInterval(timer);
        }, 100);
      } else {
        this.$refs.popupGoods.buyType = "";
      }
    },
  },
  onLoad(options) {
    this.routers = options;
  },
  mounted() {
    this.init(this.routers.sn, this.routers.sku);
  },
  methods: {
    closeShare() {
      this.shareFlage = false;
    },
    // 这里的话得先跳到商品详情才能购买商品
    toBuy() {
      this.maskFlag = true;
      this.$refs.popupGoods.parentOrder = {
        ...this.master,
        orderSn: this.routers.sn,
      };
      this.$refs.popupGoods.isMask = true;
      this.$refs.popupGoods.isClose = true;
      this.$refs.popupGoods.buyType = "PINTUAN";
    },
    // 分享
    share() {
      this.shareFlage = true;
    },
    closePopupBuy(val) {
      this.maskFlag = false;
    },
    // 实例化本页面
    async init(sn, sku) {
      let res = await getPinTuanShare(sn, sku);
      if (res.data.success && res.data.result.promotionGoods) {
        this.flage = true;
        this.data = res.data.result;
        this.selectedGoods = res.data.result.promotionGoods;
        let endTime = Date.parse(
          res.data.result.promotionGoods.endTime.replace(/-/g, "/")
        );
        // 获取当前剩余的拼团商品时间
        let timeStamp = Date.parse(new Date(endTime)) / 1000;

        // 获取当前时间时间戳
        let dateTime = Date.parse(new Date()) / 1000;

        this.timeStamp = parseInt(timeStamp - dateTime);

        this.timeStamp <= 0 ? (this.isOver = true) : (this.isOver = false);

        // 获取剩余拼团人数
        this.master =
          res.data.result.pintuanMemberVOS.length != 0 &&
          res.data.result.pintuanMemberVOS.filter((item) => {
            return item.orderSn == "";
          })[0];

        // 获取当前是否是拼团本人
        if (
          storage.getUserInfo(this.routers.sku, this.routers.goodsId).id ==
          this.master.memberId
        ) {
          this.isMaster = true;
        } else {
          this.isMaster = false;
          // 获取商品详情
          this.getGoodsDetail({
            id: this.routers.sku,
            goodsId: this.routers.goodsId,
          });
        }

        // 获取当前商品是否已经购买
        if (storage.getUserInfo().id) {
          let isBuy = res.data.result.pintuanMemberVOS.filter((item) => {
            return item.memberId == storage.getUserInfo().id;
          });
          isBuy.length != 0 ? (this.isBuy = true) : (this.isBuy = false);
        }
      } else {
        uni.showToast({
          title: "当前拼团单有误！请联系管理员重试",
          duration: 2000,
          icon: "none",
        });
      }
    },
    // 获取商品详情
    getGoodsDetail(val) {
      let { id, goodsId } = val;
      uni.showLoading({
        title: "加载中",
        mask: true,
      });
      getGoods(id, goodsId).then((response) => {
        this.goodsDetail = response.data.result.data;
        this.selectedGoods = response.data.result.data;
        this.goodsSpec = response.data.result.specs;
        uni.hideLoading();
        this.PromotionList = response.data.result.promotionMap;

        // 判断是否拼团活动 如果有则显示拼团活动信息
        this.PromotionList &&
          Object.keys(this.PromotionList).forEach((item) => {
            if (item.indexOf("PINTUAN") == 0) {
              this.isGroup = true;
            }
          });
      });
    },
    handleClickHome() {
      uni.switchTab({
        url: "/pages/tabbar/home/index",
      });
    },
  },
};
</script>

<style lang="scss" scoped>
page {
  background: #fff;
}
.over {
  margin: 10% 0;
}
.goods {
  display: flex;
  align-content: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
  width: 80%;
  margin: 0 auto;
}
.goods-image {
  margin: 40rpx auto;
  width: 400rpx;
  height: 400rpx;
}
.goodsName {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  font-size: 30rpx;
  font-weight: bold;
}
.goodsPrice {
  margin-top: 10rpx;
  font-weight: bold;
  font-size: 40rpx;
  color: $main-color;
}
.master {
  z-index: 99;
  position: absolute;
  top: 0;
  left: 0;
  background: $light-color;
  padding: 0 8rpx;
  border-radius: 10rpx;
  color: #fff;
}
.user-item {
  position: relative;
  margin: 20rpx;
}
.count-down {
  margin: 40rpx 0;
  text-align: center;
}
.img {
  border-radius: 50%;
  border: 2rpx solid $light-color;
  width: 100rpx;
  height: 100rpx;
}
.user-list {
  width: 80%;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
}
.tips {
  margin-top: 10%;
  text-align: center;
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 100rpx;
}
.num {
  color: $main-color;
  font-size: 60rpx;
  margin: 0 10rpx;
}

.home,
.share-user {
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  width: 80%;
  margin: 30rpx auto 0 auto;
  color: #fff;
  border-radius: 0.4em;
}
.share-user {
  background: $main-color;
}
.disabled {
  background: rgba($main-color, $alpha: 0.2);
}
.home {
  color: $main-color;
  border: 2rpx solid $main-color;
}
</style>
