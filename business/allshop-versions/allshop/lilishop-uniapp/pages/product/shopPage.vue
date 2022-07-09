<template>
  <div>
    <u-navbar :border-bottom="false">
      <u-search v-model="keyword" @search="search" @click="search" placeholder="请输入搜索"></u-search>
    </u-navbar>
    <div class="wrapper">
      <!-- 店铺信息模块 -->
      <div class="store flex">
        <u-image border-radius="10" width="150" height="150" :src="storeInfo.storeLogo || config.logo" mode="aspectFit">
        </u-image>
        <div class="box">
          <div class="store-name" @click="getStoreLicencePhoto">
            {{ storeInfo.storeName || ''}}
            <u-icon style="margin-left:10rpx;" name="arrow-right"></u-icon>
          </div>
          <div class="flex store-message">
            <div> <span>{{ storeInfo.collectionNum || 0 }}</span>关注 </div>
            <div> <span>{{ storeInfo.goodsNum || 0 }}</span>件商品 </div>
          </div>
        </div>
        <div class="collection">
          <div class="collection-btn" @click="whetherCollection"> {{ isCollection  ? '已关注' : '+ 关注' }}</div>
        </div>
      </div>
      <!-- 店铺简介 -->
      <div class="store-desc wes-2">
        {{storeInfo.storeDesc}}
      </div>

      <!-- 联系客服 -->
      <div class="kefu" @click="linkKefuDetail">
        <u-icon name="kefu-ermai"></u-icon>
        联系客服
      </div>
    </div>
    <!-- 优惠券 -->
    <scroll-view scroll-x="true" show-scrollbar="false" class="discount" v-if="couponList.length > 0">
      <view class="card-box" v-for="(item, index) in couponList" :key="index">
        <view class="card" @click="getCoupon(item)">
          <view class="money">
            <view>
              <span v-if="item.couponType == 'DISCOUNT'">{{ item.couponDiscount }}折</span>
              <span v-else>{{ item.price }}元</span>
            </view>

          </view>
          <view class="xian"></view>
          <view class="text">
            <text>{{'领取优惠券'}}</text>
            <text>满{{ item.consumeThreshold | unitPrice }}元可用</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- tab -->
    <u-tabs :list="tabs" :active-color="mainColor" :is-scroll="false" :current="current" @change="changeTab"></u-tabs>
    <!-- menu -->

    <!-- 商品 -->
    <div class="contant" v-if="current == 0">
      <view v-if="!goodsList.length" class="empty">暂无商品信息</view>
			<goodsTemplate :res="goodsList" :storeName="false" />
    </div>
    <!-- 全部分类 -->
    <div class="category" v-if="current == 1">
      <div class="category-item" v-for="(item,index) in categoryList" :key="index">
        <div class="flex" @click="getCategoryGoodsList(item)">
          <div>{{item.labelName}}</div>
          <div>
            <u-icon color="#999" name="arrow-right"></u-icon>
          </div>
        </div>
        <!-- 分类子级 -->
        <div class="child-list" v-if="item.children && item.children.length!=0">
          <div class="child" @click="getCategoryGoodsList(child)" v-for="(child,i) in item.children">{{child.labelName}}
          </div>
        </div>
      </div>
    </div>
    <u-back-top :scroll-top="scrollTop"></u-back-top>
  </div>
</template>

