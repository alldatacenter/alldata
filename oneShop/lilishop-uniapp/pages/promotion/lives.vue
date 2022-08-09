<template>
  <div class="box">
    <u-navbar class="navbar">
      <view class="slot-wrap">
        <u-search placeholder="搜索直播间" @custom="searchLive" @clear="clear" @search="searchLive" v-model="keyword"></u-search>
      </view>
    </u-navbar>
    <!-- 轮播图 -->
    <u-swiper @click="clickSwiper" class="swiper" :effect3d="true" :list="swiperImg">

    </u-swiper>
    <u-tabs :is-scroll="false" @change="changeTabs" :current="current" :active-color="activeColor" inactive-color="#606266" ref="tabs" :list="tabs"></u-tabs>

    <div class="wrapper">
      <!-- 直播中 全部 直播回放 -->
      <div class="live-item" :class="{'invalid':item.status == 'END'}" v-for="(item,index) in liveList" :key="index" @click="handleLivePlayer(item)">
        <div class="live-cover-img">
          <div class="tips">
            <div class="live-box">
              <image class="live-gif" src="./static/live.gif"></image>
            </div>
            <span>{{item.status == 'END' ? '已结束' : item.status =='NEW' ? '未开始' : '直播中'}}</span>
          </div>
          <div class="bg"></div>
          <u-image width="326" height="354" :src="item.shareImg" />
        </div>
        <div class="live-goods">
          <div class="live-goods-name">
            {{item.name}}
          </div>
          <div class="live-store">
            <span class="wes">lilishop</span>
          </div>
          <div class="live-goods-list">
            <div class="live-goods-item">
              <u-image border-radius="20" :src="item.roomGoodsList ? item.roomGoodsList[0] : ''" height="140"></u-image>
            </div>
            <div class="live-goods-item">
              <u-image border-radius="20" :src="item.roomGoodsList ? item.roomGoodsList[1] : ''" height="140"></u-image>
            </div>
          </div>
        </div>
      </div>
      <u-loadmore v-if="liveList.length > 10" bg-color="#f8f8f8" :status="status" />
    </div>
  </div>
</template>

<script>
import { getLiveList } from "@/api/promotions.js";
export default {
  data() {
    return {
      status: "loadmore",
      activeColor: this.$lightColor,
      current: 0, // 当前tabs索引
      keyword: "", //搜索直播间
      // 标签栏
      tabs: [
        {
          name: "直播中",
        },
        {
          name: "全部",
        },
      ],
      // 导航栏的配置
      background: {
        background: "#ff9f28",
      },
      // 直播间params
      params: [
        {
          pageNumber: 1,
          pageSize: 10,
          status: "START",
        },
        {
          pageNumber: 1,
          pageSize: 4,
        },
      ],
      // 推荐直播间Params
      recommendParams: {
        pageNumber: 1,
        pageSize: 3,
        recommend: 0,
      },
      // 直播间列表
      liveList: [],
      // 推荐直播间列表
      recommendLiveList: [],

      //轮播图滚动的图片
      swiperImg: [
        {
          image:
            "https://lilishop-oss.oss-cn-beijing.aliyuncs.com/48d789cb9c864b7b87c1c0f70996c3e8.jpeg",
        },
      ],
    };
  },
  onShow() {
    this.params[this.current].pageNumber = 1;
    this.liveList = [];
    this.getLives();
    this.getRecommendLives();
  },
  onReachBottom() {
    this.params[this.current].pageNumber++;
    this.getLives();
  },
  methods: {
    /**
     * 点击标签栏切换
     */
    changeTabs(index) {
      this.current = index;
      this.init();
    },

    /**
     * 初始化直播间
     */
    init() {
      this.liveList = [];
      this.status = "loadmore";
      this.getLives();
    },

    /**
     * 清除搜索内容
     */
    clear() {
      delete this.params[this.current].name;
      this.init();
    },
    /**
     * 点击顶部推荐直播间
     */
    clickSwiper(val) {
      console.log(this.swiperImg[val]);
      this.handleLivePlayer(this.swiperImg[val]);
    },

    /**
     * 搜索直播间
     */
    searchLive(val) {
      this.params[this.current].pageNumber = 1;
      this.params[this.current].name = val;
      this.init();
    },
    /**
     * 获取推荐直播间
     */
    async getRecommendLives() {
      this.status = "loading";
      let recommendLives = await getLiveList(this.recommendParams);
      if (recommendLives.data.success) {
        // 推荐直播间
        if (recommendLives.data.result.records.length ) {
          this.status = "loadmore";
          this.recommendLives = recommendLives.data.result.records;
        } else {
          this.status = "noMore";
        }

        /**
         * 如果推荐直播间没有的情况下
         * 1.获取直播间第一个图片
         * 2.如果没有直播间设置一个默认图片
         */

        if (!this.recommendLives.length) {
          if (this.liveList[0].shareImg) {
            this.$set(this, "swiperImg", [
              {
                image: this.liveList[0].shareImg,
                roomId: this.liveList[0].roomId,
              },
            ]);
          }
        } else {
          this.recommendLives.forEach((item) => {
            this.$set(this, "swiperImg", [
              { image: item.shareImg, roomId: item.roomId },
            ]);
          });
        }
      }
    },

    /**
     * 获取直播间
     */
    async getLives() {
      this.status = "loading";
      let res = await getLiveList(this.params[this.current]);
      // 直播间
      if (res.data.success) {
        if (res.data.result.records.length ) {
          this.status = "loadmore";
          this.liveList.push(...res.data.result.records);
        } else {
          this.status = "noMore";
        }
        res.data.result.total >
        this.params[this.current].pageNumber *
          this.params[this.current].pageSize
          ? (this.status = "loadmore")
          : (this.status = "noMore");

      
        this.liveList.forEach((item) => {
          if (item.roomGoodsList) {
            item.roomGoodsList = JSON.parse(item.roomGoodsList);
          }
        });
      }
    },

    /**
     * 进入直播间
     */
    handleLivePlayer(val) {
      // #ifdef MP-WEIXIN
      let roomId = val.roomId; // 填写具体的房间号，可通过下面【获取直播房间列表】 API 获取
      let customParams = encodeURIComponent(
        JSON.stringify({ path: "pages/index/index", pid: 1 })
      ); // 开发者在直播间页面路径上携带自定义参数，后续可以在分享卡片链接和跳转至商详页时获取，详见【获取自定义参数】、【直播间到商详页面携带参数】章节（上限600个字符，超过部分会被截断）
      uni.navigateTo({
        url:
          "plugin-private://wx2b03c6e691cd7370/pages/live-player-plugin?room_id=" +
          roomId +
          "&custom_params=" +
          customParams,
      });
      // #endif

      // #ifndef MP-WEIXIN
      uni.showToast({
        title: "请从微信小程序中预览直播功能",
        duration: 2000,
        icon: "none",
      });
      // #endif
    },
  },
};
</script>

