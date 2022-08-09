<template>
  <view class="wap">
    <u-navbar back-text="" title="预存款列表">
    </u-navbar>
    <view class="wrapper-show-money">
      <view class="money-view">
        <h3>预存款金额 </h3>
        <view class="money">￥{{walletNum | unitPrice }}</view>

      </view>
    </view>
    <view class="wrapper-tabs">

      <swiper class="swiper-box"  :current="swiperCurrent">
        <swiper-item class="swiper-item" v-for="index in list.length" :key="index">
          <scroll-view class="scroll-v view-wrapper" enableBackToTop="true" scroll-with-animation scroll-y @scrolltolower="loadMore">
            <view v-if="datas.length!=0" class="view-item" v-for="(logItem, logIndex) in datas" :key="logIndex">
              <view class="view-item-detail">
                <view class="-title">{{logItem.detail}}</view>
                <!-- <view class="-number">{{logItem.detail}}</view> -->
              </view>
              <view class="view-item-change">
                <view class="-money green" v-if="logItem.serviceType == 'WALLET_PAY' || logItem.serviceType == 'WALLET_WITHDRAWAL'"> {{logItem.money | unitPrice}} </view>
                <view class="-money" v-if="logItem.serviceType == 'WALLET_REFUND' || logItem.serviceType == 'WALLET_RECHARGE' || logItem.serviceType == 'WALLET_COMMISSION' ">
                  +{{logItem.money | unitPrice}} </view>
                <view class="-time">{{logItem.createTime}}</view>
              </view>
            </view>

            <u-empty v-if="datas.length==0" mode="history" text="暂无记录" />
            <u-loadmore v-else bg-color='#f8f8f8' :status="status" />
          </scroll-view>

        </swiper-item>

      </swiper>
    </view>
  </view>
</template>

<script>
import { getUserRecharge, getWalletLog } from "@/api/members";
import { getUserWallet } from "@/api/members";
export default {
  data() {
    return {
      walletNum: 0,
      status: "loadmore",
      current: 0,
      swiperCurrent: 0,
      userInfo: "", //用户详情信息
      params: {
        pageNumber: 1,
        pageSize: 10,
        order: "desc",
      },
      datas: [], //遍历的数据集合
      rechargeList: "", //充值明细列表
      walletLogList: "", //钱包变动列表
      list: [
        // {
        //   name: "充值明细",
        // },
        {
          name: "预存款变动明细",
        },
      ],
    };
  },
  watch: {
    swiperCurrent(index) {
      this.swiperCurrent = index;
    },
  },
  async mounted() {
    this.getWallet();
    let result = await getUserWallet(); //预存款

    this.walletNum = result.data.result.memberWallet;
  },
  methods: {
   
    /**分页获取预存款充值记录 */
    getRecharge() {
      this.status = "loading";
      getUserRecharge(this.params).then((res) => {
        if (res.data.success) {
          if (res.data.result.records.length != 0) {
            this.status = "loadmore";
            this.datas.push(...res.data.result.records);
          } else {
            this.status = "nomore";
          }
        }
      });
    },

    getWallet() {
      this.status = "loading";
      getWalletLog(this.params).then((res) => {
        if (res.data.success) {
          if (res.data.result.records.length != 0) {
            this.datas.push(...res.data.result.records);
          } else {
            this.status = "nomore";
          }
        }
      });
    },

  
    changed(index) {
      this.datas = [];
      this.swiperCurrent = index;
      this.params.pageNumber = 1;
      if (index == 0) {
        // this.getRecharge();
        this.getWallet();
      } else {
        this.getWallet();
      }
    },

    loadMore() {
      this.params.pageNumber++;
      this.getWallet();
    },
  },
};
</script>

<style lang="scss" scoped>
.green {
    color: $aider-color-green !important;
  }
  .view-item {
    padding: 32rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .view-item-change {
    text-align: right;
    > .-money {
      font-size: 36rpx;
      color: $main-color;
      font-weight: bold;
    }
    > .-time {
      font-size: 22rpx;
      color: #999;
    }
  }
  .view-item-detail {
    line-height: 1.75;
    > .-title {
      font-size: 28rpx;
    }
    > .-number {
      font-size: 22rpx;
      color: #999;
    }
  }
  .submit-btn {
    line-height: 90rpx;
    text-align: center;
  
    color: #fff;
    background: $main-color;
  
    margin: 0 auto;
    height: 90rpx;
  }
  
  .operation {
    font-size: 32rpx;
    margin-right: 24rpx;
    color: rgb(96, 98, 102);
  }
  .money {
    font-size: 40rpx;
    font-weight: bold;
  }
  
  .money-view {
    height: 100%;
    width: 100%;
    padding: 0 32rpx;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    flex-direction: column;
    color: #fff;
    background-image: linear-gradient(
      25deg,
      $main-color,
      $light-color,
      $aider-light-color
    );
  }
  
  .swiper-item,
  .scroll-v {
    height: 100%;
  }
  
  .swiper-box {
    /* #ifndef H5 */
    height: calc(100vh - 200rpx);
    /* #endif */
  
    /* #ifdef H5 */
    height: calc(100vh - 288rpx);
    /* #endif */
  }
  
  .wap {
    width: 100%;
    height: calc(100vh - 44px);
  }
  
  .wrapper-show-money {
    height: 200rpx;
    // background-image: url('/static/img/main-bg.jpg');
  }
</style>