<script>
import { getStoreBaseInfo, getStoreCategory } from "@/api/store.js";
import goodsTemplate from '@/components/m-goods-list/list'
import {
  receiveCoupons,
  deleteStoreCollection,
  collectionGoods,
  getGoodsIsCollect,
} from "@/api/members.js";
import config from "@/config/config";
import storage from "@/utils/storage";
import { getGoodsList } from "@/api/goods.js";
import { getAllCoupons } from "@/api/promotions.js";
export default {
  data() {
    return {
      config,
      scrollTop: 0,
      mainColor: this.$mainColor, //主色调
      current: 0, //初始tabs的索引
      tabs: [{ name: "全部商品" }, { name: "分类查看" }], // 标签
      storeId: "",
      keyword: "",
      storeInfo: {}, //店铺详情
      isCollection: false, //是否关注
      goodsList: [], //推荐货物
      couponList: [], //优惠券列表
      categoryList: [],
      couponParams: { pageNumber: 1, pageSize: 50, storeId: "" },
      goodsParams: { pageNumber: 1, pageSize: 50, storeId: "" },
    };
  },
  watch: {
    current(val) {
      val == 0 ? ()=>{ this.goodsList = []; this.getGoodsData()} : this.getCategoryData();
    },
  },
	components:{goodsTemplate},

  /**
   * 加载
   */
  async onLoad(options) {
    this.storeId = options.id;
    this.goodsParams.storeId = options.id;
    this.couponParams.storeId = options.id;
  },
  onPageScroll(e) {
    this.scrollTop = e.scrollTop;
  },
  onPullDownRefresh() {
    this.init();
  },
  mounted() {
    // #ifdef MP-WEIXIN
    // 小程序默认分享
    uni.showShareMenu({ withShareTicket: true });
    // #endif
    this.init();
  },

  // 下拉加载
  onReachBottom() {
    this.goodsParams.pageNumber++;
    this.getGoodsData();
  },

  methods: {
    getStoreLicencePhoto() {
      uni.navigateTo({
        url: `/pages/product/licencePhoto?id=${this.storeId}`,
      });
    },
    /**
     * 初始化信息
     */
    init() {
      this.goodsList = [];
      this.categoryList = [];
      this.couponList = [];
      this.goodsParams.pageNumber = 1;
      if (this.$options.filters.isLogin("auth")) {
        this.enableGoodsIsCollect();
      }
      // 店铺信息
      this.getStoreData();
      // 商品信息
      this.getGoodsData();
      // 优惠券信息
      this.getCouponsData();
      // 店铺分类
      this.getCategoryData();
    },
    /**
     * 联系客服
     */
    linkKefuDetail() {
      // // 客服
      // // #ifdef MP-WEIXIN

      // const params = {
      //   // originalPrice: this.goodsDetail.original || this.goodsDetail.price,
      //   uuid: storage.getUuid(),
      //   token: storage.getAccessToken(),
      //   sign: this.storeInfo.yzfSign,
      //   mpSign: this.storeInfo.yzfMpSign,
      // };
      // uni.navigateTo({
      //   url:
      //     "/pages/product/customerservice/index?params=" +
      //     encodeURIComponent(JSON.stringify(params)),
      // });
      // // #endif
      // // #ifndef MP-WEIXIN
      // const sign = this.storeInfo.yzfSign;
      // uni.navigateTo({
      //   url:
      //     "/pages/tabbar/home/web-view?src=https://yzf.qq.com/xv/web/static/chat/index.html?sign=" +
      //     sign,
      // });
      // // #endif
			
			uni.navigateTo({
			   url: `/pages/tabbar/home/web-view?IM=${this.storeId}`,
			 });
    },

    /** 获取店铺分类 */
    async getCategoryData() {
      let res = await getStoreCategory(this.storeId);
      if (res.data.success) {
        this.categoryList = res.data.result;
      }
    },
    /**是否收藏店铺 */
    async enableGoodsIsCollect() {
      let res = await getGoodsIsCollect("STORE", this.storeId);
      if (res.data.success) {
        this.isCollection = res.data.result;
      }
    },

    /**商品分类中商品集合 */
    getCategoryGoodsList(val) {
      uni.navigateTo({
        url: `/pages/product/shopPageGoods?title=${val.labelName}&id=${val.id}&storeId=${this.storeId}`,
      });
    },

    /**
     * 搜索
     */
    search() {
      uni.navigateTo({
        url: `/pages/navigation/search/searchPage?storeId=${this.storeId}&keyword=${this.keyword}`,
      });
    },

    /** 点击tab */
    changeTab(index) {
      this.current = index;
    },

    /**
     * 店铺信息
     */
    async getStoreData() {
      let res = await getStoreBaseInfo(this.storeId);
      res.data.success
        ? (this.storeInfo = res.data.result)
        : uni.reLaunch({ url: "/" });
    },

    /** 加载商品 */
    async getGoodsData() {
      let res = await getGoodsList(this.goodsParams);
      if (res.data.success) {
        this.goodsList.push(...res.data.result.content);
      }
    },

    /** 加载优惠券 */
    async getCouponsData() {
      this.couponParams.storeId = this.storeId;
      let res = await getAllCoupons(this.couponParams);
      if (res.data.success) {
        this.couponList.push(...res.data.result.records);
      }
    },

    /**
     *  是否收藏
     */
    whetherCollection() {
      if (this.isCollection) {
        deleteStoreCollection(this.storeId).then((res) => {
          if (res.data.success) {
            this.isCollection = false;
            uni.showToast({
              icon: "none",
              duration: 3000,
              title: "取消关注成功！",
            });
          }
        });
      } else {
        collectionGoods("STORE", this.storeId).then((res) => {
          if (res.data.success) {
            this.isCollection = true;
            uni.showToast({
              icon: "none",
              duration: 3000,
              title: "关注成功！",
            });
          }
        });
      }
    },

    /**
     * 领取
     */
    getCoupon(item) {
      if (!this.$options.filters.isLogin("auth")) {
        uni.showToast({
          icon: "none",
          duration: 3000,
          title: "请先登录！",
        });

        this.$options.filters.navigateToLogin("redirectTo");
        return false;
      }
      receiveCoupons(item.id).then((res) => {
        if (res.data.success) {
          uni.showToast({
            icon: "none",
            duration: 3000,
            title: "领取成功！",
          });
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.wrapper {
  background: #fff;
  padding: 32rpx;
}
.store {
  align-items: center;
  > .box {
    display: flex;
    flex-direction: column;
    justify-content: center;
    margin-left: 30rpx;
    font-size: 24rpx;
    color: #999;
    flex: 2;
    > .store-name {
      font-size: 34rpx;
      color: #333;
      letter-spacing: 1rpx;
      font-weight: bold;
    }
    > .store-message {
      margin-top: 25rpx;
      > div {
        font-size: 26rpx;
        margin: 0 5rpx;
        > span {
          font-size: 26rpx;
          font-weight: bold;
          color: #333;
          margin-right: 8rpx;
        }
      }
    }
  }
}
.collection-btn {
  background: $main-color;
  padding: 6rpx 0;
  width: 140rpx;
  font-size: 24rpx;
  color: #fff;
  border-radius: 100px;
  text-align: center;
}
.store-desc {
  margin: 40rpx 0 0 0;
  color: #999;
}

.contant {
  margin-top: 20rpx;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  > .empty {
    width: 100%;
    display: flex;
    justify-content: center;
    margin-top: 40rpx;
  }
}
.discount {
  height: 154rpx;
  border-top: 1px solid #f6f6f6;
  border-bottom: 18rpx solid #f6f6f6;
  background: #f6f6f6;
  overflow: hidden;
  white-space: nowrap;
  .card-box {
    display: inline-block;
    padding-top: 25rpx;
  }
  .card {
    width: 324rpx;
    height: 116rpx;
    background: #fff;
    margin-left: 20rpx;
    border-radius: 5px;
    display: flex;
    align-items: center;
    .money {
      width: 45%;
      color: #fd6466;
      font-weight: 500;
      text-align: center;
      text {
        font-size: 50rpx;
      }
    }
    .xian {
      height: 66rpx;
      border: 1px dashed #f6f6f6;
      position: relative;
      &:before,
      &:after {
        content: "";
        width: 22rpx;
        height: 12rpx;
        position: absolute;
        background: #f6f6f6;
      }
      &:before {
        border-radius: 0 0 22rpx 22rpx;
        top: -30rpx;
        left: -10rpx;
      }
      &:after {
        border-radius: 22rpx 22rpx 0 0;
        bottom: -30rpx;
        left: -10rpx;
      }
    }
    .text {
      flex: 1;
      color: $aider-light-color;
      font-size: 24rpx;
      display: flex;
      flex-direction: column;
      margin-left: 14rpx;
      text:nth-of-type(2) {
        color: #ccc;
      }
      .cur:nth-of-type(1) {
        color: #ccc;
      }
    }
  }
}
.category-item {
  background: #fff;
  padding: 22rpx;
  margin: 20rpx 10rpx;
  > .flex {
    color: #666;
    justify-content: space-between;
  }
  > .child-list {
    display: flex;
    margin: 20rpx 0;
    flex-wrap: wrap;
    > .child {
      justify-content: center;
      margin: 1% 0;
      display: flex;
      width: 48%;
      font-size: 24rpx;
      color: #999;
      margin-right: 1%;
      border: 1rpx solid #ededed;
      box-sizing: border-box;
      height: 70rpx;
      text-align: center;
      line-height: 70rpx;
    }
  }
}
.kefu {
  background: #f7f7f7;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 32rpx;
  border-radius: 10rpx;
  font-size: 24rpx;
  color: #999;
}
</style>