<template>
  <view class="index">
    <user-point />
    <view class="index-head">
      <scroll-view scroll-x class="list-scroll-content" :scroll-left="currentLeft" scroll-with-animation>
        <view class="index-head-navs">
          <view class="index-head-nav" v-for="(ele, index) in categoryIndexData" :key="index"
            :class="{ 'index-head-nav-active': tabIndex == index }" @click="setCat(index)">
            {{ ele.name }}
          </view>
        </view>
      </scroll-view>
    </view>
    <swiper :current="tabIndex" class="swiper-box" @change="ontabchange" duration="300">

      <swiper-item v-for="(nav, index) in categoryIndexData" :key="index" class="swiper-item">

        <scroll-view class="scroll-v" enableBackToTop="true" scroll-with-animation scroll-y @scrolltolower="loadMore">
          <view class="index-items">
            <view class="index-item" v-for="(item, key) in nav.goods" :key="key" @click="toGoods(item)">
              <view class="index-item-img">
                <u-image :src="item.thumbnail" mode="aspectFill">
                  <u-loading slot="loading"></u-loading>
                </u-image>
                <view class="index-item-title">{{ item.goodsName }}</view>
                <view class="index-item-price">
                  {{ item.points }}积分
                  <span class="tipsMkt">¥{{ item.originalPrice | unitPrice }}</span>
                </view>
              </view>
            </view>
          </view>
          <uni-load-more :status="nav.loadStatus"></uni-load-more>
        </scroll-view>
      </swiper-item>
    </swiper>
  </view>
</template>

<script>
import { getPointsCategory, getPointsGoods } from "@/api/promotions.js";
import userPoint from "./user";
export default {
  components: {
    "user-point": userPoint,
  },
  data() {
    return {
      headOffSetTop: "0",
      tabIndex: 0,
      categoryIndexData: [
        {
          categoryId: 0,
          name: "全部",
          loadStatus: "more",
          goods: [],
          params: {
            pageNumber: 1,
            pageSize: 10,
            pointsGoodsCategoryId: "",
          },
        },
      ],
      currentLeft: 0,
      pageParams: {
        pageNumber: 1,
        pageSize: 10,
        pointsGoodsCategoryId: 0,
      },
      flag: true,
    };
  },
  onLoad() {},
  async onPullDownRefresh() {
    this.categoryIndexData[this.tabIndex].goods = [];
    this.categoryIndexData[this.tabIndex].params.pageNumber = 1;
    this.categoryIndexData[this.tabIndex].loadStatus = "more";
    this.loadGoods();
  },
  onPageScroll(e) {
    if (e.scrollTop < -40 && this.flag) {
      this.flag = false;
      this.categoryIndexData[this.tabIndex].goods = [];
      this.categoryIndexData[this.tabIndex].params.pageNumber = 1;
      this.categoryIndexData[this.tabIndex].loadStatus = "more";
      uni.startPullDownRefresh();
      this.loadGoods();
    }
  },
  watch: {
    tabIndex(val) {
      if (
        this.categoryIndexData[this.tabIndex].goods.length == 0 &&
        this.categoryIndexData[this.tabIndex].params.pageNumber == 1
      ) {
        this.loadGoods();
      }
    },
  },
  async onShow() {
    //获取顶级分类
    this.categoryIndexData = [
      {
        categoryId: 0,
        name: "全部",
        loadStatus: "more",
        goods: [],
        params: {
          pageNumber: 1,
          pageSize: 10,
          pointsGoodsCategoryId: "",
        },
      },
    ];

    let response = await getPointsCategory();

    if (response.data.success) {
      let navData = response.data.result.records;
      navData.forEach((item) => {
        this.categoryIndexData.push({
          categoryId: item.id,
          goods: [],
          loadStatus: "more",
          name: item.name,
          params: {
            pageNumber: 1,
            pageSize: 10,
            pointsGoodsCategoryId: item.id,
          },
        });
      });
    }

    this.loadGoods();
  },
  methods: {
    // 跳转
    navigateTo(url) {
      uni.navigateTo({
        url,
      });
    },

    toGoods(item) {
      //跳转详情
      uni.navigateTo({
        url: `/pages/promotion/point/detail?id=${item.id}`,
      });
    },

    async loadGoods() {
      let index = this.tabIndex;

      //获取商品数据
      let response = await getPointsGoods(this.categoryIndexData[index].params);
      if (response.data.success) {
        this.categoryIndexData[index].goods.push(
          ...response.data.result.records
        );
        if (response.data.result.records.length < 10) {
          this.categoryIndexData[index].loadStatus = "noMore";
        }
        let _this = this;
        setTimeout(function () {
          _this.flag = true;
        }, 3000);
      }

      uni.stopPullDownRefresh();
    },
    setCat(type) {
      this.tabIndex = type;
    },
    ontabchange(e) {
      this.tabIndex = e.detail.current;
      if (e.detail.current > 3) {
        this.currentLeft = (e.detail.current - 3) * 70;
      } else {
        this.currentLeft = 0;
      }
    },
    loadMore() {
      if (this.categoryIndexData[this.tabIndex].loadStatus == "more") {
        this.categoryIndexData[this.tabIndex].params.pageNumber++;
        this.loadGoods();
      }
    },
  },
};
</script>
<style lang="scss" scoped>
page {
  height: 100%;
}
.tipsMkt {
  float: right;
  color: #c0c4cc;
  font-size: 24rpx;
  text-decoration: line-through;
  margin-right: 20rpx;
}

