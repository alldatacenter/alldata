<template>
  <div class="seckill">
    <div class="desc">秒杀商品需要在促销活动中添加商品，有商品时才会在首页展示</div>
    <div class="aside">
      <div class="title">{{ actName }}</div>
      <div class="hour">
        <span>{{ currHour }}:00</span>点场 倒计时
      </div>
      <div class="count-down" v-if="actStatus === 1">
        <span>{{ hours }}</span
        ><span>{{ minutes }}</span
        ><span>{{ seconds }}</span>
      </div>
      <div class="act-status" v-else>
        {{ actStatus == 0 ? "未开始" : "已结束" }}
      </div>
    </div>
    <div class="section">
      <swiper ref="mySwiper" :options="swiperOptions">
        <swiper-slide
          v-for="(item, index) in options.list[0].goodsList"
          :key="index"
        >
          <div class="content">
            <img :src="item.img" width="140" height="140" :alt="item.name" />
            <div class="ellipsis">{{ item.name }}</div>
            <div>
              <span>{{ item.price | unitPrice("￥") }}</span>
              <span>{{ item.originalPrice | unitPrice("￥") }}</span>
            </div>
          </div>
        </swiper-slide>
      </swiper>
    </div>
  </div>
</template>
<script>
import { Swiper, SwiperSlide, directive } from "vue-awesome-swiper";
import "swiper/swiper-bundle.css";
export default {
  components: {
    Swiper,
    SwiperSlide,
  },
  directives: {
    swiper: directive,
  },
  props: {
    data: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      options: this.data.options, // 当前数据
      actStatus: 0, // 0 未开始  1 进行中   2 已结束
      actName: "限时秒杀",
      currHour: "00", // 当前秒杀场
      diffSeconds: 0, // 倒地时
      hours: "00", // 小时
      minutes: "00", // 分钟
      seconds: "00", // 秒
      interval: undefined, // 定时器
      swiperOptions: {
        // 轮播图参数
        slidesPerView: 5,
        autoplay: true,
        loop: true,
      },
    };
  },
  watch: {
    diffSeconds(val) {
      const hours = Math.floor(val / 3600);
      // 当前秒数 / 60，向下取整
      // 获取到所有分钟数 3600 / 60 = 60分钟
      // 对60取模，超过小时数的分钟数
      const minutes = Math.floor(val / 60) % 60;
      // 当前的秒数 % 60，获取到 超过小时数、分钟数的秒数（秒数）
      const seconds = val % 60;
      this.hours = hours < 10 ? "0" + hours : hours;
      this.minutes = minutes < 10 ? "0" + minutes : minutes;
      this.seconds = seconds < 10 ? "0" + seconds : seconds;

      if (val === 0) {
        clearInterval(this.interval);
        this.hours = 0;
        this.minutes = 0;
        this.seconds = 0;
        this.countDown(this.options.list);
      }
    },
  },
  mounted() {
    this.countDown(this.options.list);
  },
  beforeDestroy() {
    clearInterval(this.interval);
  },
  methods: {
    // 倒计时
    countDown(list) {
      /**
       * 默认倒计时两小时
       * 如果没有开始，则显示未开始
       * 进行中显示倒计时
       * 今天的秒杀结束则显示已结束
       */
      let nowHour = new Date().getHours();
      if (nowHour < Number(list[0].time)) {
        // 活动未开始
        this.currHour = list[0].time;
        this.actStatus = 0;
      } else if (nowHour >= Number(list[list.length - 1].time + 2)) {
        // 活动已结束
        this.actStatus = 2;
        this.currHour = list[list.length - 1].time;
      } else {
        // 活动进行中
        this.actStatus = 1;
        for (let i = 0; i < list.length; i++) {
          if (nowHour == Number(list[i].time)) {
            this.currHour = list[i].time;
          }
          if (
            nowHour > Number(list[i].time) &&
            nowHour < Number(list[i].time + 2)
          ) {
            this.currHour = list[i].time;
          }
        }
        // 当前0点时间戳
        let zeroTime = new Date(new Date().toLocaleDateString()).getTime();
        // 活动倒计时
        this.diffSeconds = Math.floor(
          (zeroTime +
            3600 * 1000 * (this.currHour + 2) -
            new Date().getTime()) /
            1000
        );
        const that = this;
        this.interval = setInterval(() => {
          this.diffSeconds--;
        }, 1000);
      }
    },
  },
};
</script>
<style lang="scss" scoped>
.seckill {
  width: 100%;
  height: 260px;
  display: flex;
  position: relative;
  .desc{
    position: absolute;
    color: $theme_color;
    left: 200px;
    top: 5px;
  }
  .aside {
    overflow: hidden;
    width: 190px;
    height: 100%;
    color: #fff;
    background-image: url("../../../assets/seckillBg.png");

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

  .section {
    width: 1000px;
    // background: #efefef;
    .swiper-slide {
      height: 260px;
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
  }
}
</style>