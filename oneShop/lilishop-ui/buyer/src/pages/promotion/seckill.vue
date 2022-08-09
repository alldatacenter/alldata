<template>
  <div class="seckill-list">
    <BaseHeader></BaseHeader>
    <Search />
    <!-- 秒杀时间段 -->
    <div class="promotion-decorate">限时秒杀</div>
    <ul class="time-line">
      <template v-for="(time, index) in list">
        <li v-if="index < 5" @click="currIndex = index" :key="index" :class="{'currTimeline': currIndex === index}">
          <div>{{time.timeLine+':00'}}</div>
          <div v-if="currIndex === index">
            <p>{{nowHour >= time.timeLine ? '秒杀中' : '即将开始'}}</p>
            <p>{{nowHour >= time.timeLine ? '距结束' : '距开始'}}&nbsp;{{currTime}}</p>
          </div>
          <div v-else class="not-curr">
            {{nowHour >= time.timeLine ? '秒杀中' : '即将开始'}}
          </div>
        </li>
      </template>
    </ul>
    <!-- 秒杀商品列表 -->
    <div class="goods-list">
      <empty v-if="goodsList.length === 0" />
      <div
        v-else
        class="goods-show-info"
        v-for="(item, index) in goodsList"
        :key="index"
        @click="goGoodsDetail(item.skuId, item.goodsId)"
      >
        <div class="goods-show-img">
          <img width="220" height="220" :src="item.goodsImage" />
        </div>
        <div class="goods-show-price">
          <span>
            <span class="seckill-price text-danger">{{
              item.price | unitPrice("￥")
            }}</span>
            <span style="color:#999;text-decoration:line-through;">{{item.originalPrice | unitPrice('￥')}}</span>
          </span>
        </div>
        <div class="goods-show-detail">
          <span>{{ item.goodsName }}</span>
        </div>
        <div class="goods-seckill-btn" :class="{'goods-seckill-btn-gray' : nowHour < list[currIndex].timeLine}">{{nowHour >= list[currIndex].timeLine ? '立即抢购' : '即将开始'}}</div>
        <div class="goods-show-num">
          已售<Progress style="width:110px"  class="ml_10" :percent="Math.ceil(item.salesNum/(item.quantity+item.salesNum))" />
        </div>
        <div class="goods-show-seller">
          <span>{{ item.storeName }}</span>
        </div>
      </div>
    </div>
    <BaseFooter />
  </div>
</template>
<script>
import {seckillByDay} from '@/api/promotion'
export default {
  data () {
    return {
      list: [], // 秒杀时段列表
      goodsList: [], // 商品列表
      interval: null, // 定时器
      currIndex: 0, // 当前时间段的下标
      currTime: 0, // 当前显示的倒计时
      diffSeconds: 0, // 倒计时时间戳
      nowHour: new Date().getHours() // 当前小时数
    }
  },
  beforeDestroy () {
    // 销毁前清除定时器
    clearInterval(this.interval);
  },
  watch: {
    currIndex (val) {
      clearInterval(this.interval)
      this.interval = null
      this.nowHour = new Date().getHours()
      this.countDown(val)
      this.goodsList = this.list[val].seckillGoodsList
    },
    diffSeconds (val) {
      const hours = Math.floor(val / 3600);
      // 当前秒数 / 60，向下取整
      // 获取到所有分钟数 3600 / 60 = 60分钟
      // 对60取模，超过小时数的分钟数
      const minutes = Math.floor(val / 60) % 60;
      // 当前的秒数 % 60，获取到 超过小时数、分钟数的秒数（秒数）
      const seconds = val % 60;
      this.currTime = filteTime(hours) + ':' + filteTime(minutes) + ':' + filteTime(seconds)
      if (val <= 0) {
        clearInterval(this.interval)
        this.interval = null
      }
      function filteTime (time) {
        if (time < 10) {
          return '0' + time
        } else {
          return time
        }
      }
    }
  },
  methods: {
    getListByDay () { // 当天秒杀活动
      seckillByDay().then(res => {
        if (res.success) {
          this.list = res.result
          this.goodsList = this.list[0].seckillGoodsList
          this.countDown(this.currIndex)
        }
      })
    },
    goGoodsDetail (skuId, goodsId) {
      // 跳转商品详情
      let routeUrl = this.$router.resolve({
        path: '/goodsDetail',
        query: { skuId, goodsId }
      });
      window.open(routeUrl.href, '_blank');
    },
    countDown (currIndex) { // 倒计时
      // 0点时间戳
      let zeroTime = new Date(new Date().toLocaleDateString()).getTime();
      let currTime = new Date().getTime()
      let actTime = 0;
      let nowHour = new Date().getHours(); // 当前小时数
      if (this.list[currIndex].timeLine > nowHour) { // 活动未开始
        actTime = zeroTime + this.list[currIndex].timeLine * 3600 * 1000
      } else if (this.list[currIndex].timeLine <= nowHour) { // 活动进行中
        if (currIndex === this.list.length - 1) { // 如果是最后一个活动，直到24点结束
          actTime = zeroTime + 24 * 3600 * 1000
        } else {
          actTime = zeroTime + this.list[currIndex + 1].timeLine * 3600 * 1000
        }
      }
      this.diffSeconds = Math.floor((actTime - currTime) / 1000)
      this.interval = setInterval(() => {
        this.diffSeconds--
      }, 1000)
    }
  },
  mounted () {
    this.getListByDay()
  }
}
</script>
<style lang="scss" scoped>
@import '../../assets/styles/goodsList.scss';
.goods-seckill-btn {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 80px;
  color: #fff;
  height: 35px;
  text-align: center;
  line-height: 35px;
  font-size: 14px;
  background-color: $theme_color;
}
.goods-seckill-btn-gray {
  background-color: #666;
}
.promotion-decorate::before,.promotion-decorate::after{
  background-image: url('/src/assets/images/sprite@2x.png');
}
.time-line{
  width: 1200px;
  height: 60px;
  margin: 0 auto;
  background-color: #fff;
  display: flex;
  li{
    padding: 0 30px;
    font-size: 16px;
    font-weight: bold;
    width: 240px;
    height: 100%;
    display: flex;
    align-items: center;
    &:hover{
      cursor: pointer;
    }
    .not-curr {
      border: 1px solid #999;
      border-radius: 20px;
      padding: 3px 10px;
      margin-left: 10px;
      font-size: 12px;
      font-weight: normal;
    }
  }
  .currTimeline{
    background-color: $theme_color;
    color: #fff;
    >div:nth-child(1) {
      font-size: 20px;
    }
    >div:nth-child(2) {
      font-size: 14px;
      margin-left: 10px;
    }
  }
}
</style>
