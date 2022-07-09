<template>
  <view class="container">
    <u-navbar :custom-back="back" title="小程序登录"></u-navbar>
    <u-modal v-model="phoneAuthPopup" :mask-close-able="true" :title="projectName+'商城'" :show-confirm-button="false">
      <div class="tips">
        为了更好地用户体验，需要您授权手机号
      </div>
      <button class="register" type="primary" open-type="getPhoneNumber" @getphonenumber="getPhoneNumber">
        去授权
      </button>
    </u-modal>
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
          <button type="primary" bindtap="getUserProfile" @click="getUserProfile()" class="btn-auth">使用微信授权</button>
          <div @click="backToHome" class="btn-callback">暂不登录</div>
        </view>
      </div>
    </view>
  </view>
</template>

<script>
import { mpAutoLogin } from "@/api/connect.js";

import { whetherNavigate } from "@/utils/Foundation"; //登录跳转
import { getUserInfo } from "@/api/members";
import storage from "@/utils/storage.js";
import config from '@/config/config'
export default {
  data() {
    return {
      // 是否展示手机号码授权弹窗，默认第一步不展示，要先获取用户基础信息
      phoneAuthPopup: false,
      // 授权信息展示，商城名称
      projectName: config.name,
      //微信返回信息，用于揭秘信息，获取sessionkey
      code: "",
      //微信昵称
      nickName: "",
      //微信头像
      image: "",
    };
  },

  //微信小程序进入页面，先获取code，否则几率出现code和后续交互数据不对应情况
  mounted() {
    // 小程序默认分享
    uni.showShareMenu({ withShareTicket: true });

    let that = this;
    //获取code
    uni.login({
      success: (res) => {
        that.code = res.code;
      },
    });
  },
  methods: {
    /**
     * TODO 此方法不一定是最优解，如果有更好的办法请在  https://gitee.com/beijing_hongye_huicheng/lilishop/issues 中提出
     * 小程序返回bug
     * 1.介于微信登录是在login.vue的基础上作为判断跳转来
     * 所以在页面栈中会自动记录回退路径，所以导致每次微信小程序点击回退就会自动返回login页面
     * 当然login页面的判断就是 没有登录就会跳转到微信小程序页面 导致了无法回退到之前页面
     * 2.解决方法： 尝试在回退的时候判断地址，让回退多一级这样就避免了
     */

    back() {
      whetherNavigate("wx");
    },
    backToHome() {
      uni.switchTab({
        url: `/pages/tabbar/home/index`,
      });
    },
    //获取用户信息
    getUserProfile(e) {
      let that = this;
      // 推荐使用wx.getUserProfile获取用户信息，开发者每次通过该接口获取用户个人信息均需用户确认
      uni.getUserProfile({
        desc: "用于完善会员资料", // 声明获取用户个人信息后的用途，后续会展示在弹窗中，请谨慎填写
        success: (res) => {
          that.nickName = res.userInfo.nickName;
          that.image = res.userInfo.avatarUrl;
          //展示手机号获取授权
          this.phoneAuthPopup = true;
        },
        fail: (res) => {
          that.nickName = "微信用户";
          that.image =
            "https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132";
          //展示手机号获取授权
          this.phoneAuthPopup = true;
        },
      });
    },
    //获取手机号授权
    getPhoneNumber(e) {
      let iv = e.detail.iv;
      let encryptedData = e.detail.encryptedData;
      if (!e.detail.encryptedData) {
        uni.showToast({
          title: "请授予手机号码权限，手机号码会和会员系统用户绑定！",
          icon: "none",
        });
        return;
      }

      let code = this.code;
      let image = this.image;
      let nickName = this.nickName;
      mpAutoLogin({
        encryptedData,
        iv,
        code,
        image,
        nickName,
      }).then((res) => {
        storage.setAccessToken(res.data.result.accessToken);
        storage.setRefreshToken(res.data.result.refreshToken);
        // 登录成功
        uni.showToast({
          title: "登录成功!",
          icon: "none",
        });
        //获取用户信息
        getUserInfo().then((user) => {
          storage.setUserInfo(user.data.result);
          storage.setHasLogin(true);

          uni.navigateBack({
            delta: 1,
          });
        });
      });
    },
  },
};
</script>
<style lang="scss" scoped>
/*微信授权*/
page {
  background-color: #ffffff;
}

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

.logo-info-img {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  border: none;
}

text.title,
text.shop {
  display: inline-block;
  font-size: 60rpx;
  color: #333;
}

text.shop {
  display: inline-block;
  font-size: 55rpx;
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

.auth-button {
  padding: 10px 20px;
  width: calc(100% - 20 * 4rpx);
}

.tips {
  width: 80%;
  text-align: left;
  margin: 6% 10%;
  margin-top: 48rpx;
  line-height: 1.75;
}

.register {
  color: $weChat-color !important;
  border: none !important;
  background: #fff !important;
}

.btn-auth {
  width: 92%;
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
