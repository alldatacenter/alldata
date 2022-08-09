<template>
  <view class="wrapper">
    <!-- 筛选弹出层 TODO后续版本更新 -->
    <!-- <u-popup width="90%" v-model="popup" mode="right">
      <view class="screen-title">商品筛选</view>

      <view class="screen-view">
        <view class="screen-item">
          <h4>价格区间</h4>
          <view class="flex">
            <u-input class="u-bg" placeholder-style="font-size:22rpx;" type="number" input-align="center" placeholder="最低价"></u-input>
            <view class="line"></view>
            <u-input class="u-bg" placeholder-style="font-size:22rpx;" type="number" input-align="center" placeholder="最高价"></u-input>
          </view>
        </view>
        <view class="screen-item">
          <h4>销量</h4>
          <view class="flex">
            <u-input class="u-bg w200 flex1" placeholder-style="font-size:22rpx;" type="number" input-align="center" placeholder="销量"></u-input>
            <view class="flex1">笔以上</view>
          </view>
        </view>
        <view class="screen-item">
          <h4>收入比率</h4>
          <view class="flex">
            <u-input class="u-bg" placeholder-style="font-size:22rpx;" type="number" input-align="center" placeholder="最低%"></u-input>
            <view class="line"></view>
            <u-input class="u-bg" placeholder-style="font-size:22rpx;" type="number" input-align="center" placeholder="最高%"></u-input>
          </view>
        </view>
        <view class="screen-item">
          <h4>包邮</h4>
          <view class="flex">
            <u-tag class="u-tag" shape="circle" text="包邮" mode="plain" type="info" />
          </view>
        </view>
        <view class="screen-item">
          <h4>促销活动</h4>
          <view class="flex">
            <u-tag class="u-tag" shape="circle" text="限时抢购" mode="plain" type="info" />
            <u-tag class="u-tag" shape="circle" text="拼团秒杀" mode="plain" type="info" />
          </view>
        </view>
        <view class="screen-item">
          <h4>经营类型</h4>
          <view class="flex">
            <u-tag class="u-tag" shape="circle" text="平台自营" mode="plain" type="info" />
            <u-tag class="u-tag" shape="circle" text="三方店铺" mode="plain" type="info" />
          </view>
        </view>
      </view>

      <view class="screen-btn">
        <view class="screen-clear"> 重置 </view>
        <view class="screen-submit"> 确定 </view>
      </view>
    </u-popup> -->

    <!-- 导航栏 -->
    <view class="nav">
      <view class="nav-item" @click="handleMyGoods(true)" :class="{ checked: params.checked }">已选择</view>
      <view class="nav-item" @click="handleMyGoods(false)" :class="{ checked: !params.checked }">未选择</view>

      <!-- <view class="nav-item" @click="popup = !popup">筛选</view> -->
    </view>
    <!-- 商品列表 -->

    <view class="goods-list">
      <u-swipe-action v-for="(item, index) in goodsList" :disabled="!params.checked" :show="item.___selected" @open="openAction(item)" :index="index" :options="options" bg-color="#fff"
        ref="swiperAction" :key="item.id" @click="changeActionTab(item)">

        <div class="goods-item">
          <view class="goods-item-img" @click="handleNavgationGoods(item)">
            <u-image width="176rpx" height="176rpx" :src="item.thumbnail"></u-image>
          </view>
          <view class="goods-item-desc">
            <!-- 商品描述 -->
            <view class="-item-title" @click="handleNavgationGoods(item)">
              {{ item.goodsName }}
            </view>
            <!-- 商品金额 -->
            <view class="-item-price" @click="handleNavgationGoods(item)">
              佣金:
              <span> ￥{{ item.commission | unitPrice }}</span>
            </view>
            <!-- 比率佣金 -->
            <view class="-item-bottom">
              <view class="-item-bootom-money" @click="handleNavgationGoods(item)">
                <!-- <view class="-item-bl">
                比率:
                <span>{{ "5.00%" }}</span>
              </view> -->
                <view class="-item-yj">
                  <span>￥{{ item.price | unitPrice }}</span>
                </view>
              </view>
              <view>
                <view class="click" v-if="!params.checked" @click="handleClickGoods(item)">立即选取</view>
                <view class="click" v-if="params.checked" @click="handleLink(item)">分销商品</view>
              </view>
            </view>
          </view>
        </div>
      </u-swipe-action>

      <view class="empty">
        <!-- <u-empty v-if="empty" text="没有分销商品了" mode="list"></u-empty> -->
      </view>
    </view>
    <canvas class="canvas-hide" canvas-id="qrcode" />
    <drawCanvas ref="drawCanvas" v-if="showFlag" :res="res" />
    <u-modal v-model="deleteShow" :confirm-style="{'color':lightColor}" @confirm="delectConfirm" show-cancel-button :content="deleteContent" :async-close="true"></u-modal>

  </view>
