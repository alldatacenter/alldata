<template>
  <view class="mp-iphonex-bottom content">
    <u-form :model="form" ref="uForm">
      <view class="after-sales-goods-detail-view">
        <view class="header">
          <view>
            本次售后服务将由
            <text class="seller-name">{{ sku.storeName }}</text>
            为您提供
          </view>
        </view>
        <view>
          <view class="goods-item-view" @click="gotoGoodsDetail(sku.skuId)">
            <view class="goods-img">
              <u-image border-radius="6" width="131rpx" height="131rpx" :src="sku.image"></u-image>
            </view>
            <view class="goods-info">
              <view class="goods-title u-line-2">{{ sku.name }}</view>
              <view class="goods-price">
                <span>￥{{ sku.price | unitPrice }}</span>
                <span class="num">购买数量: {{ sku.num }} </span>
              </view>
            </view>
          </view>
        </view>
      </view>

      <scroll-view scroll-y>
        <!-- 上传凭证 -->
        <view class="opt-view">
          <view class="img-title" style="font-size: 30rpx">填写物流信息</view>
          <u-form-item label="返回方式" :label-width="150">
            <u-input type="text" input-align="right" value="快递至第三方卖家" />
          </u-form-item>
          <u-form-item label="快递公司" :label-width="150">
            <u-input v-model="form.courierCompany" type="select" input-align="right" :select-open="companySelectShow" @click="companySelectShow = true" placeholder="请选择快递公司" />
          </u-form-item>
          <u-form-item label="快递单号" :label-width="150">
            <u-input input-align="right" v-model="form.logisticsNo" placeholder="请输入快递单号" />
          </u-form-item>
          <u-form-item label="发货时间" :label-width="150">
            <u-input input-align="right" type="selects" disabled v-model="form.mDeliverTime" @click="timeshow = true" placeholder="请选择发货时间" />
          </u-form-item>
        </view>
      </scroll-view>

      <view class="submit-view">
        <u-button ripple :customStyle="{'background':$lightColor,'color':'#fff' }" shape="circle" @click="onSubmit">提交申请</u-button>
      </view>
    </u-form>
    <u-select mode="single-column" :list="companyList" v-model="companySelectShow" @confirm="companySelectConfirm"></u-select>
    <u-calendar v-model="timeshow" :mode="'date'" @change="onTimeChange"></u-calendar>
    <u-toast ref="uToast" />
  </view>
</template>

<script>
import { getLogistics } from "@/api/address.js";
import { fillShipInfo } from "@/api/after-sale.js";
import storage from "@/utils/storage";

export default {
  data() {
    return {
      //快递公司 弹出框
      companySelectShow: false,
      companyList: [], //快递公司集合
      timeshow: false, //发货时间
      form: {
        courierCompany: "", //快递公司
        logisticsId: "", //快递公司ID
        logisticsNo: "", //快递单号
        mDeliverTime: "", //发货时间
      },
      serviceDetail: {}, //服务详情
      sku: {}, //sku信息
    };
  },
  onLoad(options) {
    
    this.sku = storage.getAfterSaleData();
    let navTitle = "服务单详情";
    uni.setNavigationBarTitle({
      title: navTitle, //此处写页面的title
    });
    this.serviceDetail.sn = options.serviceSn;
    this.Logistics();
  },
  methods: {
    /**
     * 确认快递公司
     */
    companySelectConfirm(e) {
      this.form.logisticsId = e[0].value;
      this.form.courierCompany = e[0].label;
    },

    /**
     * 获取快递公司
     */
    Logistics() {
      getLogistics().then((res) => {
        if (res.data.success) {
          res.data.result.forEach((item, index) => {
            this.companyList[index] = {
              value: item.id,
              label: item.name,
            };
          });
        }
      });
    },

    /**
     * 更改时间
     */
    onTimeChange(e) {
      this.form.mDeliverTime = e.result;
    },

    /**
     * 点击提交
     */
    onSubmit() {
      uni.showLoading({
        title: "加载中",
        mask: true,
      });
      delete this.form.courierCompany;
      fillShipInfo(this.serviceDetail.sn, this.form).then((res) => {
        uni.hideLoading();
        if (res.statusCode === 200) {
          this.$refs.uToast.show({
            title: "提交成功",
            type: "success",
            back: true,
            url: "/pages/order/afterSales/afterSales",
          });
        }
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
.mp-iphonex-bottom {
  overflow: hidden;
}
.after-sales-goods-detail-view {
  background-color: #fff;
  padding: 10rpx 0rpx;
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
    padding: 20rpx 30rpx;
    background-color: #eef1f2;

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
.opt-view {
  background-color: #fff;
  padding: 40rpx 30rpx 0rpx 30rpx;

  font-size: 26rpx;
  .how-view {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    height: 80rpx;
    border-bottom: 1px solid $page-color-base;
  }
  .explain-view {
    display: flex;
    flex-direction: row;
    align-items: center;
    height: 150rpx;
  }
  .img-title {
    height: 80rpx;
    display: flex;
    flex-direction: row;
    align-items: center;
  }
  .images-view {
    padding: 20rpx;
    display: flex;
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
  }
}
.submit-view {
  position: fixed;
  z-index: 999;
  bottom: 10px;
  left: 0px;
  margin-top: 100rpx;
  height: 100rpx;
  width: 750rpx;
  align-items: center;
  padding: 0rpx 20rpx;
  color: #fff;
}
</style>
