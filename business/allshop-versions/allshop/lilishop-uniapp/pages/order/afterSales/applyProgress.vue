<template>
  <view>
    <view class="info-view">
      <view class="header-title-view">
        <view class="title">售后单号:</view>
        <view>{{ sn }}</view>
      </view>
      <view class="header-title-view">
        <view class="title">申请时间:</view>
        <view>{{ createTime }}</view>
      </view>
    </view>
    <view class="info-view">
      <view class="header-title-view">
        <view>{{ serviceStatus }}</view>
      </view>
    </view>
    <view class="info-view">
      <view>
        <u-time-line v-if="logList.length != 0">
          <u-time-line-item>
            <!-- 此处没有自定义左边的内容，会默认显示一个点 -->
            <template v-slot:content>
              <view v-for="(time,index) in logList" :key="index">
                <view class="u-order-desc">{{time.message}}</view>
                <view class="u-order-time">{{time.createTime}}</view>
              </view>
            </template>
          </u-time-line-item>
        </u-time-line>
        <view v-else>
          <u-empty text="暂无审核日志"></u-empty>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      sn: "", //sn
      createTime: "", //创建时间
      logList: [], //日志集合
      serviceStatus: "", //订单状态
    };
  },
  onLoad(options) {
    this.sn = options.sn;
    this.createTime = decodeURIComponent(options.createTime);
    this.serviceStatus = this.statusFilter(options.serviceStatus);
    this.logList = JSON.parse(decodeURIComponent(options.logs));

  },
  methods: {
    statusFilter(val) {
      switch (val) {
        case "APPLY":
          return "售后服务申请成功，等待商家审核";
        case "PASS":
          return "售后服务申请审核通过";
        case "REFUSE":
          return "售后服务申请已被商家拒绝，如有疑问请及时联系商家";
        case "FULL_COURIER":
          return "申请售后的商品已经寄出，等待商家收货";
        case "STOCK_IN":
          return "商家已将售后商品入库";
        case "WAIT_FOR_MANUAL":
          return "等待平台进行人工退款";
        case "REFUNDING":
          return "商家退款中，请您耐心等待";
        case "COMPLETED":
          return "售后服务已完成，感谢您的支持";
        case "ERROR_EXCEPTION":
          return "系统生成新订单异常，等待商家手动创建新订单";
        case "CLOSED":
          return "售后服务已关闭";
        case "WAIT_REFUND":
          return "等待平台进行退款";
        default:
          return "";
      }
    },
  },
};
</script>

<style lang="scss" scoped>
page,
.content {
  background: $page-color-base;
  height: 100%;
}

.u-order-time {
  font-size: 24rpx;
  color: #999;
  margin: 20rpx 0;
}

.info-view {
  margin: 20rpx 0;
  border-radius: 20rpx;
  background-color: #fff;
  padding: 30rpx;

  .header-title-view {
    display: flex;
    flex-direction: row;
    align-items: center;
    color: #909399;

    .title {
      width: 160rpx;
    }
  }

  .steps-view {
    display: flex;
    flex-direction: row;
    align-items: center;
    color: #909399;
    border-bottom: 1px solid $page-color-base;
    margin-bottom: 10rpx;

    .title {
      width: 160rpx;
    }
  }
}
</style>
