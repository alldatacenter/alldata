<template>
  <div class="layout">
    <div class="join-list">
      <div
        v-for="(item, index) in res.list"
        :key="index"
        class="join-list-item"
        @click="goToDetail(item.type)"
      >
        <div>
          <div class="join-title">
            <div>{{ item.title }}</div>
            <div
              class="sub"
              v-if="item.type !== 'SECKILL'"
              :style="{
                backgroundColor: item.bk_color,
                color: item.color1,
                borderColor: item.bk_color,
              }"
            >
              {{ item.title1 }}
            </div>
            <div class="sub-seckill" v-else>
              <div class="sub-seckill-block flex">
                <div class="sub-seckill-block-text">
                  {{ timeLine[0] ? timeLine[0].timeLine : "x" }}点场
                </div>
                {{ times.hours == "00" ? "0" : times.hours }}:{{
                  times.minutes
                }}:{{ times.seconds }}
              </div>
            </div>
          </div>
          <div class="join-box">
            <div
              class="join-item"
              v-for="(i, _index) in item.data"
              :key="_index"
            >
              <div class="item-img-box">
                <img
                  class="item-img"
                  :src="i.thumbnail ? i.thumbnail : i.goodsImage"
                  alt
                />
              </div>
              <div
                class="ellipsis"
                :class="{ 'max-width': res.list.length <= 1 }"
              >
                {{ i.goodsName ? i.goodsName : i.name }}
              </div>
              <div class="item-price">
                <span>￥{{ i.price ? i.price : i.originalPrice }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import * as API_Promotions from "@/api/promotions";
import Foundation from "@/utils/Foundation.js";
export default {
  props: ["res"],
  data() {
    return {
      timeLine: "", //获取几个点活动
      resTime: 0, //当前时间
      time: 0, //距离下一个活动的时间值
      times: {}, //时间集合
      onlyOne: "", //是否最后一个商品
    };
  },
  mounted() {
    let params = {
      pageNumber: 1,
      pageSize: 2,
      status: "START",
      promotionStatus: "START",
    };
    this._setTimeInterval = setInterval(() => {
      if (this.time <= 0) {
        clearInterval(this._setTimeInterval);
      } else {
        this.times = Foundation.countTimeDown(this.time);
        this.time--;
      }
    }, 1000);
    this.res.list.forEach((ele) => {
      switch (ele.type) {
        case "PINTUAN":
          API_Promotions.getAssembleList(params).then((response) => {
            const data = response.data.result.records;
            if (data) {
              ele.data = data;
            }
          });
          break;
        case "SECKILL":
          API_Promotions.getSeckillTimeLine().then((response) => {
            if (response.data.success && response.data.result) {
              ele.data = response.data.result[0].seckillGoodsList.slice(0, 2);
              let timeLine = response.data.result.sort(
                (x, y) => Number(x.timeLine) - Number(y.timeLine)
              );
              this.timeLine = timeLine.slice(0, 5);
              this.resTime = parseInt(new Date().getTime() / 1000);
              this.onlyOne = response.data.result.length === 1;
              this.diffTime = parseInt(new Date().getTime() / 1000) - this.resTime;

              this.time =
                this.timeLine[0].distanceStartTime ||
                (this.timeLine[1] && this.timeLine[1].distanceStartTime) ||
                Foundation.theNextDayTime() - this.diffTime;
              this.times = Foundation.countTimeDown(this.time);
              console.log(this.timeLine);
            }
          });
          break;
        case "LIVE":
          API_Promotions.getLiveList(params).then((response) => {
            if (response.success && response.result.records) {
              ele.data = response.result.records[0].commodityList.slice(0, 2);
            }
          });
          break;
        case "KANJIA":
          API_Promotions.getBargainList(params).then((response) => {
            if (response.success && response.result) {
              ele.data = response.result.records(0, 2);
            }
          });
          break;
        default:
          break;
      }
    });
  },
  methods: {
    //跳转详情
    goToDetail(type) {
      switch(type) {
        case "SECKILL":
          uni.navigateTo({
            url: `/pages/promotion/seckill`,
          });
          break;
        case "PINTUAN":
          uni.navigateTo({
            url: `/pages/promotion/joinGroup`,
          });
          break;
        case "LIVE":
          uni.navigateTo({
            url: `/pages/promotion/lives`,
          });
          break;
        case "KANJIA":
          uni.navigateTo({
            url: `/pages/promotion/bargain/list`,
          });
          break;
      };
    }
  },
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.join-box {
  display: flex;
}
.join-list {
  width: 100%;
  display: flex;
  overflow: hidden;
}
.join-list-item {
  flex: 1;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 108rpx; // 大于1个活动
  font-size: 22rpx;
}
.max-width {
  width: 316rpx !important;
}
.item-price {
  > span {
    font-size: 28rpx;
    font-weight: 500;
    color: #e1212b;
  }
}
.join-item {
  flex: 1;
}
.item-img {
  width: 150rpx;
  height: 150rpx;
  margin: 0 auto;
  display: block;
}
.item-img-box {
  position: relative;
}
.item-line-through {
  > span {
    font-size: 20rpx;
    font-weight: 400;
    text-decoration: line-through;
    color: #999;
  }
}
.item-position-tips {
  position: absolute;
  right: 0;
  color: #fff;
  font-size: 24rpx;
  bottom: 0;
}
.join-title {
  display: flex;

  align-items: center;
  background: #fff;
  height: 100rpx;
  > div:nth-of-type(1) {
    font-size: 30rpx;
    font-weight: bold;
  }
  > div:nth-of-type(2) {
    font-size: 20rpx;
    line-height: 1.75;
    border-radius: 16rpx;
    text-align: center;
    padding: 0 16rpx;
    margin-left: 20rpx;
  }
  .sub {
    background-color: #e1212b;
    margin-right: 80rpx;
  }
  .sub-seckill {
    white-space: nowrap;
    padding: 0 !important;
  }
  .sub-seckill-block {
    background: rgba($main-color, 0.3);
    border-radius: 100px !important;
    color: rgba($main-color, 0.7);
    overflow: hidden;
    padding-right: 8rpx;
  }
  .sub-seckill-block-text {
    background-color: $main-color;
    color: #fff;
    border-top-right-radius: 100px;
    border-bottom-right-radius: 100px;
    padding: 0 12rpx !important;
    margin-right: 12rpx;
  }
}
</style>