<style lang="scss" scoped>
.slot-wrap {
  display: flex;
  align-items: center;
  /* 如果您想让slot内容占满整个导航栏的宽度 */
  flex: 1;
  /* 如果您想让slot内容与导航栏左右有空隙 */
  /* padding: 0 30rpx; */
}
.invalid {
  filter: grayscale(1);
}
.wrapper {
  padding: 0 24rpx;
}
.live-item {
  display: flex;
  overflow: hidden;
  border-radius: 20rpx;
  flex-wrap: wrap;
  background: #fff;

  margin: 20rpx 0;
}
.live-cover-img {
  position: relative;
}
.swiper {
  margin: 20rpx 0;
}

.live-goods {
  position: relative;
  flex: 1;
  padding: 16rpx 24rpx 24rpx;
}
.live-goods-name {
  height: 84rpx;
  font-weight: bold;
  font-size: 30rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.live-store {
  display: flex;
  align-items: center;
  margin: 20rpx 0;
  overflow: hidden;
  width: calc(100% - 50rpx);
}
.live-gif {
  width: 20rpx;
  height: 20rpx;
}
.live-box {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  width: 40rpx;
  margin-right: 10rpx;
  height: 40rpx;
  background: linear-gradient(90deg, #ff6b35, #ff9f28, #ffcc03);
}
.live-goods-list {
  display: flex;
  align-items: center;
  justify-content: space-between;
  > .live-goods-item {
    flex: 1;
  }
  > .live-goods-item:nth-of-type(1) {
    padding-right: 38rpx;
  }
}

.live-icon,
.zan {
  position: absolute;
  width: 80rpx;
  height: 80rpx;
  z-index: 9;
}
.tips {
  display: flex;
  position: absolute;
  z-index: 9;

  align-items: center;
  top: 20rpx;
  right: 0;
  padding: 4rpx 12rpx 4rpx 0;
  font-size: 24rpx;
  border-radius: 100px;
  color: #fff;
  background: rgba(0, 0, 0, 0.46);
}
.live-icon {
  right: 0;
  top: 104rpx;
}

.zan {
  bottom: 0;
  right: 0;
  width: 100rpx;
  height: 100rpx;
}
.bg {
  position: absolute;
  bottom: 4rpx;
  width: 100%;
  height: 100rpx;
  z-index: 8;
  background-image: -webkit-gradient(
    linear,
    left bottom,
    left top,
    from(rgba(0, 0, 0, 0.25)),
    color-stop(82%, transparent)
  );
  background-image: linear-gradient(0deg, rgba(0, 0, 0, 0.25), transparent 82%);
  border-bottom-left-radius: 20rpx;
}
</style>