
<template>
  <view class="edition-intro">
    <image :src="config.logo" class="logo" />
    <h1> {{config.name}}</h1>
    <view class='version'>
      <!-- #ifdef APP-PLUS -->
      Version {{localVersion.version}}
      <!-- #endif -->
    </view>

    <!-- {{localVersion}} -->
    <u-cell-group class="cell" :border="false">
      <!--  #ifdef APP-PLUS -->
      <u-cell-item v-if="IosWhether" @click="checkStar" title="去评分"></u-cell-item>
      <u-cell-item title="功能介绍" @click="navigateTo('/pages/mine/set/versionFunctionList')"></u-cell-item>
      <u-cell-item title="检查更新" @click="checkUpdate"></u-cell-item>
      <!--  #endif -->
      <u-cell-item title="证照信息" @click="navigateTo('/pages/mine/help/tips?type=message')"></u-cell-item>
      <u-cell-item title="服务协议" @click="navigateTo('/pages/mine/help/tips?type=user')"></u-cell-item>
      <u-cell-item title="隐私协议" @click="navigateTo('/pages/mine/help/tips?type=privacy')"></u-cell-item>
      <u-cell-item title="关于我们" :border-bottom="false" @click="navigateTo('/pages/mine/help/tips?type=about')"></u-cell-item>

    </u-cell-group>

    <view class="intro">
      <view>{{config.customerServiceMobile ? `客服热线：${config.customerServiceMobile}` :  ``}}</view>
      <view style="margin:20rpx 0 0 0;">{{config.customerServiceEmail ? `客服邮箱：${config.customerServiceEmail}` :  ``}}</view>

      <view>
        <view style="margin:20rpx 0; color:#003a8c;" @click="navigateTo('/pages/mine/help/tips?type=user')">《{{config.name}}用户协议》</view>
        <view>CopyRight @{{config.name}} </view>
      </view>
    </view>
  </view>
</template>

<script>
import APPUpdate from "@/plugins/APPUpdate";
import config from "@/config/config";
import { getAppVersion } from "@/api/message.js";
export default {
  data() {
    return {
      config,
      IosWhether: false, //是否是ios
      editionHistory: [], //版本历史
      versionData: {}, //版本信息
      localVersion: "", //当前版本信息
      params: {
        pageNumber: 1,
        pageSize: 5,
      },
    };
  },
  onLoad() {
    // #ifdef APP-PLUS
    const platform = uni.getSystemInfoSync().platform;
    /**
     * 获取是否是安卓
     */
    if (platform === "android") {
      this.params.type = 0;
    } else {
      this.IosWhether = true;
      this.params.type = 1;
    }
    this.getVersion(platform);

    plus.runtime.getProperty(plus.runtime.appid, (inf) => {
      this.localVersion = {
        versionCode: inf.version.replace(/\./g, ""),
        version: inf.version,
      };
    });
    // #endif
  },

  methods: {
    async getVersion(platform) {
      let type;
      platform == "android" ? (type = "ANDROID") : (type = "IOS");

      let res = await getAppVersion(type);
      if (res.data.success) {
        this.versionData = res.data.result;
      }
    },

    navigateTo(url) {
      uni.navigateTo({
        url,
      });
    },

    /**
     * ios点击评分
     */
    checkStar() {
      plus.runtime.launchApplication({
        action: `itms-apps://itunes.apple.com/app/${config.iosAppId}?action=write-review`,
      });
    },

    /**
     * 检查更新
     */
    checkUpdate() {
      if (
        this.versionData.version.replace(/\./g, "") <
        this.localVersion.versionCode
      ) {
        APPUpdate();
      } else {
        uni.showToast({
          title: "当前版本已是最新版",
          duration: 2000,
          icon: "none",
        });
      }
    },
  },
};
</script>

<style lang="scss" scoped>
page {
  background: #fff !important;
}
.cell {
  width: 90%;
  margin: 0 auto;
}
.edition-intro {
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  > h1 {
    margin: 20rpx 0 20rpx 0;
    letter-spacing: 2rpx;
  }
  > .version {
    font-size: 30rpx;
    margin-bottom: 100rpx;
  }
}
.intro {
  margin-top: 100rpx;
  font-size: 24rpx;
  letter-spacing: 2rpx;
}
.logo {
  width: 200rpx;
  height: 200rpx;
}
</style>