.header {
  background: $light-color;
  position: relative;
  color: #fff;
  display: flex;
  height: 80rpx;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-size: 34rpx;

  .left,
  .right {
    position: absolute;
    width: max-content;
    height: max-content;
    top: 0;
    bottom: 0;
    margin: auto;
  }

  .left {
    float: left;
    top: 0;
    bottom: 0;
    left: 20rpx;
  }

  .right {
    float: right;
    right: 20rpx;
  }
}

.index {
  height: 100vh;
  // #ifdef H5
  height: calc(100vh - 44px);
  // #endif
  width: 100%;
  overflow: hidden;
}

.index-head {
  background: #fff;
}

.list-scroll-content {
  white-space: nowrap;
  width: 100%;
  height: 100rpx;
  color: #333;
}

.index-head-navs {
  width: 100%;
  height: 92rpx;
  display: -webkit-box;
  display: -webkit-flex;
  display: flex;
  align-items: center;
}

.index-head-nav {
  padding-bottom: 8rpx;
  margin: 20rpx;
  text-align: center;
  box-sizing: border-box;
  white-space: nowrap;
  font-size: 30rpx;
  color: #333;

  display: -webkit-box;
  display: -webkit-flex;
  display: flex;
  justify-content: center;
  align-items: center;

  &-active {
    border-bottom: 4rpx solid $light-color;
  }
}

.swiper-box {
  // #ifdef H5
  height: calc(100vh - (100rpx + 300rpx + 44px));
  // #endif

  // #ifndef H5
  height: calc(100vh - 400rpx);
  // #endif

  .scroll-v {
    height: 100%;
  }
}

.index-items {
  padding-top: 10rpx;
  margin-top: 20rpx;

  padding-left: 20rpx;
  background-color: #f7f7f7;
  display: -webkit-box;
  display: -webkit-flex;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.index-item {
  width: 346rpx;
  // height: 2100rpx;
  background-color: #fff;
  margin: 0 18rpx 20rpx 0;
  border-radius: 16rpx;
  box-sizing: border-box;
  overflow: hidden;
  height: auto;
  padding-bottom: 20rpx;
}

.index-item-img {
  /deep/ .u-image {
    width: 346rpx !important;
    height: 320rpx !important;
    border-radius: 10rpx !important;
  }
}

.index-item-title {
  font-size: 26rpx;
  color: #333333;
  padding: 0 20rpx;
  height: 80rpx;
  box-sizing: border-box;
  max-height: 3em;
  overflow: hidden;
}

.index-item-title-desc {
  font-size: 25rpx;
  color: #999999;
  margin-top: 10rpx;
}

.index-item-price {
  font-size: 28rpx;
  color: #ff5a10;
  padding: 20rpx 0 0 20rpx;
}
</style>
