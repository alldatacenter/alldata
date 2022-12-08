<template>
  <div class="seckill" v-if="goodsList.length">
    <div class="aside hover-pointer" @click="goToSeckill">
      <div class="title">{{ actName }}</div>
      <div class="hour">
        <span>{{ currHour }}:00</span>点场 倒计时
      </div>
      <div class="count-down" v-if="actStatus === 1">
        <span>{{ hours }}</span>
        <span>{{ minutes }}</span>
        <span>{{ seconds }}</span>
      </div>
      <div class="act-status" v-else>未开始</div>
    </div>
    <swiper :options="swiperOption" ref="mySwiper">
      <swiper-slide v-for="(item,index) in goodsList" :key="index">
        <div class="content hover-pointer"  @click.stop="goToSeckill">
          <img :src="item.goodsImage" width="140" height="140" :alt="item.goodsName">
          <div class="ellipsis">{{item.goodsName}}</div>
          <div>
            <span>{{ item.price | unitPrice('￥') }}</span>
            <span>{{ item.originalPrice | unitPrice('￥') }}</span>
          </div>
        </div>
      </swiper-slide>
      <div class="swiper-button-prev" slot="button-prev">
        <Icon type="ios-arrow-back" />
      </div>
      <div class="swiper-button-next" slot="button-next">
        <Icon type="ios-arrow-forward" />
      </div>
    </swiper>
  </div>
