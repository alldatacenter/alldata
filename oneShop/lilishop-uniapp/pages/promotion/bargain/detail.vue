<template>
  <div class="page">
    <u-navbar :custom-back="back" back-icon-color="#fff" :background="background" :border-bottom="false">
    </u-navbar>

    <div class="wrapper">
      <!-- 砍价列表 -->
      <div class="box box1">
        <div class="bargain">
          <div class="flex bargain-item">
            <div class="goods-img">
              <u-image width="200" height="200" :src="bargainDetail.thumbnail"></u-image>
            </div>
            <div class="goods-config">
              <div class="goods-title wes-2">
                {{bargainDetail.goodsName}}
              </div>
              <div class="flex price-box">
                <div class="purchase-price">
                  当前:<span>￥{{ activityData.surplusPrice == 0 ? this.bargains.purchasePrice :  activityData.surplusPrice | unitPrice}}</span>
                </div>
                <div class="max-price">原价:<span>￥{{ bargainDetail.price | unitPrice}}</span>

                </div>
              </div>
              <div class="tips">{{bargainDetail.sellingPoint}}</div>
            </div>
          </div>
          <!-- 砍价进度 -->
          <div class="bargain-progress">
            <u-line-progress class="line" :active-color="lightColor" striped striped-active :percent="totalPercent">
            </u-line-progress>
            <div class="flex tips">
              <div>已砍{{cutPrice}}元</div>
              <div>还剩{{activityData.surplusPrice}}元</div>
            </div>
          </div>
          <!-- 参与砍价 -->
          <div class="bargaining" v-if="!activityData.pass && activityData.status!='END'" @click="shareBargain">
            邀请砍价
          </div>
          <!-- 立即购买 -->
          <div v-else>

            <div v-if="activityData.status!='END'" class="buy" @click="getGoodsDetail">
              立即购买
            </div>
          </div>
          <!-- 我要开团 -->
          <div class="start" v-if="activityData.memberId != $options.filters.isLogin().id" @click="startOpenGroup">我要开团
          </div>
        </div>
      </div>
      <!-- 帮砍列表 -->
      <div class="box box2">
        <div class="bargain">
          <div class="bargain-title">帮忙砍</div>
          <div class="user-item flex" v-if="logData.length !=0 " v-for="(item,index) in logData" :key="index">
            <div>
              <u-image width="75" shape="circle" height="75" :src="item.kanjiaMemberFace"></u-image>
            </div>
            <div class="user-config flex">
              <div class="user-name">
                <div>{{item.kanjiaMemberName | noPassByName}}</div>
                <div>使出吃的奶劲儿</div>
              </div>
              <div class="save">砍掉：<span>￥{{item.kanjiaPrice | unitPrice}}</span></div>
            </div>
          </div>
        </div>
      </div>
      <!-- 产品详情 -->
      <div class="box box3">
        <div class="bargain">
          <div class="bargain-title">商品详情</div>
          <view class="u-content">
            <u-parse :html="bargainDetail.mobileIntro"></u-parse>
          </view>

        </div>
      </div>

      <!-- 砍价 -->
      <u-modal title="恭喜您砍掉了" v-model="Bargaining" mask-close-able :show-confirm-button="false"
        :title-style="{color: lightColor}">
        <view class="slot-content">
          <u-count-to :start-val="0" ref="uCountTo" font-size="100" :color="lightColor" :end-val="kanjiaPrice"
            :decimals="2" :autoplay="autoplay"></u-count-to><span class="price">元</span>
        </view>
      </u-modal>
      <!-- 帮砍 -->

      <u-modal :show-title="false" v-model="helpBargainFlage" :show-confirm-button="false">
        <view class="help-bargain" @click="handleClickHelpBargain">
          <u-image width="100%" height="600rpx"
            src="https://lilishop-oss.oss-cn-beijing.aliyuncs.com/91631d5a66c7426bbe3f7d644ee41946.jpeg"></u-image>
          <u-image class="help" width="300rpx" height="80rpx" src="/pages/promotion/static/help-bargain.png"></u-image>
        </view>
      </u-modal>

      <!-- 分享 -->
      <shares @close="closeShare" :link="share()" type="kanjia" :thumbnail="bargainDetail.thumbnail"
        :goodsName="bargainDetail.goodsName" v-if="shareFlage " />

      <!-- 购买 -->
      <popupGoods ref="popupGoods" :buyMask="maskFlag" @closeBuy="closePopupBuy" :goodsDetail="bargainDetail"
        :goodsSpec="goodsSpec" v-if="bargainDetail.id " @handleClickSku="getGoodsDetail" />

      <!-- 产品详情 -->
      <div class=" box4">

      </div>
    </div>
  </div>
</template>

