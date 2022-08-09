<template>
  <view class="content">
    <view class="u-tabs-box">
      <u-tabs
        bg-color="#fff"
        :list="list"
        :is-scroll="false"
        :current="current"
        @change="change"
        :active-color="$lightColor"
      ></u-tabs>
    </view>
    <div class="u-tabs-search">
      <u-search
        placeholder="请输入订单编号"
        @search="submitSearchOrderList(current)"
        @clear="submitSearchOrderList(current)"
        @custom="submitSearchOrderList(current)"
        v-model="orderSn"
      >
      </u-search>
    </div>
    <scroll-view class="body-view" scroll-y @scrolltolower="renderDate">
      <view
        class="seller-view"
        v-for="(order, orderIndex) in orderList"
        :key="orderIndex"
      >
        <!-- 店铺名称 -->
        <view class="seller-info u-flex u-row-between" v-if="current == 0">
          <view class="seller-name">
            <view class="name">{{ order.storeName }}</view>
          </view>
          <view class="order-sn">订单编号:{{ order.sn }}</view>
        </view>
        <!-- 申请记录 选项卡 -->
        <view class="seller-info u-flex u-row-between" v-if="current != 0">
          <view class="order-sn"
            >售后单号：{{ order.service_sn || order.sn }}</view
          >
          <view class="order-sn">{{ order.serviceType_text }}</view>
        </view>
        <view v-for="(sku, goodsIndex) in order.orderItems" :key="goodsIndex">
          <view class="goods-item-view" @click="onDetail(order, sku)">
            <view class="goods-img">
              <u-image
                border-radius="6"
                width="100%"
                height="100%"
                :src="sku.image"
              ></u-image>
            </view>
            <view class="goods-info">
              <view class="goods-title u-line-2">{{ sku.name }}</view>
              <!-- 如果商品多个则不显示每个商品价格-->
              <view class="goods-price" v-if="order.orderItems.length <= 1">
                ￥{{ order.flowPrice | unitPrice }}
              </view>
            </view>
            <view class="goods-num">
              <view>x{{ sku.num }}</view>
            </view>
          </view>
          <view class="description">
            <!-- 售后申请  -->
            <view v-if="current === 0 && order.groupAfterSaleStatus">
              <view
                v-if="order.groupAfterSaleStatus.includes('ALREADY_APPLIED')"
                class="cannot_apply not_center"
              >
                <u-icon class="icon" name="info-circle-fill"></u-icon>
                该商品已申请售后服务
              </view>
            </view>
            <view v-if="current === 0 && order.groupAfterSaleStatus">
              <view
                v-if="order.groupAfterSaleStatus.includes('EXPIRED')"
                class="cannot_apply not_center"
                @click="tipsShow = true"
              >
                <u-icon class="icon" name="info-circle-fill"></u-icon>
                该商品无法申请售后
              </view>
            </view>

            <div v-if="current === 1 || current === 2">
              <!-- 申请中 -->
              <view
                class="cannot_apply not_center"
                v-if="order.serviceType == 'RETURN_GOODS'"
              >
                退货处理-{{ order.serviceStatus | serviceStatusList }}</view
              >
              <view
                class="cannot_apply not_center"
                v-if="order.serviceType == 'SUPPLY_AGAIN_GOODS'"
              >
                补发商品-{{ order.serviceStatus | serviceStatusList }}</view
              >
              <view
                class="cannot_apply not_center"
                v-if="order.serviceType == 'RETURN_MONEY'"
              >
                退款-{{ order.serviceStatus | serviceStatusList }}</view
              >
              <view
                class="cannot_apply not_center"
                v-if="order.serviceType == 'EXCHANGE_GOODS'"
              >
                换货-{{ order.serviceStatus | serviceStatusList }}</view
              >
              <view
                class="cannot_apply not_center"
                v-if="order.serviceType == 'CANCEL'"
              >
                取消订单-{{ order.serviceStatus | serviceStatusList }}</view
              >
            </div>

            <!-- 申请记录 -->
          </view>
          <view class="btn-view u-flex u-row-right">
            <!-- 售后申请  -->

            <div class="sale" v-if="current === 0 && sku.afterSaleStatus">
              <div
                v-if="
                  sku.afterSaleStatus.includes('NOT_APPLIED') ||
                  sku.afterSaleStatus.includes('PART_AFTER_SALE')
                "
                @click="applyService(sku.sn, order, sku)"
              >
                <view class="default-btn border"> 申请售后 </view>
              </div>
            </div>
            <view class="after-line">
              <!-- 申请中 -->
              <view
                class="default-btn border"
                v-if="
                  current === 2 &&
                  order.serviceStatus &&
                  order.serviceStatus == 'PASS' &&
                  order.serviceType != 'RETURN_MONEY'
                "
                @click="onExpress(order, sku)"
              >
                提交物流
              </view>
              <view
                @click="close(order, sku)"
                v-if="current === 1"
                class="default-btn close"
              >
                取消售后
              </view>
              <view
                @click="afterDetails(order, sku)"
                v-if="current === 1 || current === 2"
                class="default-btn border"
              >
                售后详情
              </view>
            </view>
          </view>
        </view>
        <view
          v-if="
            current === 0 &&
            order.groupAfterSaleStatus &&
            order.groupAfterSaleStatus != 'ALREADY_APPLIED' &&
            order.orderItems.length >= 1
          "
          class="btn-view u-flex u-row-between"
        >
          <!-- 多个商品显示订单总价格 -->
          <view class="cannot_apply">
            订单总金额:<span class="countMoney"
              >￥{{ order.flowPrice | unitPrice }}</span
            >
          </view>
        </view>
      </view>
      <u-loadmore bg-color="#f8f8f8" :status="status" />
    </scroll-view>
    <u-modal
      show-cancel-button
      @confirm="closeService"
      v-model="cancelShow"
      content="确认取消售后"
    ></u-modal>
    <u-modal
      v-model="tipsShow"
      content="当订单未确认收货|已过售后服务有效期|已申请售后服务时，不能申请售后"
    ></u-modal>
  </view>