</template>
<script>
// 引入插件
import { swiper, swiperSlide } from 'vue-awesome-swiper';
import 'swiper/dist/css/swiper.css';
export default {
  components: {
    swiper,
    swiperSlide
  },
  props: {
    data: Object
  },
  data () {
    return {
      list: [], // 秒杀时段列表
      goodsList: [], // 商品列表
      actStatus: 0, // 0 未开始  1 进行中
      actName: '限时秒杀', // 活动名称
      currIndex: 0, // 当前时间段的下标
      currHour: '00', // 当前秒杀场
      diffSeconds: 0, // 倒计时秒数
      hours: 0, // 小时
      minutes: 0, // 分钟
      seconds: 0, // 秒
      interval: null, // 定时器
      swiperOption: { // 轮播图参数

        slidesPerView: 5,
        // 设置点击箭头
        navigation: {
          nextEl: '.swiper-button-next',
          prevEl: '.swiper-button-prev'
        }
      }
    };
  },
  watch: {
    currIndex (val) {
      clearInterval(this.interval)
      this.interval = null
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
      this.hours = filteTime(hours)
      this.minutes = filteTime(minutes)
      this.seconds = filteTime(seconds)
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
  computed: {
    swiper () { // 轮播组件
      return this.$refs.mySwiper.swiper;
    }
  },
  beforeDestroy () {
    // 销毁前清除定时器
    clearInterval(this.interval);
  },
  mounted () {
    this.getListByDay()
  },
  methods: {
    goToSeckill () { // 跳转秒杀页面
      let routeUrl = this.$router.resolve({
        path: '/seckill'
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
        this.actStatus = 0;
        actTime = zeroTime + this.list[currIndex].timeLine * 3600 * 1000
      } else if (this.list[currIndex].timeLine <= nowHour) { // 活动进行中
        this.actStatus = 1;
        if (currIndex === this.list.length - 1) { // 如果是最后一个活动，直到24点结束
          actTime = zeroTime + 24 * 3600 * 1000
        } else {
          actTime = zeroTime + this.list[currIndex + 1].timeLine * 3600 * 1000
        }
      }
      this.currHour = this.list[this.currIndex].timeLine
      this.diffSeconds = Math.floor((actTime - currTime) / 1000)
      this.interval = setInterval(() => {
        this.diffSeconds--
      }, 1000)
    },
    getListByDay () { // 当天秒杀活动
      // const list = [
      //   {
      //     timeLine: 18,
      //     seckillGoodsList: [
      //       {goodsImage: 'https://lilishop-oss.oss-cn-beijing.aliyuncs.com/a9593607de404546953055f279fd5d54.png', goodsName: 'dfsdgsdf', originalPrice: 39, price: 12},
      //       {goodsImage: 'https://lilishop-oss.oss-cn-beijing.aliyuncs.com/a9593607de404546953055f279fd5d54.png', goodsName: 'dfsdgsdf', originalPrice: 39, price: 12},
      //       {goodsImage: 'https://lilishop-oss.oss-cn-beijing.aliyuncs.com/a9593607de404546953055f279fd5d54.png', goodsName: 'dfsdgsdf', originalPrice: 39, price: 12},
      //       {goodsImage: 'https://lilishop-oss.oss-cn-beijing.aliyuncs.com/a9593607de404546953055f279fd5d54.png', goodsName: 'dfsdgsdf', originalPrice: 39, price: 12},
      //       {goodsImage: 'https://lilishop-oss.oss-cn-beijing.aliyuncs.com/a9593607de404546953055f279fd5d54.png', goodsName: 'dfsdgsdf', originalPrice: 39, price: 12},
      //       {goodsImage: 'https://lilishop-oss.oss-cn-beijing.aliyuncs.com/a9593607de404546953055f279fd5d54.png', goodsName: 'dfsdgsdf', originalPrice: 39, price: 12}
      //     ]
      //   }
      // ]
      this.list = this.data.options.list
      this.goodsList = this.list[0].seckillGoodsList
      console.log( this.goodsList)
      this.countDown(this.currIndex)
    }
  }
};
</script>
<style lang="scss" scoped>
.seckill {
  width: 100%;
  height: 260px;
  display: flex;
  background-color: #eee;
  .aside {
    overflow: hidden;
    width: 190px;
    height: 100%;
    color: #fff;
    background-image: url("../../../assets/images/seckillBg.png");

    .title {
      width: 100%;
      text-align: center;
      font-size: 28px;
      margin-top: 31px;
    }

    .hour {
      margin-top: 90px;
      text-align: center;
      span {
        font-size: 18px;
      }
    }

    .count-down {
      margin: 10px 0 0 30px;
      > span {
        position: relative;
        float: left;
        width: 30px;
        height: 30px;
        text-align: center;
        background-color: #2f3430;
        margin-right: 20px;
        color: white;
        font-size: 20px;
        &::after {
          content: ":";
          display: block;
          position: absolute;
          right: -20px;
          font-weight: bolder;
          font-size: 18px;
          width: 20px;
          height: 100%;
          top: 0;
        }
      }
      > span:last-child::after {
        content: "";
      }
    }

    .act-status {
      margin: 10px 0 0 65px;
      font-size: 20px;
    }
  }

  .content {
    width: 200px;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    position: relative;
    &::after {
      content: "";
      display: block;
      position: absolute;
      top: 50%;
      right: 0;
      width: 1px;
      height: 200px;
      transform: translateY(-50%);
      background: linear-gradient(180deg, white, #eeeeee, white);
    }
    img {
      margin-top: 30px;
    }
    > div {
      width: 160px;
      margin-top: 10px;
      font-size: 12px;
      position: relative;
    }
    > div:nth-of-type(1):hover {
      color: $theme_color;
      cursor: pointer;
    }
    > div:nth-of-type(2) {
      border: 1px solid $theme_color;
      line-height: 24px;
      display: flex;
      text-align: center;

      span:nth-child(1) {
        color: #fff;
        font-size: 16px;
        width: 92px;
        background-color: $theme_color;
        position: relative;
        &::before {
          content: " ";
          width: 0;
          height: 0;
          border-color: transparent white transparent transparent;
          border-style: solid;
          border-width: 24px 8px 0 0;
          position: absolute;
          top: 0;
          left: 84px;
        }
      }

      span:nth-child(2) {
        color: #999;
        width: 66px;
        text-decoration: line-through;
      }
    }
  }
}
.swiper-container {
  height: 260px;
  width: 1000px;
  margin-left: 10px;
  background-color: #fff;
}
.swiper-button-prev, .swiper-button-next {
  background: #ccc;
  width: 25px;
  height: 35px;
  font-size: 16px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.swiper-button-prev:hover, .swiper-button-next:hover {
  background: #aaa;
}
.swiper-button-prev {
  left: 0;
  border-bottom-right-radius: 18px;
  border-top-right-radius: 18px;
  padding-right: 5px;
}
.swiper-button-next {
  right: 0;
  border-top-left-radius: 18px;
  border-bottom-left-radius: 18px;
  padding-left: 5px;
}
</style>