<script>
import popupGoods from "@/components/m-buy/goods"; //购物车商品的模块
import {
  getBargainDetail,
  getBargainActivity,
  openBargain,
  getBargainLog,
  helpBargain,
} from "@/api/promotions";
import shares from "@/components/m-share/index";
import config from "@/config/config";
export default {
  components: {
    shares,
    popupGoods,
  },
  data() {
    return {
      background: {
        backgroundColor: "transparent",
      },
      maskFlag: false, //商品弹框
      shareFlage: false,
      lightColor: this.$lightColor,
      bargains: {},
      bargainDetail: {}, //砍价商品详情
      Bargaining: false, //砍价弹出框
      helpBargainFlage: false, //帮砍弹出框
      autoplay: false, //砍价金额滚动
      kanjiaPrice: 0, //砍价金额
      totalPercent: 0, //砍价半分比
      activityData: "", //砍价活动
      cutPrice: 0, //已砍金额
      params: {
        // id: "", //砍价活动ID
        // kanjiaActivityGoodsId: "", //砍价商品SkuID
        // kanjiaActivityId: "", //邀请活动ID，有值说明是被邀请人
        // status: "", //状态
      },

      logData: [], // 帮砍记录
      //获取帮砍记录参数
      logParams: {
        pageNumber: 1,
        pageSize: 20,
        kanJiaActivityId: "",
      },

      goodsDetail: {}, //商品详情
      goodsSpec: {}, //商品规格
      selectedGoods: "", //选择的商品
    };
  },
  onLoad(options) {
    this.routerVal = options;
    if (options.activityId) {
      this.params.kanjiaActivityId = options.activityId;
    }
  },

  // #ifdef MP-WEIXIN
  onShareAppMessage(res) {
    return {
      path: this.share(),
      title: `请快来帮我砍一刀${this.bargainDetail.goodsName}`,
      imageUrl: this.bargainDetail.thumbnail || config.logo,
    };
  },
  // #endif

  onShow() {
    this.init();
  },
  watch: {
    // 砍价弹窗
    Bargaining(val) {
      if (val) {
        this.$nextTick(() => {
          this.$refs.uCountTo.start();
        });
      }
    },
    // 监听砍价活动金额
    activityData: {
      handler(val) {
        if (val) {
          // 计算砍价百分比
          this.totalPercent =
            100 -
            Math.floor((val.surplusPrice / this.bargainDetail.price) * 100);
          this.cutPrice = (
            this.bargainDetail.price - this.activityData.surplusPrice
          ).toFixed(2);
          // 获取砍价日志
          this.logParams.kanJiaActivityId = val.id;

          // 判断是否是帮砍
          if (this.params.kanjiaActivityId && val.help) {
            this.helpBargainFlage = true;
          }

          this.getBargainLogList();
        }
      },
      immediate: true,
    },
  },
  methods: {
    share() {
      return (
        "/pages/promotion/bargain/detail?id=" +
        this.routerVal.id +
        "&activityId=" +
        this.activityData.id
      );
    },
    // 返回上一级
    back() {
      // 进行路由栈判定如果当前路由栈是空的就返回拼团列表页面
      if (getCurrentPages().length > 1) {
        uni.navigateBack();
      } else {
        uni.redirectTo({
          url: `/pages/promotion/bargain/list`,
        });
      }
    },

    // 跳转选择商品页面
    startOpenGroup() {
      uni.redirectTo({
        url: `/pages/promotion/bargain/list`,
      });
    },
    closePopupBuy(val) {
      this.maskFlag = false;
    },
    closeShare() {
      this.shareFlage = false;
    },
    // 邀请砍价
    shareBargain() {
      this.shareFlage = true;
    },

    // 获取商品详情
    getGoodsDetail() {
      uni.showLoading({
        title: "加载中",
        mask: true,
      });
      this.$refs.popupGoods.buy({
        skuId: this.bargainDetail.id,
        id: this.routerVal.id,
        num: 1,
        cartType: "KANJIA",
      });
    },

    // 初始化商品以及砍价活动
    async init() {
      // 获取商品
      let res = await getBargainDetail(this.routerVal.id);
      if (res.data.success) {
        this.bargainDetail = res.data.result.goodsSku;
        this.bargains = res.data.result;
        // 被邀请活动id
        if (this.params.kanjiaActivityId) {
        } else {
          this.params.kanjiaActivityGoodsId = this.routerVal.id;
        }
        // 获取砍价活动
        this.activity();
      }
    },
    // 获取砍价活动
    async activity() {
      let res = await getBargainActivity(this.params);
      // 判断当前是否是第一次进入，如果是第一次进入默认砍一刀
      res.data.success
        ? res.data.result.launch
          ? (this.activityData = res.data.result)
          : this.openActivity()
        : "";
    },
    // 分页获取砍价活动-帮砍记录
    async getBargainLogList() {
      let res = await getBargainLog(this.logParams);
      if (res.data.success) {
        this.logData = res.data.result.records;
      }
    },
    // 帮忙砍一刀
    async handleClickHelpBargain() {
      let res = await helpBargain(this.params.kanjiaActivityId);
      if (res.data.success) {
        this.helpBargainFlage = false;
        this.kanjiaPrice = res.data.result.kanjiaPrice;
        this.Bargaining = true;
        // 帮砍完成之后查询帮砍记录
        this.init();
      } else {
        this.helpBargainFlage = false;
      }
    },
    // 发起砍价活动
    async openActivity(data) {
      let res = await openBargain({ id: this.routerVal.id });
      if (res.data.success) {
        this.kanjiaPrice = res.data.result.kanjiaPrice;
        this.Bargaining = true;
        // 查询帮砍记录
        this.init();
      }
    },
  },
};
</script>
<style lang="scss">
page {
  background-color: $light-color !important;
}
</style>
<style lang="scss" scoped>
.slot-content {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  margin: 20rpx 0 80rpx 0;
}
.price {
  margin-left: 10rpx;
  color: $light-color;
}
.price-box {
  align-items: center;
  padding: 10rpx 0;
}
.wrapper {
  background: url("https://lilishop-oss.oss-cn-beijing.aliyuncs.com/aac88f4e8eff452a8010af42c4560b04.png");
  background-repeat: no-repeat;
  background-size: 100% 100%;
  height: 700rpx;
  width: 100%;
}

