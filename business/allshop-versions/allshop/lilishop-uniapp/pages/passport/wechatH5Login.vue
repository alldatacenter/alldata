<template>
  <view class="container">
    <u-navbar :custom-back="back" title="公众网页登录"></u-navbar>
    <view class="wx-auth-container">
      <div class="box">
        <view class="logo-info">
          <text class="title">欢迎进入{{ projectName }}</text>
        </view>
        <view class="small-tips">
          <view>为您提供优质服务,{{ projectName }}需要获取以下信息</view>
          <view>您的公开信息（昵称、头像）</view>
        </view>
        <view class="btns">
          <button @click="getUserProfile()" class="btn-auth">使用微信授权</button>
          <div @click="backToHome" class="btn-callback">暂不登录</div>
        </view>
      </div>
    </view>
  </view>
</template>

<script>
import { whetherNavigate } from "@/utils/Foundation"; //登录跳转
import config from "@/config/config";
import api from "@/config/api.js";
export default {
  data() {
    return {
      // 授权信息展示，商城名称
      projectName: config.name,
    };
  },

  //微信小程序进入页面，先获取code，否则几率出现code和后续交互数据不对应情况
  mounted() {
    // 小程序默认分享
    uni.showShareMenu({ withShareTicket: true });
  },
  methods: {
    back() {
      whetherNavigate();
    },
    getUserProfile() {
      let code = "WECHAT";
      let buyer = api.buyer;
      window.open(buyer + `/passport/connect/connect/login/web/` + code, "_self");
    },
    backToHome() {
      uni.switchTab({
        url: `/pages/tabbar/home/index`,
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.wx-auth-container {
  width: 100%;

  margin-top: 20%;
}

.logo-info {
  display: flex;
  flex-wrap: nowrap;
  justify-content: flex-start;
  flex-direction: row;
  align-items: flex-start;
  padding: 20rpx;
  flex-direction: column;
  font-weight: bold;
}

image {
  width: 100px;
  height: 100px;
  text-align: center;
  -webkit-transform: scale(2.5);
  transform: scale(2.5);
}

text.title,
text.shop {
  display: inline-block;
  font-size: 60rpx;
  color: #333;
}

.box {
  margin: 0 32rpx;
}

/* 文字提示*/
.small-tips {
  width: 94%;
  padding: 20rpx;
  font-size: 24rpx;
  margin: 0 0 20rpx;
  color: #999;
}

.btn-auth {
  width: 92%;
  background: $light-color;
  color: #fff;
  margin: 0 auto 40rpx;
  border-radius: 100px;
  animation: mymove 5s infinite;
  -webkit-animation: mymove 5s infinite; /*Safari and Chrome*/
  animation-direction: alternate; /*轮流反向播放动画。*/
  animation-timing-function: ease-in-out; /*动画的速度曲线*/
  /* Safari 和 Chrome */
  -webkit-animation: mymove 5s infinite;
  -webkit-animation-direction: alternate; /*轮流反向播放动画。*/
  -webkit-animation-timing-function: ease-in-out; /*动画的速度曲线*/
}
.btn-callback {
  text-align: center;
  font-size: 30rpx;
  background: #ededed;
  height: 90rpx;
  line-height: 90rpx;
  border-radius: 100px;
  width: 92%;
  margin: 0 auto;
}

.btns {
  margin-top: 100rpx;
  display: flex;
  flex-direction: column;
  width: 100%;
  justify-content: center;
}

@keyframes mymove {
  0% {
    transform: scale(1); /*开始为原始大小*/
  }
  25% {
    transform: scale(1.1); /*放大1.1倍*/
  }
  50% {
    transform: scale(1);
  }
  75% {
    transform: scale(1.1);
  }
}
</style>
