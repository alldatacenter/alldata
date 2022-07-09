<template>
  <view>
    <view class="seller-view" v-for="(item, index) in complaionData" :key="index">
      <view class="seller-info u-flex u-row-between">
        <view class="seller-name">
          <view class="name">{{ item.storeName }}</view>
        </view>
        <view class="order-sn">{{ statusData[item.complainStatus] }}</view>
      </view>
      <u-line color="#DCDFE6"></u-line>
      <view class="goods-item-view">
        <view class="goods-img" @click="handleToGoods(item)">
          <u-image border-radius="6" width="131rpx" height="131rpx" :src="item.goodsImage"></u-image>
        </view>
        <view class="goods-info" @click="handleToGoods(item)">
          <view class="goods-title u-line-2">{{ item.goodsName }}</view>
          <view class="goods-price">
            ￥{{ item.goodsPrice | unitPrice }}
            <!-- <span>+{{ '1' }}积分</span> -->
          </view>
        </view>
        <view class="goods-num">
          <view>x{{ item.num }}</view>
        </view>
      </view>
      <view class="complain-item-view">
        <view class="complain-time"> {{ item.createTime }} </view>
        <view class="complain-speak"> {{ item.complainTopic }} </view>
      </view>
      <view class="complain-btn">
        <u-tag mode="plain" @click="handleClear(item)" class="complain-tag" text="撤销投诉" type="info" v-if="
            item.complainStatus != 'EXPIRED' && item.complainStatus != 'CANCEL'
          " />
        <u-tag mode="plain" @click="handleInfo(item)" class="complain-tag" text="投诉详情" type="info" />
      </view>
    </view>

    <u-empty v-if="empty" :style="{'marginTop':complaionDetail.total == 0 ? '200rpx':'0rpx'}" class="empty" style="" text="暂无投诉列表" mode="list"></u-empty>

    <u-modal show-cancel-button @confirm="handleClearConfirm" v-model="show" :content="content"></u-modal>
  </view>
</template>

<script>
import { getComplain, clearComplain } from "@/api/after-sale";

export default {
  data() {
    return {
      statusData: {
        NEW: "新订单",
        NO_APPLY: "未申请",
        APPLYING: "申请中",
        COMPLETE: "已完成",
        EXPIRED: "已失效",
        CANCEL: "已取消",
      },
      show: false,
      content: "是否撤销投诉？",
      params: {
        pageNumber: 1,
        pageSize: 20,
      },
      complaionDetail: "", //返回的整个response
      complaionData: [], //投诉列表
      empty: false,
      checkComplainData: "", //存储投诉信息
    };
  },
  mounted() {
    this.init();
  },
  /**
   * 触底加载
   */
  onReachBottom() {
    if (
      this.complaionDetail &&
      this.complaionDetail.total < this.params.pageNumber * this.params.pageSize
    ) {
      this.params.pageNumber++;
      this.init();
    }
  },

  methods: {
    // 点击跳转到商品
    handleToGoods(val) {
      uni.navigateTo({
        url: "/pages/product/goods?id=" + val.skuId + "&goodsId=" + val.goodsId,
      });
    },

    /**
     * 点击撤销投诉
     */
    handleClear(val) {
      this.show = true;
      this.checkComplainData = val;
    },
    /**
     * 执行撤销
     */
    handleClearConfirm() {
      clearComplain(this.checkComplainData.id).then((res) => {
        if (res.data.success) {
          uni.showToast({
            title: "撤销成功",
            duration: 2000,
            icon: "none",
          });
          this.complaionData = [];
          this.params.pageNumber = 1;
          this.init();
        }
      });
    },

    /**
     * 查看详情
     */
    handleInfo(val) {
      uni.navigateTo({
        url: "./complainInfo?id=" + val.id,
      });
    },

    /**
     * 初始化投诉列表
     */
    init() {
      uni.showLoading({
        title: "加载中",
      });
      getComplain(this.params).then((res) => {
        this.complaionDetail = res.data.result;
        if (res.data.result.records.length >= 1) {
          this.complaionData.push(...res.data.result.records);
        } else {
          this.empty = true;
        }
        uni.hideLoading();
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../goods.scss";

.complain-item-view {
  border-bottom: 2rpx solid #f5f7fa;
  border-top: 2rpx solid #f5f7fa;
  padding: 20rpx 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.complain-time {
  font-size: 24rpx;
  color: #999;
}
/deep/ .seller-name {
  width: auto !important;
}
.complain-btn {
  padding: 20rpx 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-right: 30rpx;
}
.complain-tag {
  margin-left: 10rpx;
}
.empty {
  margin-top: 40rpx;
}
</style>