</template>

<script>
import uniLoadMore from "@/components/uni-load-more/uni-load-more.vue";
import { getAfterSaleList, cancelAfterSale } from "@/api/after-sale.js";
import { getOrderList } from "@/api/order.js";
import storage from "@/utils/storage";

export default {
  components: {
    uniLoadMore,
  },
  data() {
    return {
      list: [
        //tab表头
        {
          name: "售后申请",
        },
        {
          name: "申请中",
        },
        {
          name: "申请记录",
        },
      ],
      current: 0, //当前表头索引
      tipsShow: false, //提示开关
      cancelShow: false, //取消显示开关
      selectedOrder: "", //选中的order
      orderList: [], //订单集合
      params: {
        pageNumber: 1,
        pageSize: 10,
        sort: "createTime",
        flowPrice: 0,
        order: "desc",
      },

      logParams: {
        pageNumber: 1,
        pageSize: 10,
      },
      status: "loadmore",
      orderSn: "", // 搜索订单sn
    };
  },
  onLoad(options) {
    this.orderList = [];
    this.params.pageNumber = 1;
    if (options.orderSn) this.params.orderSn = options.orderSn;
    this.searchOrderList(this.current);
  },
  onPullDownRefresh() {
    this.change(this.current);
  },
  methods: {
    /**
     * 点击搜索执行搜索
     */
    submitSearchOrderList(current) {
      this.params.pageNumber = 1;
      this.logParams.pageNumber = 1;
      this.orderList = [];
      this.searchOrderList(current);
    },
    /**
     * 切换tab页时，初始化数据
     */
    change(index) {
      this.current = index;
      this.params = {
        pageNumber: 1,
        pageSize: 10,
      };
      this.orderList = [];
      //如果是2 则读取售后申请记录列表
      this.searchOrderList(index);
      uni.stopPullDownRefresh();
    },

    /**
     * 搜索初始化
     * 根据当前tab传值的索引进行更改
     */
    searchOrderList(index) {
      if (index == 0) {
        this.orderSn ? (this.params.orderSn = this.orderSn) : "";
        this.getOrderList();
      } else {
        this.logParams = {
          pageNumber: 1,
          pageSize: 10,
          sort: "createTime",
          order: "desc",
        };
        if (index === 1) {
          this.logParams.serviceStatus = "APPLY";
        }
        this.orderSn ? (this.logParams.orderSn = this.orderSn) : "";
        this.orderList = [];
        this.getAfterSaleLogList();
      }
    },

    /**
     * 获取订单列表
     */
    getOrderList() {
      uni.showLoading({
        title: "加载中",
        mask: true,
      });
      getOrderList(this.params).then((res) => {
        uni.hideLoading();
        const orderList = res.data.result.records;
        if (orderList.length > 0) {
          this.orderList = this.orderList.concat(orderList);
          this.params.pageNumber += 1;
        }
        if (orderList.length < 10) {
          this.status = "nomore";
        } else {
          this.status = "loading";
        }
      });
    },

    close(order, sku) {
      console.log(order, sku);
      this.selectedOrder = order;
      this.cancelShow = true;
    },

    async closeService() {
      uni.showLoading({
        title: "加载中",
      });
      console.log(this.selectedOrder);
      let res = await cancelAfterSale(this.selectedOrder.sn);
      if (res.data.success) {
        uni.showToast({
          title: "取消成功!",
          duration: 2000,
          icon: "none",
        });
      }
      this.orderList = [];
      this.searchOrderList(this.current);

      uni.hideLoading();
    },

    /**
     * 售后详情
     */
    afterDetails(order) {
      uni.navigateTo({
        url: "./applyDetail?sn=" + order.sn,
      });
    },

    /**
     * 申请记录列表
     */
    getAfterSaleLogList() {
      getAfterSaleList(this.logParams).then((res) => {
        let afterSaleLogList = res.data.result.records;

        afterSaleLogList.forEach((item) => {
          item.orderItems = [
            {
              image: item.goodsImage,
              skuId: item.skuId,
              name: item.goodsName,
              num: item.num,
              price: item.flowPrice,
            },
          ];
        });

        this.orderList = this.orderList.concat(afterSaleLogList);

        if (afterSaleLogList.length < 10) {
          this.status = "nomore";
        } else {
          this.status = "loading";
        }
      });
    },

    /**
     * 申请售后
     */
    applyService(sn, order, sku) {
      let data = {
        ...order,
        ...sku,
      };
      storage.setAfterSaleData(data);
      uni.navigateTo({
        url: `/pages/order/afterSales/afterSalesSelect?sn=${sn}`,
      });
    },

    /**
     * 提交物流信息
     */
    onExpress(order, sku) {
      sku.storeName = order.storeName;
      let data = {
        ...order,
        ...sku,
      };

      storage.setAfterSaleData(data);
      uni.navigateTo({
        url: `./afterSalesDetailExpress?serviceSn=${order.sn}`,
      });
    },

    /**
     * 查看详情
     */
    onDetail(goods, sku) {
      // 售后申请
      if (this.current == 0) {
        uni.navigateTo({
          url: `/pages/product/goods?id=${sku.skuId}&goodsId=${
            sku.goodsId || sku.goodsId
          }`,
        });
      } else {
        uni.navigateTo({
          url: `/pages/product/goods?id=${goods.skuId}&goodsId=${
            goods.goodsId || goods.goodsId
          }`,
        });
      }
    },

    /**
     * 底部加载数据
     */
    renderDate() {
      if (this.current === 0) {
        this.params.pageNumber += 1;
        this.getOrderList();
      } else {
        this.logParams.pageNumber += 1;
        this.getAfterSaleLogList();
      }
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

.body-view {
  overflow-y: auto;
  height: calc(100vh - 44px - 80rpx - 104rpx);
}

.u-tabs-search {
  padding: 20rpx;
  background: #fff;
}

.countMoney {
  margin-left: 7rpx;
  color: $main-color;
  font-size: 28rpx;
}

.seller-view {
  background-color: #fff;
  margin: 20rpx 0rpx;
  padding: 0rpx 20rpx;
  border-radius: 20rpx;

  .seller-info {
    height: 70rpx;

    .seller-name {
      font-size: 28rpx;
      display: flex;
      flex-direction: row;

      .name {
        margin-left: 15rpx;
        margin-top: -2rpx;
      }
    }

    .order-sn {
      font-size: 22rpx;
      color: #909399;
    }
  }

  .goods-item-view {
    display: flex;
    flex-direction: row;
    padding: 10rpx 10rpx;

    .goods-img {
      width: 131rpx;
      height: 131rpx;
    }

    .goods-info {
      padding-left: 30rpx;
      flex: 1;

      .goods-title {
        margin-bottom: 10rpx;
        color: #333333;
      }

      .goods-specs {
        font-size: 24rpx;
        margin-bottom: 10rpx;
        color: #cccccc;
      }

      .goods-price {
        font-size: 28rpx;
        margin-bottom: 10rpx;
        color: #ff5a10;
      }
    }

    .goods-num {
      width: 60rpx;
      color: $main-color;
    }
  }

  .btn-view {
    padding: 16rpx 0;

    .after-line {
      display: flex;
      line-height: 90rpx;
    }
  }
}

.description {
  color: #909399;
  size: 25rpx;
}

.cannot_apply {
  text-align: center;
  font-size: 22rpx;

  color: #999999;
  height: 70rpx;
  line-height: 70rpx;
}
.not_center {
  text-align: left;
}

.icon {
  margin-right: 10rpx;
}

.sale {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.default-btn {
  background-color: #ffffff;
  margin-left: 15rpx;
  height: 60rpx;
  line-height: 60rpx;
  text-align: center;
  font-size: 24rpx;
  padding: 0 24rpx;
  border-radius: 200px;
}

.close {
  color: $light-color;
}

.border {
  border: 2rpx solid $light-color;
  color: $light-color;
}
</style>
