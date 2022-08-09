<template>
  <div class="box">
    <div v-if="bargainLog.length != 0">
      <div v-for="(item,index) in bargainLog" class="flex" :key="index">
        <div>
          <u-image border-radius="20" width='230' height="230" :src="item.thumbnail"></u-image>
        </div>
        <div class="goods">
          <div class="wes-2">
            {{item.goodsName}}
          </div>
          <div>
            还剩<span class="surplusPrice">{{item.surplusPrice | unitPrice}}元</span>
          </div>

          <div @click="navigateToBargainDetail(item)" v-if="item.status == 'START'" class="buy">
            继续免费领
          </div>
        </div>
        <div class="tips-box">
          <div class="tips" :class="[item.status]">
            {{statusWay[item.status]}}
          </div>
        </div>
      </div>
    </div>
    <div v-else>
      <u-empty style="margin-top:20%;" text="暂无砍价活动"></u-empty>
    </div>
  </div>
</template>

<script>
import { getMineBargainLog } from "@/api/promotions";
export default {
  data() {
    return {
      params: {
        pageNumber: 1,
        pageSize: 10,
      },
      bargainLog: [],
      statusWay: {
        START: "砍价开始",
        FAIL: "砍价失败",
        SUCCESS: "砍价成功",
        END: "活动结束",
      },
    };
  },
  onReachBottom() {
    this.params.pageNumber++;
    this.init();
  },
  onShow() {
    this.params.pageNumber = 1;
    this.bargainLog = [];
    this.init();
  },
  methods: {
    // 初始化砍价记录
    async init() {
      let res = await getMineBargainLog(this.params);
      if (res.data.success) {
        this.bargainLog.push(...res.data.result.records);
      }
    },
    // 跳转到砍价详情
    navigateToBargainDetail(val) {
      uni.navigateTo({
        url: `/pages/promotion/bargain/detail?id=${val.kanjiaActivityGoodsId}`,
      });
    },
  },
};
</script>
<style scoped>
page {
  background: #fff;
}
</style>
<style scoped lang="scss">
.box {
  padding: 0 32rpx;
  background: #fff;
}
.buy {
  background: $light-color;
  color: #fff;
  display: inline;
  padding: 10rpx 0;
  border-radius: 100rpx;
  width: 200rpx;
  text-align: center;
  font-size: 24rpx;
  margin-top: 20rpx;
}
.tips-box {
  flex: 1;
  justify-content: center;
  display: flex;

  align-items: center;
}
.tips {
  color: #999;

  margin-top: 20rpx;
}
.surplusPrice {
  font-size: 40rpx;
  margin-left: 10rpx;
  font-weight: bold;
  color: $light-color;
}
.goods {
  margin: 0 20rpx;
  display: flex;
  flex: 2;
  flex-direction: column;
  justify-content: center;
}
.flex {
  border-bottom: 1rpx solid #f7f7f7;

  padding: 20rpx 0;
  margin: 10rpx 0;
}
.SUCCESS {
  color: $light-color;
}
.START {
  color: $aider-light-color;
}
.END {
  color: #999;
}
.FAIL {
  color: $main-color;
}
</style>