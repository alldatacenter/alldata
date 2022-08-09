<template>
  <view class="sign-in">
    <view class="date-card">
      <div class="box">
        <div class="circle-box">
          <div class="cricle" @click="signIn()">
            <span v-if="!ifSign" :class="{ active: signFlag || ifSign }">签到</span>
            <span v-else :class="{ active: signFlag || ifSign }"
              :style="ifSign ? 'transform: rotateY(0deg);' : ''">已签</span>
          </div>
        </div>
        <text class="tips">坚持每天连续签到可以获多重奖励哦</text>
      </div>
    </view>
    <div class="date-card">
      <view class="date-con">
        <view class="date-tit">
          <div class="current-month">
            <div class="day">每日记录<span> ({{ currentMonth }})</span></div>
          </div>
        </view>
        <view class="week">
          <text v-for="item in weekArr" :key="item.id">{{ item }}</text>
        </view>
        <view class="date" v-for="obj in dataObj" :key="obj.id">
          <view class="item" v-for="item in obj" :key="item.id" :class="item == '' ? 'hide' : ''"
            :animation="item == currentDay ? animationData : ''">
            <view class="just" :class="signArr.indexOf(item) != -1 ? 'active' : ''">
              <view class="top">{{ item }} </view>
              <view class="bottom">
                <u-icon name="error" v-if="item <= currentDay" color="#999"></u-icon>
              </view>
            </view>
            <view class="back" :class="signArr.indexOf(item) != -1 ? 'active' : ''" :style="
                signArr.indexOf(item) != -1 && ifSign
                  ? 'transform: rotateY(0deg);'
                  : signArr.indexOf(item) != -1 && item != currentDay
                  ? 'transform: rotateY(0deg);'
                  : ''
              ">
              <view class="top">{{ item }}</view>
              <view class="bottom">
                <u-icon name="checkmark" :color="aiderLightColor"></u-icon>
              </view>
            </view>
          </view>
        </view>
      </view>
    </div>
    <view class="mask" :class="{ show: maskFlag, trans: transFlag }" ref="mask">
      <view class="mask-header">
        <text class="close"></text>
        <text>签到成功</text>
        <text class="close" @click="close">×</text>
      </view>
      <view class="mask-con">
        <u-icon size="120" style="margin: 50rpx 0" :color="aiderLightColor" name="checkmark"></u-icon>
        <text class="text">连续签到可获得额外奖励哦！</text>
      </view>
    </view>
  </view>
</template>

