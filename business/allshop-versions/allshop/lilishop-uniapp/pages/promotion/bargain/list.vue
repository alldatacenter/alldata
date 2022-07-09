<template>
  <div class="page">
    <u-navbar :custom-back="back" back-icon-color="#fff" :background="background" :border-bottom="false" >
    </u-navbar>
    <div class="wrapper">
      <!-- 砍价列表 -->
      <div class="box">
        <!-- 已砍的商品 -->
        <div class="bargain" v-if="bargainList.length!=0">
          <div class="flex bargain-item" v-for="(item,index) in bargainList" :key="index">
            <div class="goods-img">
              <u-image width="150" height="150" :src="item.thumbnail"></u-image>
            </div>
            <div class="goods-config">
              <div class="goods-title wes-2">
                {{item.goodsName}}
              </div>
              <div class="flex goods-buy">
                <div class="max-price">最低：<span>￥{{item.purchasePrice | unitPrice}}</span></div>
                <div class="bargaining" @click="navigateToBargainDetail(item)">参与砍价</div>
              </div>
            </div>
          </div>
        </div>
        <div class="bargain empty" v-else>
          <u-empty text="暂无活动" mode="list"></u-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getBargainList } from "@/api/promotions";
export default {
  data() {
    return {
      background: {
        backgroundColor: "transparent",
      },
      params: {
        promotionStatus: "START", //开始/上架
        pageNumber: 1,
        pageSize: 20,
      },
      bargainList: [], //砍价活动列表
    };
  },
  onShow() {
    this.params.pageNumber = 1;
    this.bargainList = [];
    this.init();
  },
  onReachBottom() {
    this.params.pageNumber++;
    this.init();
  },
  methods: {
    // 返回上一级
    back() {
      uni.switchTab({
        url: "/pages/tabbar/home/index",
      });
    },
    /**
     * 初始化砍价列表
     */
    async init() {
      let res = await getBargainList(this.params); //砍价列表
      if (res.data.success) {
        this.bargainList.push(...res.data.result.records);
      }
    },

    // 跳转到砍价详情
    navigateToBargainDetail(val) {
      uni.navigateTo({
        url: `/pages/promotion/bargain/detail?id=${val.id}`,
      });
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
  top: 750rpx;
  width: 94%;
  margin: 0 auto;
  > .bargain {
    padding: 32rpx;
  }
}
.bargain-item {
  align-items: center;
  border-bottom: 1rpx solid #f6f6f6;
  padding: 32rpx 0;
}
.goods-config {
  flex: 8;
  margin-left: 20rpx;
  > .goods-title {
    height: 80rpx;
    font-weight: bold;
  }
}
.max-price {
  color: $main-color;
  font-size: 24rpx;
  > span {
    font-size: 32rpx;
    font-weight: bold;
  }
}
.goods-buy {
  margin: 10rpx 0;
  align-items: center;
  justify-content: space-between;
}
.bargaining {
  font-size: 24rpx;
  color: #fff;
  background: $light-color;
  padding: 10rpx 24rpx;
  border-radius: 100px;
}
.empty {
  height: 400rpx;
}
</style>