</template>
<script>
import {
  distributionGoods,
  checkedDistributionGoods,
  getMpCode,
} from "@/api/goods";

import drawCanvas from "@/components/m-canvas";
export default {
  data() {
    return {
      lightColor: this.$lightColor,
      deleteContent: "解绑该商品？", //删除显示的信息
      // 商品栏右侧滑动按钮
      options: [
        {
          text: "解绑",
          style: {
            backgroundColor: this.$lightColor, //高亮颜色
          },
        },
      ],
      showFlag: false, //分销分享开关
      empty: false,
      popup: false, //弹出层开关
      active_color: this.$mainColor,
      current: 0,
      params: {
        pageNumber: 1,
        pageSize: 10,
        checked: true,
      },
      goodsList: [],

      // 分销分享 实例
      res: {
        container: {
          width: 600,
          height: 960,
          background: "#fff",
          title: "分享背景",
        },
        // 分销分享
        bottom: {
          img: "",
          code: "",
          price: 0,
        },
      },

      routers: "",
      deleteShow: false, //删除模态框
      goodsVal: false, //分销商铺信息
    };
  },
  components: {
    drawCanvas,
  },
  onLoad(options) {
    this.routers = options;
  },
  watch: {},
  onShow() {
    this.goodsList = [];
    this.init();
  },
  methods: {
    /**
     * 滑动删除
     */
    changeActionTab(val) {
      this.deleteShow = true;
      this.goodsVal = val;
    },

    /**
     * 点击解绑商品
     */
    delectConfirm() {
      checkedDistributionGoods({ id: this.goodsVal.id, checked: false }).then(
        (res) => {
          if (res.data.success) {
            uni.showToast({
              title: "此商品解绑成功",
              duration: 2000,
            });
            this.deleteShow = false;
            this.goodsList = [];
            this.init();
          }
        }
      );
    },

    /**
     * 左滑打开删除
     */
    openAction(val) {
      this.goodsList.forEach((item) => {
        this.$set(item, "___selected", false);
      });
      this.$set(val, "___selected", true);
    },

    /**
     * 查看图片
     */
    handleNavgationGoods(val) {
      uni.navigateTo({
        url: `/pages/product/goods?id=${val.skuId}&goodsId=${val.goodsId}`,
      });
    },

    async handleLink(goods) {
      uni.showToast({
        title: "请请按住保存图片",
        duration: 2000,
        icon: "none",
      });
      let page = `pages/product/goods`;
      let scene = `${goods.skuId},${goods.goodsId},${this.routers.id}`;
      let result = await getMpCode({ page, scene });
      if (result.data.success) {
        let callback = result.data.result;
        this.res.container.title = `${goods.goodsName}`;
        this.res.bottom.code = `data:image/png;base64,${callback}`;
        this.res.bottom.price = this.$options.filters.unitPrice(
          goods.price,
          "￥"
        );
        this.res.bottom.desc = `${goods.goodsName}`;
        this.res.bottom.img = `${goods.thumbnail}`;

        if (this.showFlag) {
          this.$refs.drawCanvas.init();
        }
        this.showFlag = true;
      } else {
        uni.showToast({
          title: `制作二维码失败！请稍后重试`,
          duration: 2000,
          icon: "none",
        });
      }
    },

    change(index) {
      this.current = index;
    },
    // 点击我的选品库
    handleMyGoods(flag) {
      this.goodsList = [];
      this.params.checked = flag;
      this.init();
    },

    // 选择商品
    handleClickGoods(val) {
      checkedDistributionGoods({ id: val.id, checked: true }).then((res) => {
        if (res.data.success) {
          uni.showToast({
            title: "已添加到我的选品库",
            duration: 2000,
            icon: "none",
          });

          setTimeout(() => {
            this.goodsList = [];
            this.init();
          }, 500);
        }
      });
    },

    init() {
      distributionGoods(this.params).then((res) => {
        if (res.data.success && res.data.result.records.length >= 1) {
          res.data.result.records.forEach((item) => {
            this.$set(item, "___selected", false);
          });
          this.goodsList.push(...res.data.result.records);
        }
        if (this.goodsList.length == 0) {
          this.empty = true;
        }
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.canvas-hide {
  /* 1 */
  position: fixed;
  right: 100vw;
  bottom: 100vh;
  /* 2 */
  z-index: -9999;
  /* 3 */
  opacity: 0;
}
.empty {
  margin: 40rpx 0;
}
.checked {
  color: $main-color;
  font-weight: bold;
}
.screen-btn {
  display: flex;
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  position: fixed;
  bottom: 0;
  > .screen-clear,
  .screen-submit {
    width: 50%;
    text-align: center;
  }
  .screen-submit {
    background: $main-color;
    color: #fff;
  }
}
.screen-item {
  margin-bottom: 40rpx;
}
.flex1 {
  padding-left: 10rpx;
}
.u-tag {
  margin-right: 20rpx;
}
.line {
  width: 40rpx;
  height: 2rpx;
  background: #999;
  margin: 0 10rpx;
}
.u-bg {
  background: #eff1f4;
  border-radius: 0.4em;
  font-size: 22rpx;
}
.screen-title {
  height: 88rpx;
  text-align: center;
  font-size: 28upz;
  line-height: 88rpx;
  border-bottom: 1px solid #ededed;
}
.flex {
  display: flex;
  margin: 20rpx 0;
  align-items: center;
}
.screen-view {
  padding: 32rpx;
}
.bar {
  padding: 0 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 88rpx;
  width: 100%;
  background: #fff;
  z-index: 8;
  > .bar-btn {
    display: flex;
  }
}
.nav {
  background: #fff;
  width: 100%;
  display: flex;
  height: 88rpx;
  box-sizing: border-box;
  border-top: 1px solid #ededed;
  border-bottom: 1px solid #ededed;
  > .nav-item {
    line-height: 88rpx;
    height: 88rpx;
    flex: 1;
    text-align: center;
  }
}
.click {
  background: $main-color;
  color: #fff;
  margin: 0 4rpx;
  font-size: 22rpx;
  padding: 10rpx 20rpx;
  border-radius: 100px;
}
.goods-list {
  // #ifdef H5
  height: calc(100vh - 176rpx);
  // #endif
  // #ifndef H5
  height: calc(100vh - 88rpx);
  // #endif
  overflow: auto;
}
.goods-item {
  border-radius: 20rpx;
  background: #fff;
  display: flex;
  padding: 22rpx;
  margin: 20rpx;
  justify-content: space-between;
  > .goods-item-desc {
    flex: 2;
    padding: 0 16rpx;
    line-height: 1.7;
    > .-item-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;

      padding-bottom: 20rpx;
      > .-item-bootom-money {
        > .-item-bl,
        .-item-yj {
          margin-right: 10rpx;
          font-size: 24rpx;
          color: $font-color-base;
        }
      }
    }
    > .-item-title {
      display: -webkit-box;

      -webkit-box-orient: vertical;

      -webkit-line-clamp: 1;

      overflow: hidden;
    }
    > .-item-price {
      color: $price-color;
      > span {
        font-size: 36rpx;
      }
    }
  }
}
.wrapper {
  width: 100%;
}
</style>