.box {
  background: #fff;
  border-radius: 20rpx;
  position: relative;
  width: 94%;
  margin: 0 auto;
  > .bargain {
    padding: 32rpx;
  }
}
.box1 {
  top: 750rpx;
}
.box2 {
  top: 770rpx;
}
.box3 {
  top: 790rpx;
}
.box4 {
  top: 810rpx;
  height: 200rpx;
}
.bargain-item {
  align-items: center;
}
.goods-config {
  margin-left: 20rpx;
  > .goods-title {
    font-weight: bold;
  }
}
.max-price,
.purchase-price {
  font-size: 24rpx;
  color: #999;
}
.max-price {
  margin-left: 10rpx;
  text-decoration: line-through;
}
.purchase-price {
  color: $main-color;
  > span {
    font-size: 32rpx;
    font-weight: bold;
  }
}
.bargaining,
.buy,
.start {
  font-size: 24rpx;

  width: 80%;
  margin: 50rpx auto 0 auto;
  text-align: center;

  font-size: 30rpx;
  padding: 18rpx;
  border-radius: 100px;
}
.start {
  border: 1rpx solid $main-color;
  color: $main-color;
}
.bargaining,
.buy {
  font-size: 24rpx;
  color: #fff;
  width: 80%;
  margin: 50rpx auto 0 auto;
  text-align: center;
  font-size: 30rpx;
  background-image: linear-gradient(
    25deg,
    $main-color,
    $light-color,
    $aider-light-color
  );

  padding: 18rpx;
  border-radius: 100px;
  animation: mymove 5s infinite;
  -webkit-animation: mymove 5s infinite; /*Safari and Chrome*/
  animation-direction: alternate; /*轮流反向播放动画。*/
  animation-timing-function: ease-in-out; /*动画的速度曲线*/
  /* Safari 和 Chrome */
  -webkit-animation: mymove 5s infinite;
  -webkit-animation-direction: alternate; /*轮流反向播放动画。*/
  -webkit-animation-timing-function: ease-in-out; /*动画的速度曲线*/
}

@keyframes mymove {
  0% {
    transform: scale(1); /*开始为原始大小*/
  }
  25% {
    transform: scale(1.1); /*放大1.1倍*/
  }
  50% {
    transform: scale(1);
  }
  75% {
    transform: scale(1.1);
  }
}
.line {
  margin: 20rpx 0;
}
.tips {
  font-size: 24rpx;
  color: #999;
  justify-content: space-between;
}
.bargain-progress {
  margin: 20rpx 0;
}
.bargain-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $light-color;
  text-align: center;
  margin-bottom: 40rpx;
}
.user-item {
  margin: 40rpx 0;
  align-items: center;
}
.user-config {
  margin-left: 20rpx;
  flex: 8;
  align-items: center;
  justify-content: space-between;
  > .user-name {
    > div:nth-of-type(1) {
      font-size: 28rpx;
      max-width: 300rpx;
      overflow: hidden;
      word-wrap: normal;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    > div:nth-last-of-type(1) {
      font-size: 24rpx;
      color: #999;
    }
  }
}
.save {
  color: $light-color;
  > span {
    font-weight: bold;
  }
}
.mobile-intro {
  overflow: hidden;
  max-width: 100%;
}

@keyframes fontMove {
  0% {
    transform: scale(1); /*开始为原始大小*/
  }
  25% {
    transform: scale(1.1); /*放大1.1倍*/
  }
  50% {
    transform: scale(1);
  }
  75% {
    transform: scale(1.1);
  }
}

.help-bargain {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  > .help {
    margin-bottom: 40rpx;
    border-radius: 20rpx;

    margin-top: 40rpx;
    animation: fontMove 5s infinite;
    -webkit-animation: fontMove 5s infinite; /*Safari and Chrome*/
    animation-direction: alternate; /*轮流反向播放动画。*/
    animation-timing-function: ease-in-out; /*动画的速度曲线*/
    /* Safari 和 Chrome */
    -webkit-animation: fontMove 5s infinite;
    -webkit-animation-direction: alternate; /*轮流反向播放动画。*/
    -webkit-animation-timing-function: ease-in-out; /*动画的速度曲线*/
  }
}
</style>