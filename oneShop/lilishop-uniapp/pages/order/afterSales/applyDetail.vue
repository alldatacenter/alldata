<template>
  <view v-if="serviceDetail">
    <view class="after-sales-goods-detail-view">
      <view class="header">
        <view>
          本次售后服务将由
          <text class="seller-name">{{ serviceDetail.storeName }}</text>
          为您提供
        </view>
      </view>
      <view class="apply-info-view">
        <view class="status-info">
          <view class="status-info-box">
            <view class="status-val">{{
            serviceDetail.serviceStatus | serviceStatusList
            }}</view>

            <view class="status-tip">{{
              serviceDetail.serviceStatus | statusFilter
            }}</view>
          </view>
        </view>
        <view class="log-box-bottom"></view>
        <view class="log-box-top" @click="onProgress()">
          <view class="top01">
            <view>审核日志</view>
            <view class="log-first-show" v-if="logs[0]">{{
              logs[0].message
            }}</view>
          </view>
         <u-icon name="arrow-right" style="margin-right: 5px" size="30" color="#999"></u-icon>
        </view>
      </view>
      <view class="goods-info">
        <view class="info-box">
          <view class="goods-item-view" @click="navgiateToGoodsDetail(serviceDetail)">
            <view class="goods-img">
              <u-image border-radius="6" width="131rpx" height="131rpx" :src="serviceDetail.goodsImage"></u-image>
            </view>
            <view class="goods-info">
              <view class="goods-title u-line-2">{{
                serviceDetail.goodsName
              }}</view>

              <view class="goods-price">
                <view class="price"> ￥{{ serviceDetail.flowPrice | unitPrice }}</view>
                <view>
                  <view>申请售后数量：{{ serviceDetail.num }}</view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="apply-detail-view">
      <view class="detail-item">
        <view class="title">服务单号:</view>
        <view class="value">{{ serviceDetail.sn }}</view>
      </view>
      <view class="detail-item">
        <view class="title">订单编号:</view>
        <view class="value">{{ serviceDetail.orderSn }}</view>
      </view>
      <view class="detail-item" v-if="serviceDetail.new_order_sn">
        <view class="title">新订单编号:</view>
        <view class="value">{{ serviceDetail.new_order_sn }}</view>
      </view>
      <view class="detail-item">
        <view class="title">服务类型:</view>
        <view class="value">{{
          serviceTypeList[serviceDetail.serviceType]
        }}</view>
      </view>
      <view class="detail-item">
        <view class="title">申请原因:</view>
        <view class="value">{{ reason }}</view>
      </view>
      <!-- <view class="detail-item" v-if="serviceDetail.apply_vouchers">
				<view class="title">申请凭证:</view>
				<view class="value">{{ serviceDetail.apply_vouchers }}</view>
			</view> -->
      <view class="detail-item" v-if="serviceDetail.problemDesc">
        <view class="title">问题描述:</view>
        <view class="value">{{ serviceDetail.problemDesc }}</view>
      </view>

      <view class="detail-item" v-if="
          serviceDetail.afterSaleImage &&
          serviceDetail.afterSaleImage.split(',').length != 0
        ">
        <view v-for="(img, index) in serviceDetail.afterSaleImage.split(',')" :key="index">
          <u-image width="100" height="100" :src="img" @click="preview(serviceDetail.afterSaleImage.split(','), index)"
            ></u-image>
        </view>
      </view>
      <!-- 如果服务类型为退款则不显示 -->
      <view class="detail-item"
        v-if="serviceDetail.serviceType != 'RETURN_MONEY' && serviceDetail.serviceStatus != 'APPLY'">
        <view class="title">收货地址:</view>
        <view class="value">
          <span v-if="storeAfterSaleAddress.salesConsigneeAddressPath">{{
            storeAfterSaleAddress.salesConsigneeAddressPath
          }}</span>
        </view>
      </view>
      <!-- 如果服务类型为退款则不显示 -->
      <view class="detail-item"
        v-if="serviceDetail.serviceType != 'RETURN_MONEY'  && serviceDetail.serviceStatus != 'APPLY'">
        <view class="title">联系人:</view>
        <view class="value">{{ storeAfterSaleAddress.salesConsigneeName }}</view>
      </view>
      <!-- 如果服务类型为退款则不显示 -->
      <view class="detail-item"
        v-if="serviceDetail.serviceType != 'RETURN_MONEY'  && serviceDetail.serviceStatus != 'APPLY'">
        <view class="title">联系方式:</view>
        <view class="value">{{
          storeAfterSaleAddress.salesConsigneeMobile || "" | secrecyMobile
        }}</view>
      </view>
      <view v-if="refundShow">
        <view class="detail-item">
          <view class="title">退款金额:</view>
          <view class="value">{{
            serviceDetail.flowPrice | unitPrice("￥")
          }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.agree_price">
          <view class="title">同意退款:</view>
          <view class="value">{{
            serviceDetail.agree_price | unitPrice("￥")
          }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.actual_price">
          <view class="title">实际退款:</view>
          <view class="value">{{
            serviceDetail.actual_price | unitPrice("￥")
          }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.actual_price">
          <view class="title">退款时间:</view>
          <view class="value">{{
            serviceDetail.refund_time | unixToDate
          }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.refund_price !== 0">
          <view class="title">退款方式:</view>
          <view class="value">{{
            serviceDetail.refundWay | refundWayFilter
          }}</view>
        </view>
        <view class="detail-item" v-if="accountShow && serviceDetail.refund_price != 0">
          <view class="title">账户类型:</view>
          <view class="value">{{
            serviceDetail.accountType | accountTypeFilter
          }}</view>
        </view>
        <view class="detail-item" v-if="
            accountShow && !bankShow && serviceDetail.actualRefundPrice != 0
          ">
          <view class="title">退款账号:</view>
          <view class="value">{{ serviceDetail.bankAccountNumber }}</view>
        </view>
        <view class="detail-item" v-if="bankShow">
          <view class="title">银行名称:</view>
          <view class="value">{{ serviceDetail.bankAccountName }}</view>
        </view>
        <view class="detail-item" v-if="bankShow">
          <view class="title">银行账号:</view>
          <view class="value">{{ serviceDetail.bankAccountNumber }}</view>
        </view>
        <view class="detail-item" v-if="bankShow">
          <view class="title">银行开户名:</view>
          <view class="value">{{ serviceDetail.bankAccountName }}</view>
        </view>
        <view class="detail-item" v-if="bankShow">
          <view class="title">银行开户行:</view>
          <view class="value">{{ serviceDetail.bankDepositName }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.mlogisticsName">
          <view class="title">回寄快递:</view>
          <view class="value">{{ serviceDetail.mlogisticsName }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.mlogisticsNo">
          <view class="title">回寄运单号:</view>
          <view class="value">{{ serviceDetail.mlogisticsNo }}</view>
        </view>
        <view class="detail-item" v-if="serviceDetail.mDeliverTime">
          <view class="title">回寄时间:</view>
          <view class="value">{{ serviceDetail.mDeliverTime }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getServiceDetail,
  getStoreAfterSaleAddress,
  getAfterSaleLog,
  getAfterSaleReason,
} from "@/api/after-sale.js";
export default {
  data() {
    return {
      reason: "", //申请原因
      serviceTypeList: {
        // 售后类型
        CANCEL: "取消",
        RETURN_GOODS: "退货",
        EXCHANGE_GOODS: "换货",
        RETURN_MONEY: "退款",
      },
      serviceDetail: {}, // 售后详情
      logs: [], //日志
      goodsList: [], //商品列表
      storeAfterSaleAddress: {}, //售后地址
      refundShow: false, //退款开关
      accountShow: false, //账户显示
      bankShow: false, //银行显示
      sn: "", //订单sn
    };
  },
  onLoad(options) {
    uni.setNavigationBarTitle({
      title: "服务单详情",
    });
    this.sn = options.sn;
    this.loadDetail();
    this.getAddress();
    this.getLog(options.sn);
  },
  filters: {
    /**
     * 售后状态信息
     */
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

    /**
     * 退款信息
     */
    refundWayFilter(val) {
      switch (val) {
        case "OFFLINE":
          return "账户退款";
        case "OFFLINE":
          return "线下退款";
        case "ORIGINAL":
          return "原路退回";
        default:
          return "";
      }
    },
    /**
     * 账户信息
     */
    accountTypeFilter(val) {
      switch (val) {
        case "WEIXINPAY":
          return "微信";
        case "ALIPAY":
          return "支付宝";
        case "BANK_TRANSFER":
          return "银行卡";
        default:
          return "";
      }
    },
  },
  methods: {
    /**
     * 点击图片放大或保存
     */
    preview(urls, index) {
      uni.previewImage({
        current: index,
        urls: urls,
        longPressActions: {
          itemList: ["保存图片"],
          success: function (data) {},
          fail: function (err) {},
        },
      });
    },

    /**
     * 获取地址信息
     */
    getAddress() {
      getStoreAfterSaleAddress(this.sn).then((res) => {
        if (res.data.success) {
          this.storeAfterSaleAddress = res.data.result;
        }
      });
    },

    /**
     * 获取日志
     */
    getLog(sn) {
      getAfterSaleLog(sn).then((res) => {
        this.logs = res.data.result;
      });
    },

    /**
     * 获取申请原因
     */
    getReasonList(serviceType) {
      getAfterSaleReason(serviceType).then((res) => {
        if (res.data.success) {
          // 1357583466371219456
          this.reason = this.serviceDetail.reason;
        }
      });
    },

    /**
     * 初始化详情
     */
    loadDetail() {
      uni.showLoading({
        title: "加载中",
      });
      getServiceDetail(this.sn).then((res) => {
        uni.hideLoading();
        this.serviceDetail = res.data.result;
        if (
          this.serviceDetail.serviceType == "RETURN_GOODS" ||
          this.serviceDetail.serviceType === "RETURN_MONEY"
        ) {
          this.refundShow = true;
        }

        this.accountShow =
          (this.serviceDetail.serviceType === "RETURN_GOODS" ||
            this.serviceDetail.serviceType === "ORDER_CANCEL") &&
          this.serviceDetail.refundWay === "OFFLINE";

        this.bankShow =
          this.serviceDetail.accountType === "BANK_TRANSFER" &&
          this.serviceDetail.refundWay === "OFFLINE" &&
          ((this.serviceDetail.serviceType === "RETURN_GOODS") |
            (this.serviceDetail.serviceType === "ORDER_CANCEL") ||
            this.serviceDetail.serviceType === "RETURN_MONEY");

        this.getReasonList(this.serviceDetail.serviceType);
      });
    },

    /**
     * 访问商品详情
     */
    navgiateToGoodsDetail(item) {
      uni.navigateTo({
        url: `/pages/product/goods?id=${item.id}&goodsId=${item.goodsId}`,
      });
    },

    /**
     * 进度
     */
    onProgress() {
      uni.navigateTo({
        url: `./applyProgress?sn=${
          this.serviceDetail.sn
        }&createTime=${encodeURIComponent(this.serviceDetail.createTime)}
         &logs=${encodeURIComponent(JSON.stringify(this.logs))}&serviceStatus=${
          this.serviceDetail.serviceStatus
        }`,
      });
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
  .apply-info-view {
    background: $page-color-base;
  }

  .goods-item-view {
    display: flex;
    flex-direction: row;
    padding: 20rpx 30rpx;
    background-color: #eef1f2;
    .goods-info {
      padding-left: 30rpx;
      flex: 1;
      .goods-title {
        margin-bottom: 10rpx;
        font-size: 28rpx;
        color: $font-color-dark;
      }
      .goods-specs {
        font-size: 24rpx;
        margin-bottom: 10rpx;
        color: #cccccc;
      }
      .goods-price {
        display: flex;
        justify-content: space-between;
        font-size: 24rpx;
        color: #999999;
      }
      .price {
        color: $light-color;
      }
    }
    .goods-num {
      width: 60rpx;
      color: $main-color;
    }
  }
}
.apply-detail-view {
  background-color: #f7f7f7;
  margin-top: 10rpx;
  padding: 20rpx;
  color: #666666;

  .detail-item {
    padding: 12rpx;
    display: flex;
    flex-direction: row;
    align-items: center;
    font-size: 24rpx;
    .title {
      padding-left: 10rpx;
      width: 140rpx;
    }
    .value {
      padding-left: 40rpx;
    }
  }
}

.log-box-bottom {
  height: 120rpx;
  flex-direction: column;
  background-color: rgb(247, 247, 247);
}
.log-box-top {
  height: 153rpx;
  display: flex;
  flex-direction: row;
  background-color: rgb(255, 255, 255);
  position: absolute;
  top: 200rpx;
  left: 0rpx;
  right: 0rpx;
  bottom: 0rpx;
  margin-left: 22rpx;
  margin-right: 22rpx;
  margin-top: 22rpx;
  border-radius: 22rpx;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 52rpx;
  padding-top: 52rpx;
  padding-left: 32rpx;

  .top01 {
    width: 90%;
    font-family: PingFangSC-Regular;
    font-size: 28rpx;
    line-height: 30rpx;
    color: rgb(46, 45, 45);
    overflow: hidden;
    text-overflow: ellipsis;
    overflow-wrap: break-word;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    white-space: normal;

    .log-first-show {
      flex-direction: row;
      margin-top: 16rpx;
      margin-right: 44rpx;
      font-family: PingFangSC-Regular;
      font-size: 22rpx;
      color: rgb(140, 140, 140);
      line-height: 30rpx;
    }
  }
}
.status-info {
  flex-direction: row;
  background-color: $light-color;

  .status-info-box {
    height: 180rpx;
    flex-direction: row;
    padding-left: 54rpx;
    padding-right: 54rpx;
    padding-top: 20rpx;
    font-family: PingFangSC-Regular;
    color: rgb(255, 255, 255);
    background-color: rgba(0, 0, 0, 0);
    line-height: 50rpx;

    .status-val {
      font-size: 32rpx;
    }
    .status-tip {
      font-size: 24rpx;
    }
  }
}
.info-box {
  padding-right: 40rpx 0rpx;
  background-color: #eef1f2;
}
</style>
