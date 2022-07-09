<template>
  <view class="content">
    <view class="portrait-box">
      <image src="/static/pointTrade/point_bg_1.png" mode=""></image>
      <image class="point-img" src="/static/pointTrade/tradehall.png" />
      <view class="position-point">
       

      </view>
    </view>
    <u-row class="portrait-box2">
      <u-col span="6" class="portrait-box2-col" :gutter="16">
        <text>累计获得：</text>
        <text class="pcolor">{{ pointData.totalPoint || 0 }}</text>
      </u-col>
      <u-col span="6" class="portrait-box2-col">
        <text>剩余积分：</text>
        <text class="pcolor">{{ pointData.point || 0 }}</text>
      </u-col>
    </u-row>

    <div class="point-list">
      <view class="point-item" v-for="(item, index) in pointList" :key="index">
        <view>
          <view>{{ item.content }}</view>
          <view>{{ item.createTime}}</view>
        </view>
        <view><span>{{item.pointType == "INCREASE" ? '+' : '-'}}</span>{{ item.variablePoint }}</view>
      </view>
      <uni-load-more :status="count.loadStatus"></uni-load-more>
    </div>
  </view>
</template>

<script>
import { getPointsData } from "@/api/members.js";
import { getMemberPointSum } from "@/api/members.js";
export default {
  data() {
    return {
      count: {
        loadStatus: "more",
      },
      pointList: [], //积分数据集合
      params: {
        pageNumber: 1,
        pageSize: 10,
      },
      pointData: {}, //累计获取 未输入 集合
    };
  },

  onLoad() {
    this.initPointData();
    this.getList();
  },

  /**
   * 触底加载
   */
  onReachBottom() {
    this.params.pageNumber++;
    this.getList();
  },
  methods: {
    /**
     * 获取积分数据
     */
    getList() {
      let params = this.params;
      uni.showLoading({
        title: "加载中",
      });
      getPointsData(params).then((res) => {
        uni.hideLoading();
        if (res.data.success) {
          let data = res.data.result.records;
          if (data.length < 10) {
            this.$set(this.count, "loadStatus", "noMore");
            this.pointList.push(...data);
          } else {
            this.pointList.push(...data);
            if (data.length < 10) this.$set(this.count, "loadStatus", "noMore");
          }
        }
      });
    },

    /**
     * 获得累计积分使用
     */
    initPointData() {
      getMemberPointSum().then((res) => {
        this.pointData = res.data.result;
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.point-list{
  margin-top: 50rpx;
}
.title {
  height: 80rpx;
  text-align: center;
  line-height: 80rpx;
  font-size: 32rpx;
  font-weight: bold;
}

.point-item {
  width: 100%;
  height: 130rpx;
  padding: 0 20rpx;
  background: #ffffff;
  font-size: $font-sm;
  border-bottom: 1px solid $border-color-light;
  display: flex;
  justify-content: end;
  align-items: center;
  > view:nth-child(1) {
    flex: 1;
    line-height: 40rpx;
    view {
      color: #666666;
    }
    :last-child {
      color: #999;
    }
  }

  > view:nth-child(2) {
    width: 100rpx;
    text-align: center;
  }
}

.portrait-box2 {
  height: 100rpx;
  background: #ffffff;
  border-radius: 0 0 20rpx 20rpx;
  margin: 0 20rpx;
  font-size: 26rpx;
  /deep/ .u-col {
    text-align: center !important;
  }
  /deep/ .u-col:first-child {
    border-right: 1px solid $border-color-light;
  }
  .pcolor {
    color: #4ebb9d;
  }
}

.content {
  background: #f9f9f9;
}

.more {
  text-align: right;
  color: $u-tips-color;
  font-size: 24rpx;
  padding-right: 40rpx !important;
}

.portrait-box {
  background-color: $main-color;
  height: 250rpx;
  background: linear-gradient(134deg, #28d094 2%, #1abc9c 98%);
  border-radius: 20rpx 20rpx 0 0;
  margin: 20rpx 20rpx 0;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #ffffff;

  > image:first-child {
    width: 263rpx;
    height: 250rpx;
    position: absolute;
    left: 0;
    bottom: 0;
    transform: rotateY(180deg);
  }

  .position-point {
    position: absolute;
    right: -2rpx;
    top: 0;

    .apply-point {
      margin-top: 30rpx;
      text-align: center;
      line-height: 40rpx;
      font-size: $font-sm;
      color: #ffffff;
      width: 142rpx;
      height: 40rpx;
      background: rgba(#ffffff, 0.2);
      border-radius: 20rpx 0px 0px 20rpx;
    }
  }
  .point-img {
    height: 108rpx;
    width: 108rpx;
    margin-bottom: 30rpx;
  }
  .point {
    font-size: 56rpx;
  }
}
</style>
