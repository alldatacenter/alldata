<template>
  <view class="invoice-detail">
    <view class="block-item flex-center">
      <view>
        <view>
          {{'增值税普通发票'}}
          <view class="circle">
            <view></view>
          </view>
        </view>
        <view>{{order.receiptPrice | unitPrice('￥')}}</view>
      </view>
    </view>
    <view class="common-msg flex-center">
      <view>
        <view>抬头类型</view>
        <view>{{order.receiptTitle}}</view>
      </view>
      <view>
        <view>发票状态</view>
        <view class="invoice_status">{{order.receiptStatus === 1?'已开具':'暂未开具'}}</view>
      </view>
    </view>
    <u-cell-group :border="false">
      <u-cell-item title="发票类型" :border-top="false" :value="'增值税普通发票'" :arrow="false"></u-cell-item>
      <u-cell-item title="发票内容" :value="order.receiptContent" :arrow="false"></u-cell-item>
      <u-cell-item title="发票抬头" :value="order.receiptTitle" :arrow="false"></u-cell-item>
      <u-cell-item title="纳税人识别号" v-if="order.taxpayerId" :value="order.taxpayerId" :arrow="false"></u-cell-item>
    </u-cell-group>
    <!-- <u-cell-group :border="false" style="margin-top: 20rpx;">
      <u-cell-item title="订单状态" :border-top="false" :value="order.order_status_text" :arrow="false"></u-cell-item>
      <u-cell-item title="订单编号" :value="order.sn" :arrow="false"></u-cell-item>
    </u-cell-group> -->
    <!-- <view class="show-pic" @click="preview">

      <text>点击预览发票</text>
    </view>
    <button class="btn" @click="download">下载电子发票</button>
    <view class="block-2-view" v-for="(item,index) in order.elec_file_list" :key="index">
      <u-image width="300" height="150" :src="item"></u-image>
    </view> -->
  </view>
</template>

<script>
import { getReceiptDetail } from "@/api/order.js";

export default {
  data() {
    return {
      order: {},
      order: {},
      title_type: "",
    };
  },
  onLoad(options) {
    this.loadData(options.id);
  },
  methods: {
    loadData(id) {
      getReceiptDetail(id).then((res) => {
        let order = res.data.result;
        this.order = order;
      });
    },
    /**
     * 点击图片放大或保存
     */
    preview() {
      //预览发票
      if (this.order.elec_file_list.length) {
        uni.previewImage({
          current: 0,
          urls: this.order.elec_file_list,
          longPressActions: {
            itemList: ["发送给朋友", "保存图片", "收藏"],
            success: function (data) {},
            fail: function (err) {},
          },
        });
      } else {
        uni.showToast({
          title: "暂无发票可预览",
          duration: 2000,
          icon: "none",
        });
      }
    },
    download() {
      //下载发票
      let _this = this;
      if (this.order.elec_file_list.length) {
        this.order.elec_file_list.forEach((item) => {
          uni.downloadFile({
            url: item,
            success: (res) => {
              if (res.statusCode === 200) {
                let tempFilePath = res.tempFilePath;
                uni.saveFile({
                  tempFilePath: tempFilePath,
                  success: function (res) {
                    uni.showToast({
                      title: "发票已下载到" + res.savedFilePath,
                      duration: 2000,
                      icon: "none",
                    });
                  },
                });
              }
            },
          });
        });
      } else {
        uni.showToast({
          title: "暂无发票可下载",
          duration: 2000,
          icon: "none",
        });
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.block-item {
  height: 217rpx;
  width: 100%;
  position: relative;
  > view {
    color: #ff6262;
  }
  > view:first-child {
    text-align: center;
    line-height: 3em;
    > view:first-child {
      position: relative;
      .circle {
        width: 166rpx;
        height: 65rpx;
        border: 1px solid #ff6262;
        border-radius: 100%;
        position: absolute;
        top: 0;
        right: 0;
        left: 0;
        bottom: 0;
        margin: auto;
        view {
          width: 130rpx;
          height: 40rpx;
          border: 1px solid #ff6262;
          border-radius: 100%;
          top: 0;
          right: 0;
          left: 0;
          bottom: 0;
          margin: auto;
          position: absolute;
        }
      }
    }
    > view:last-child {
      font-size: 40rpx;
    }
  }
}

.common-msg {
  flex-direction: row;
  padding: 20rpx;
  height: 118rpx;
  background-color: #ffffff;
  margin-bottom: 20rpx;

  > view {
    width: 50%;
    text-align: center;
    color: #666666;
    line-height: 1.5em;
    view {
      font-size: 24rpx;
    }
    .invoice_status {
      color: #ff6262;
    }
  }

  > view:first-child {
    border-right: 1px solid #eee;
  }
}
.show-pic {
  text-align: center;
  margin-top: 40rpx;
  image {
    width: 27rpx;
    height: 27rpx;
    margin-right: 10rpx;
    vertical-align: middle;
  }
  text {
    color: $main-color;
    font-size: $font-sm;
  }
}

.u-cell {
  padding: 35rpx 20rpx;
  height: 110rpx;
  color: #333333;
}
</style>
