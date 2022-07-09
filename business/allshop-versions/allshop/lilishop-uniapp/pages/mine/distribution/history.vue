<template>
  <view class="log-list">
    <!-- 提现记录 -->
    <view
      class="log-way"
      v-if="cashLogData.length != 0"
      v-for="(item, index) in cashLogData"
      :key="index"
    >
      <view class="log-item">
        <view class="log-item-view">
          <view class="title">{{
            item.distributionCashStatus == "APPLY"
              ? "待处理"
              : item.distributionCashStatus == "PASS"
              ? "通过"
              : "拒绝"
          }}</view>
          <view class="price">+{{ item.price | unitPrice }}</view>
        </view>
        <view class="log-item-view">
          <view>{{ item.createTime }}</view>
          <view></view>
        </view>
      </view>
    </view>
    <!-- 分销业绩 -->
    <view
      class="log-way"
      v-if="achievementData.length != 0"
      v-for="(item, index) in achievementData"
      :key="index"
    >
      <view class="log-item">
        <view class="log-item-view">
          <view class="title">{{ item.goodsName }}</view>
          <view class="price">提成金额：+{{ item.rebate | unitPrice }}</view>
        </view>
        <view class="log-item-view">
          <view>创建时间：{{ item.createTime }}</view>
          <view>店铺：{{ item.storeName }}</view>
        </view>
        <view class="log-item-footer">
          <view>会员名称：{{ item.memberName }}</view>
          <view>订单金额：{{ item.flowPrice | unitPrice }}</view>
        </view>
        <view class="log-item-footers">
          <view>订单号：{{ item.orderSn }}</view>
        </view>
      </view>
    </view>
    <view class="empty" v-if="empty">
      <u-loadmore :status="status" :icon-type="iconType" bg-color="#f7f7f7" />
    </view>
  </view>
</template>
<script>
import { cashLog, distributionOrderList } from "@/api/goods";
export default {
  data() {
    return {
      cashLogData: [], //提现记录数据集合
      achievementData: [], //分销业绩数据合集,
      status: "loadmore",
      iconType: "flower",
      empty: false,
      params: {
        pageNumber: 1,
        pageSize: 10,
      },

      type: 0,
      routers: "",
      achParams: {
        pageNumber: 1,
        pageSize: 10,
      },
    };
  },
  onLoad(option) {
    let title;
    option.type == 0 ? (title = "分销业绩") : (title = "提现记录");

    uni.setNavigationBarTitle({
      title: title, //这是修改后的导航栏文字
    });
    this.routers = option;
    this.type = option.type;
    option.type == 0 ? this.achievement() : this.history();
  },
  mounted() {},
  onReachBottom() {
    this.status = "loading";
    this.type == 0 ? this.achParams.pageNumber++ : this.params.pageNumber++;
    this.type == 0 ? this.achievement() : this.history();
  },
  methods: {
    // 业绩
    achievement() {
      uni.showLoading({
        title: "加载中",
      });
      distributionOrderList(this.achParams).then((res) => {
        if (res.data.success && res.data.result.records.length >= 1) {
          this.achievementData.push(...res.data.result.records);
        } else {
          this.status = "nomore";
          this.empty = true;
        }
        uni.hideLoading();
      });
    },
    // 初始化提现历史
    history() {
      uni.showLoading({
        title: "加载中",
      });
      cashLog(this.params).then((res) => {
        if (res.data.success && res.data.result.records.length >= 1) {
          this.cashLogData.push(...res.data.result.records);
        } else {
          this.status = "nomore";
          this.empty = true;
        }
        uni.hideLoading();
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.empty {
  margin: 40rpx 0;
}
.price {
  color: $main-color;
  font-weight: bold;
}
.log-list {
  padding: 0 8rpx;
  overflow: hidden;
  margin: 20rpx 0;
}
.log-way {
  margin: 10rpx 0;
  overflow: hidden;
  background: #fff;
  border-radius: 10rpx;
  padding: 20rpx 0;
}
.title {
  font-size: 30rpx;
  font-weight: bold;
}
.log-item-view {
  padding: 8rpx 32rpx;
  display: flex;
  font-size: 13px;
  justify-content: space-between;
}
.log-item-footer {
  padding: 8rpx 32rpx;
  display: flex;
  font-size: 13px;
  justify-content: space-between;
}
.log-item-footers {
  padding: 8rpx 32rpx;
  display: flex;
  font-size: 13px;
  justify-content: space-between;
}
</style>