<script>
import { sign, signTime } from "@/api/point.js";
export default {
  data() {
    return {
      aiderLightColor:this.$aiderLightColor,
      signFlag: false,
      animationData: {},
      maskFlag: false, //
      transFlag: false, //动画
      weekArr: ["日", "一", "二", "三", "四", "五", "六"], //周数组
      dateArr: [], //每个月的天数
      monthArr: [
        //实例化每个月
        "1月",
        "2月",
        "3月",
        "4月",
        "5月",
        "6月",
        "7月",
        "8月",
        "9月",
        "10月",
        "11月",
        "12月",
      ], //今天一个月英文
      currentMonth: "", //当月
      currentMonthIndex: "", //当月
      currentYear: "", //今年
      currentDay: "", //今天
      currentWeek: "", //获取当月一号是周几
      dataObj: [], //一个月有多少天这个获取
      signArr: [], //本月签到过的天数 该参数用于请求接口后获取当月都哪天签到了
      signAll: [], //所有签到数据
      ifSign: false, //今天是否签到
    };
  },
  async onLoad() {
    //获取签到数据
    var response = await signTime(
      new Date().getFullYear() + "" + this.makeUp(new Date().getMonth() + 1)
    );
    this.signAll = response.data.result;
    //获取展示数据
    this.getDate();
  },
  methods: {
    /**
     * 补0
     */
    makeUp(val) {
      if (val >= 10) {
        return val;
      } else {
        return "0" + val;
      }
    },

    /**
     * 点击签到
     */
    async signIn() {
      await sign().then((response) => {
        if (this.ifSign) return;
        if (this.signFlag) return;
        if (response.data.code != 200) {
          uni.showToast({
            title: response.data.message,
            duration: 2000,
            icon: "none",
          });

          return false;
        }
        var that = this;
        var animation = uni.createAnimation({
          duration: 200,
          timingFunction: "linear",
        });
        this.signArr.push(this.currentDay);
        this.animation = animation;
        animation.rotateY(0).step();
        this.animationData = animation.export();

        setTimeout(
          function () {
            that.signFlag = true;
            this.maskFlag = true;
            this.ifSign = !this.ifSign;
            animation.rotateY(0).step();
            this.animationData = animation.export();
          }.bind(this),
          200
        );
      });
    },

    /**
     * 签到成功后关闭弹窗
     */
    close() {
      var that = this;
      this.maskFlag = false;
      this.transFlag = true;
      setTimeout(() => {
        that.transFlag = false;
      }, 500);
    },

    /**
     * 获取今天时间
     *
     */
    getDate() {
      var date = new Date(),
        index = date.getMonth(),
        curDay = null;
      this.currentYear = date.getFullYear();
      this.currentMonth = this.monthArr[index];
      this.currentMonthIndex = index + 1;
      this.currentDay = date.getDate();
      if (this.currentDay == this.signArr[this.signArr.length - 1]) {
        this.ifSign = true;
      }
      curDay = this.getWeekByDay(this.currentYear + "-" + (index + 1) + "-1");
      this.getMonthDays(index, curDay);
      this.curentSignData();
    },

    /**
     * 获取当前已经签到的时间
     */
    curentSignData() {
      var date = new Date(),
        index = date.getMonth(),
        curDay = null;
      this.signArr = [];
      for (var i = 0; i < this.signAll.length; i++) {
        var item = this.signAll[i];
        item.createTime = item.createTime.split(" ")[0];
        var itemVal = item.createTime.split("-");
        if (
          Number(itemVal[0]) === Number(this.currentYear) &&
          Number(itemVal[1]) === Number(this.currentMonthIndex)
        ) {
          this.signArr.push(Number(itemVal[2]));
        }
        if (
          Number(itemVal[0]) === Number(date.getFullYear()) &&
          Number(itemVal[1]) === Number(index + 1) &&
          Number(itemVal[2]) === Number(date.getDate())
        ) {
          this.ifSign = true;
        }
      }
    },

    /**
     *  循环出当前月份的时间
     *  例子：
     *  "","","","","","",1,
     *  2 ,3 ,4 ,5 ,6 ,7 ,8,
     *  ...依次向下排
     */
    getMonthDays(index, day) {
      //day 当月1号是周几
      this.dateArr = [];
      this.dataObj = [];
      for (var i = 0; i < day; i++) {
        this.dateArr.push("");
      }
      if (
        index == 0 ||
        index == 2 ||
        index == 4 ||
        index == 6 ||
        index == 7 ||
        index == 9 ||
        index == 11
      ) {
        for (let i = 1; i < 32; i++) {
          this.dateArr.push(i);
        }
      }
      if (index == 3 || index == 5 || index == 8 || index == 10) {
        for (let i = 1; i < 31; i++) {
          this.dateArr.push(i);
        }
      }
      if (index == 1) {
        if (
          (this.currentYear % 4 == 0 && this.currentYear % 100 != 0) ||
          this.currentYear % 400 == 0
        ) {
          for (let i = 1; i < 30; i++) {
            this.dateArr.push(i);
          }
        } else {
          for (let i = 1; i < 29; i++) {
            this.dateArr.push(i);
          }
        }
      }
      for (var y = 0; y < 10; y++) {
        if (this.dateArr.length > 7) {
          this.dataObj.push(this.dateArr.splice(0, 7));
        } else {
          for (let i = 0; i < 7 - this.dateArr.length; i++) {
            this.dateArr.push("");
          }
        }
      }
      this.dataObj.push(this.dateArr);
    },

    /**
     * 获取当前月份有几周
     */
    getWeekByDay(dayValue) {
      var day = new Date(Date.parse(dayValue.replace(/-/g, "/"))).getDay(); //将日期值格式化
      return day;
    },
  },
};
</script>
<style scoped>
page {
  background: #f7f7f7;
}
</style>
<style lang="scss" scoped>
.date-card {
  padding: 0 32rpx;
  margin: 32rpx 0;
  box-shadow: 0 4rpx 24rpx 0 rgba($color: #f6f6f6, $alpha: 1);
}
.tips {
  margin-top: 54rpx;
  color: #999;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.circle-box {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: $aider-light-color;
  box-shadow: 0 4rpx 24rpx 0 rgba($color: $aider-light-color, $alpha: 1);
  display: flex;
  justify-content: center; //这个是X轴居中
  align-items: center; //这个是 Y轴居中
 
}

.cricle {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: $aider-light-color;
  text-align: center;
  line-height: 160rpx;
  color: #fff;
  font-size: 40rpx;
}
.current-month {
  width: 100%;
  margin: 20rpx 0;
}
.box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding:64rpx 32rpx;
  background: #fff;
  border-radius: 20rpx;
}
.sign-in {
  color: #333;
  .date-con {
    background: #fff;
    min-height: 730rpx;
    border-radius: 20rpx;
    padding: 0 28rpx;
  }
  .day {
    font-size: 36rpx;

    > span {
      font-size: 30rpx;
      color: #999;
      margin-left: 20rpx;
    }
  }
  .date-tit {
    display: flex;
    justify-content: space-between;
    margin: 0 0 30rpx 0;
  }

  .week {
    display: flex;
    justify-content: space-between;
    color: #a6a6a6;
    font-size: 26rpx;

    text {
      width: 66rpx;
      text-align: center;
    }
  }

  .date {
    margin: 10rpx 0 36rpx;
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;

    .item {
      width: 66rpx;
      height: 80rpx;
      border-radius: 5px;
      overflow: hidden;

      position: relative;

      &.hide {
        opacity: 0;
      }

      .just,
      .back {
        display: flex;
        flex-direction: column;
        width: 100%;
        height: 100%;
        position: absolute;

        .top {
          position: relative;
          flex: 1;
          text-align: center;
          line-height: 40rpx;

          &:before {
            content: "";
            width: 40rpx;
            height: 40rpx;
            position: absolute;
            left: 50%;
            transform: translateX(-50%);
            top: 15rpx;
          }
        }
      }

      .just {
        &.active {
          display: none;
        }
      }
      .back {
        display: none;
        &.active {
          display: flex;
        }

        .top {
          color: $aider-light-color;
        }
      }

      .bottom {
        color: #999;
        font-size: 20rpx;
        height: 20rpx;
        line-height: 20rpx;
        text-align: center;
      }
    }
  }
  .mask {
    position: fixed;
    top: 0;
    bottom: 0;
    left: -100%;
    right: 100%;
    background: rgba(0, 0, 0, 0.2);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    opacity: 0;
    transition: opacity 0.5s;

    &.show {
      opacity: 1;
      left: 0;
      right: 0;
    }

    &.trans {
      left: 0;
      right: 0;
    }

    .mask-header {
      width: 540rpx;
      height: 130rpx;
      line-height: 130rpx;
      background: $aider-light-color;
      color: #fff;
      font-size: 40rpx;
      font-weight: 500;
      display: flex;
      justify-content: space-between;

      .close {
        width: 60rpx;
        font-size: 66rpx;
        font-weight: 400;
        line-height: 60rpx;
      }
    }

    .mask-con {
      width: 540rpx;
      height: 380rpx;
      background: #fff;
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #999;
      font-size: 24rpx;
      border-radius: 0 0 9px 9px;

      .keep-sign {
        font-size: 30rpx;
        margin-top: 30rpx;

        text {
          font-size: 46rpx;
          font-weight: 500;
          color: #999;
          padding: 0 6rpx 0 8rpx;
        }
      }

      .mark {
        // flex: 1;
        display: flex;
        align-items: flex-end;
        position: relative;
        margin-bottom: 16rpx;

        text {
          margin-left: 4rpx;
          color: #999;
        }
      }

      .text {
        color: #6f6f6f;
        height: 90rpx;
      }
    }
  }
}
</style>
