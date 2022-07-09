<template>
  <div>
    <div class="wrapper" v-if="type === 'goodsDetail'">
      <div class="wr-l"><Icon size="23" type="ios-alarm-outline" /> 秒杀活动</div>
      <div class="count-down" v-if="end === ''">
      <p>倒计时：</p><span>{{ hours }}</span><span>{{ minutes }}</span><span>{{ seconds }}</span>
      </div>
      <div v-else>{{end}}</div>
    </div>
    <span v-else class="cart-promotion">
      <span v-if="end === ''">距活动结束：<span>{{ hours }}</span> : <span>{{ minutes }}</span> : <span>{{ seconds }}</span></span>
      <span v-else>活动已结束</span>
    </span>
  </div>
</template>
<script>
export default {
  props: {
    time: { // 传入的初始时间
      default: 1718977559428
    },
    type: { // 区分是在详情还是购物车调用
      default: 'goodsDetail', // 设置两个值，goodsDetail和cart，样式不同
      type: String
    }
  },
  data () {
    return {
      end: '', // 结束状态
      hours: '', // 小时
      minutes: '', // 分钟
      seconds: '', // 秒
      interval: '' // 定时器
    };
  },
  mounted () {
    this.init()
  },
  methods: {
    countDown (val) { // 倒计时方法
      function addZero (i) {
        return i < 10 ? '0' + i : i + '';
      }
      var nowtime = new Date();
      var endtime = new Date(val);
      var lefttime = parseInt((endtime.getTime() - nowtime.getTime()) / 1000);
      var h = parseInt((lefttime / (60 * 60)) % 24);
      var m = parseInt((lefttime / 60) % 60);
      var s = parseInt(lefttime % 60);
      h = addZero(h);
      m = addZero(m);
      s = addZero(s);
      this.hours = h;
      this.minutes = m
      this.seconds = s;
      if (lefttime <= 0) {
        this.end = `活动已结束`;
        clearInterval(this.interval)
      }
    },
    init () { // 初始化
      this.interval = setInterval(() => {
        this.countDown(this.time);
      }, 1000);
    }
  }
};
</script>
<style scoped lang="scss">
.cart-promotion{
  font-size: 13px;
  color: #999;
  margin-left: 10px;
}
.wrapper {
  background-image: linear-gradient(266deg, #ff0b33, #ff4257, #ff5f7c, #fa78a2);
  height: 32px;
  color: #fff;
  line-height: 32px;
  font-size: 16px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.wr-r{
  font-size: 13px;
}
.count-down {
  margin-right: -20px;
  p{
    float: left;
    line-height: 20px;
  }
  > span {
    position: relative;
    float: left;
    width: 20px;
    height: 20px;
    text-align: center;
    background-color: #2f3430;
    margin-right: 20px;
    color: white;
    font-size: 14px;
    line-height: 20px;
    &::after {
      content: ":";
      display: block;
      position: absolute;
      right: -20px;
      font-weight: bolder;
      font-size: 14px;
      width: 20px;
      height: 100%;
      top: 0;
    }
  }
  > span:last-child::after {
    content: "";
  }
}

</style>
