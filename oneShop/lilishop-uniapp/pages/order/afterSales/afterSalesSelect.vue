<template>
  <view>
    <view class="after-sales-goods-detail-view">
      <view class="header">
        <view>
          本次售后服务将由
          <text class="seller-name">{{ sku.storeName }}</text>
          为您提供
        </view>
      </view>
      <view>
        <view class="goods-item-view" :key="index" v-for="(item,index) in sku.orderItems" v-if="item.sn == sn" @click="navigateToGoodsDetail(sku.skuId)">
          <view class="goods-img">
            <u-image border-radius="6" width="131rpx" height="131rpx" :src="item.image"></u-image>
          </view>
          <view class="goods-info">
            <view class="goods-title u-line-2">{{ item.name }}</view>
            <view class="goods-price">
              <span v-if="sku.orderItems.length <= 1">￥{{ sku.flowPrice }}</span>
              <span class="num" v-else>购买数量{{item.num}}</span>
              <span v-if="sku.orderItems.length <= 1" class="num">购买数量: {{ item.num }}</span>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="select-view">
      <view class="select-cell"  v-if="applyInfo.returnGoods"  @click="onSelect(1)">
        <view class="select-image">
          <image style="height: 51rpx; width: 51rpx" src="/static/order/t1.png"></image>
        </view>
        <view class="right">
          <view class="select-title">退货</view>
          <view class="select-sub-title">
            退回收到的商品
            <u-icon name="arrow-right"  color="#bababa"></u-icon>
          </view>
        </view>
      </view>
      <view class="select-cell" v-if="applyInfo.returnMoney"  @click="onSelect(3)">
        <view class="select-image">
          <image style="height: 51rpx; width: 51rpx" src="/static/order/t3.png"></image>
        </view>
        <view class="right">
          <view class="select-title">退款</view>
          <view class="select-sub-title">
            退款商品返还金额
            <u-icon name="arrow-right"  color="#bababa"></u-icon>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getAfterSaleInfo } from "@/api/after-sale";
import storage from "@/utils/storage";
export default {
  data() {
    return {
      sn: "",
      sku: {}, //sku
      applyInfo:""
    };
  },
  onLoad(options) {
    this.sn = options.sn;
    this.sku = storage.getAfterSaleData();
    // 查看当前商品是否支持退款退货
    this.init()
  },
  methods: {
    // 初始化数据
    init() {
      getAfterSaleInfo(this.sn).then((response) => {
        if (response.data.success) {
          this.applyInfo = response.data.result;
        }
      });
    },

    /**
     * 选择退货流程
     */
    onSelect(value) {
      uni.redirectTo({
        url: `./afterSalesDetail?sn=${this.sn}&value=${value}`,
      });
    },

    /**
     * 跳转到商品信息
     */
    navigateToGoodsDetail(id) {
      uni.navigateTo({
        url: `/pages/product/goods?id=${id}&goodsId=${goodsId}`,
      });
    },
  },
};
</script>

<style lang="scss">
page,
.content {
  background: $page-color-base;
  height: 100%;
}
.after-sales-goods-detail-view {
  background-color: #fff;
  .header {
    background-color: #f7f7f7;
    color: #999999;
    font-size: 22rpx;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    line-height: 70rpx;
    .header-text {
      background-color: #999999;
      padding: 10rpx 30rpx;
      border-radius: 50rpx;
    }
    .seller-name {
      color: $main-color;
    }
  }

  .goods-item-view {
    display: flex;
    flex-direction: row;
    padding: 10rpx 30rpx;
    background-color: #eef1f2;
    .goods-img {
    }
    .goods-info {
      padding-left: 30rpx;
      flex: 1;
      .goods-title {
        margin-bottom: 10rpx;
        color: $font-color-dark;
      }
      .goods-specs {
        font-size: 24rpx;
        margin-bottom: 10rpx;
        color: #cccccc;
      }
      .goods-price {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        font-size: 28rpx;
        margin-bottom: 10rpx;
        color: $light-color;
        .num {
          font-size: 24rpx;
          color: #999999;
        }
      }
    }
    .goods-num {
      width: 60rpx;
      color: $main-color;
    }
  }
}
.select-view {
  background-color: #fff;
  margin-top: 20rpx;
  .select-cell {
    display: flex;
    align-items: center;
    margin: 0rpx 20rpx;
    line-height: 110rpx;
    border-bottom: 1px solid $page-color-base;
    .select-image {
      width: 51rpx;
      height: 110rpx;
      line-height: 110rpx;
      display: flex;
      align-items: center;
    }
    .right {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      .select-title {
        margin-left: 18rpx;
        color: #666666;
        width: 200rpx;
      }
      .select-sub-title {
        color: #cccccc;
      }
    }
  }
}
</style>
