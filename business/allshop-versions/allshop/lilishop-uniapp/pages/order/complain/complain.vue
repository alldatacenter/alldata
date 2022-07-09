<template>
  <view>
    <!-- 商品模块 -->
    <view class="seller-view">
      <view class="seller-info u-flex u-row-between">
        <view class="seller-name">
          <view class="name">{{ order.storeName || "" }}</view>
          <view class="status">{{ orderStatusList[order.orderStatus] }}</view>
        </view>
        <view class="order-sn"></view>
      </view>
      <u-line color="#DCDFE6"></u-line>
      <view class="goods-item-view" v-for="(sku, index) in orderGoodsList" :key="index" v-if="sku.skuId == skuId">
        <view class="goods-img">
          <u-image border-radius="6" width="131rpx" height="131rpx" :src="sku.image"></u-image>
        </view>
        <view class="goods-info">
          <view class="goods-title u-line-2">{{ sku.goodsName }}</view>
          <view class="goods-price">
            ￥{{ sku.flowPrice | unitPrice }}
          </view>
        </view>
        <view class="goods-num">
          <view>x{{ sku.num }}</view>
        </view>
      </view>
    </view>

    <!-- 投诉主题 -->
    <u-select @confirm="confirmComplain" v-model="complainShow" :list="complainList"></u-select>
    <!-- 投诉模块 -->
    <view class="cell">
      <view class="cell-item between" @click="complainShow = true">
        <view class="cell-title"> 投诉主题 </view>
        <view class="cell-view"> {{ complainTopic }} </view>
        <u-icon style="margin-left: 20rpx" name="arrow-down"></u-icon>
      </view>

      <view class="cell-item complain-content">
        <view class="cell-title title"> 投诉内容 </view>
        <view class="cell-view content">
          <u-input type="textarea" height="70rpx" auto-height v-model="complainValue" />
        </view>
      </view>
      <view class="cell-item">
        <view class="cell-title"> 投诉凭证 </view>
        <view class="cell-view">
          <u-upload ref="uUpload" :header=" { accessToken: storage.getAccessToken() }" upload-text=""  :action="action" width="100" @on-uploaded="onUploaded" :max-count="5">
          </u-upload>
        </view>
      </view>
    </view>

    <view class="submit-btn" @click="handleSumit">提交</view>
  </view>
</template>

<script>
import storage from "@/utils/storage.js";
import { getOrderDetail } from "@/api/order.js";
import { getComplainReason, addComplain } from "@/api/after-sale.js";
import { upload } from "@/api/common.js";
export default {
  data() {
    return {
      storage,
      action: upload, //上传图片地址
      orderStatusList: {
        //订单状态列表
        UNDELIVERED: "待发货",
        UNPAID: "未付款",
        PAID: "已付款",
        DELIVERED: "已发货",
        CANCELLED: "已取消",
        COMPLETE: "已完成",
        TAKE: "已完成",
      },
      complainValue: "", //投诉内容
      complainShow: false, //投诉主题开关
      complainTopic: "", //投诉抱怨话题
      complainList: [], // 投诉列表
      images: [], //投诉内容图片
      order: "", //订单
      orderGoodsList: "", //订单商品
      orderDetail: "", //订单详情
      sn: "",
      skuId: "", //商品skuid
    };
  },

  onLoad(option) {
    this.loadData(option.sn);
    this.sn = option.sn;
    this.skuId = option.skuId;
    this.getReasion();
  },

  methods: {
    /**
     * 上传完成
     */
    onUploaded(lists) {
      let images = [];
      lists.forEach((item) => {
        images.push(item.response.result);
      });
      this.images = images;
    },
    /**
     * 提交
     */
    handleSumit() {
      // 循环出商品
      let goods = this.orderGoodsList.filter((item) => {
        return item.skuId == this.skuId;
      });
      //数据赋值
      let data = {
        complainTopic: this.complainTopic, //投诉主题,
        content: this.complainValue, //投诉内容
        goodsId: goods[0].goodsId, //商品id
        images: this.images, //图片
        orderSn: this.sn, //订单号
        skuId: this.skuId, //skuid
      };
      addComplain(data).then((res) => {
        if (res.data.success) {
          uni.showToast({
            title: "提交成功!",
            duration: 2000,
            icon: "none",
          });

          setTimeout(() => {
            uni.redirectTo({
              url: "/pages/order/complain/complainList",
            });
          }, 1000);
        }
      });
    },

    /**
     * 获取投诉原因
     */
    getReasion() {
      getComplainReason().then((res) => {
        if (res.data.result.length >= 1) {
          res.data.result.forEach((item) => {
            let way = {
              value: item.reason,
              label: item.reason,
            };
            this.complainList.push(way);
          });
          this.complainTopic = res.data.result[0].reason;
        }
      });
    },

    /**
     * 加载订单详情
     */
    loadData(sn) {
      uni.showLoading({
        title: "加载中",
      });
      getOrderDetail(sn).then((res) => {
        const order = res.data.result;
        this.order = order.order;
        this.orderGoodsList = order.orderItems;
        this.orderDetail = res.data.result;
        uni.hideLoading();
      });
    },

    /**
     * 确认投诉
     */
    confirmComplain(e) {
      this.complainTopic = e[0].label;
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../goods.scss";
.cell {
  width: 100%;
  background: #fff;
  padding: 26rpx;
}
.cell-item {
  padding: 30rpx 0;
  border-bottom: 2rpx solid #f5f7fa;
  display: flex;
  align-items: center;
}
.complain-content {
  display: flex;
  align-items: center;
  .content {
    width: 100%;
  }
  .title {
    width: 140rpx;
  }
}

/deep/ .u-input__textarea {
  padding: 0;
}
.cell-title {
  font-weight: bold;
  margin-right: 20rpx;
}
.submit-btn {
  width: 70%;
  margin: 0 auto;
  height: 80rpx;
  line-height: 80rpx;
  color: #fff;
  text-align: center;
  background: $light-color;
  margin-top: 20px;
  border-radius: 200px;
}
</style>
