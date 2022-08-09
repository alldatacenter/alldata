<template>
  <div>
    <u-navbar>
      <u-search placeholder="请输入店铺名称" @search="search" @clear="search" @custom="search" v-model="params.storeName">
      </u-search>
    </u-navbar>
    <div class="wrapper" v-if="storeList.length!=0">
      <div class="store-item" @click="handleClickStore(item)" v-for="(item,index) in storeList" :key="index">
        <div>
          <u-image shape="circle" width="100" height="100" :src="item.storeLogo">
          </u-image>
        </div>
        <div class="store-msg">
          <div class="store-name">
            {{item.storeName}}
          </div>
          <div class="goods-num">
            商品 {{item.goodsNum}}
          </div>
          <div class="flex store-distance">
            <div>
              <span class="store-score">{{item.serviceScore | unitPrice}}</span>
              <span class="line">|</span>
              <span class="store-collection">收藏 {{item.collectionNum}}</span>
            </div>
          </div>
        </div>
        <!-- 
          #TODO 后续将和后端补充从此处
          <div class="flex store-goods">
            <div class="store-goods-item" v-for="i in 3" :key="i">
              <div>
                <u-image src="https://picsum.photos/id/341/200/200" border-radius="20" width="215rpx" height="215rpx">
                </u-image>
              </div>
              <div class="price">
                <span>
                  ￥
                  <span class=" goods-price-bigshow">{{ formatPrice(16)[0] }}</span>
                  .{{ formatPrice(16)[1] }}
                </span>

              </div>
              <div class="wes">test</div>
            </div>
           </div> 
        -->
      </div>
    </div>
    <u-empty style="margin-top:20%;" text="暂无店铺信息" v-else></u-empty>
  </div>
</template>

<script>
import { getStoreList } from "@/api/store";
export default {
  data() {
    return {
      keyword: "",
      params: {
        pageNumber: 1,
        pageSize: 10,
        storeName: "",
      },
      storeList: [], // 店铺列表
    };
  },
  onReachBottom() {
    this.params.pageNumber++;
    this.init();
  },
  mounted() {
    this.init();
  },
  methods: {
    handleClickStore(val){
      uni.navigateTo({
         url: `/pages/product/shopPage?id=${val.id}`
      });
    },
    search() {
      this.storeList = [];
      this.init();
    },
    // 格式化金钱  1999 --> [1999,00]
    formatPrice(val) {
      if (typeof val == "undefined") {
        return val;
      }
      return val.toFixed(2).split(".");
    },
    async init() {
      let res = await getStoreList(this.params);
      if (res.data.success) {
        let data = res.data.result;
        
        this.storeList.push(...data.records);
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.wrapper {
  padding: 0 16rpx;
}
.store-item {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx 0;
  padding: 24rpx;
}
.store-msg {
  margin-left: 20rpx;
}
.store-name {
  font-weight: bold;
  font-size: 30rpx;
}
.goods-num,
.store-collection {
  color: #999;
  font-size: 24rpx;
  line-height: 1.5;
}
.store-goods {
  margin: 20rpx 0;
}
.store-goods-item {
  flex: 1;
  overflow: hidden;
  display: flex;
  justify-content: flex-start;
  flex-direction: column;
  margin: 0 5rpx;
}
.wes {
  margin: 10rpx 0;
  width: 100%;
}
.store-score {
  color: $light-color;
  font-size: 24rpx;
  font-weight: bold;
}
.line {
  margin: 0 6rpx;
  color: #999;
}
.store-distance {
  justify-content: space-between;
}
.price {
  margin-top: 10rpx;
  color: $main-color;
}
.goods-price-bigshow {
  font-size: 34rpx;
  font-weight: bold;
}
</style>