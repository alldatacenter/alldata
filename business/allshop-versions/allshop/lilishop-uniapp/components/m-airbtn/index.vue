<template>
  <view>
    <!-- #ifdef H5 -->
    <view class="wrapper" v-if="!weChat" @click="openApp()">
      <!-- 左侧图标 -->
      <image class="img" :src="logo"></image>
      <view class="open">
        打开{{config.name}}
      </view>
    </view>

    <!-- #endif -->
  </view>
</template>

<script>
import config from "@/config/config";
export default {
  data() {
    return {
      config, // 设置工具类
      weChat: false,  // 是否微信浏览器，该项为true时不显示 当前整个页面
      logo: require("@/icon.png"), //显示的圆形logo
    };
  },
  mounted() {
    // #ifdef H5
    // 判断是否是微信浏览器
    var ua = navigator.userAgent.toLowerCase();
    var isWeixin = ua.indexOf("micromessenger") != -1;
    if (isWeixin) {
      this.weChat = true;
    } else {
      this.weChat = false;
    }
    // #endif
  },
  methods: {

    /**
     * 跳转到下载app页面
     */
    downloadApp() {
      setTimeout(function () {
        window.location.href = config.downloadLink;
      }, 2000);
    },

    /**
     * 打开app 仅在h5生效 使用ifream唤醒app
     */
    openApp() {
      let src;
      if (location.href) {
        src = location.href.split("/pages")[1];
      }
      let t = `${config.schemeLink}pages${src}`;

      try {
        var e = navigator.userAgent.toLowerCase(),
          n = e.match(/cpu iphone os (.*?) like mac os/);
        if (
          ((n = null !== n ? n[1].replace(/_/g, ".") : 0), parseInt(n) >= 9)
        ) {
          window.location.href = t;

          this.downloadApp();
        } else {
          var r = document.createElement("iframe");
          (r.src = t), (r.style.display = "none"), document.body.appendChild(r);

          this.downloadApp();
        }
      } catch (e) {
        window.location.href = t;
        this.downloadApp();
      }
    },
  },
};
</script>

<style scoped lang="scss">
.img {
  margin: 0 4rpx;
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  border: 5rpx solid #fff;
}

.open {
  margin: 0 10rpx;
  text-align: center;
  font-size: 26rpx;
}
.wrapper:hover {
  transform: translateX(0);
}
.wrapper {
  transform: translateX(160rpx);
  transition: 0.35s;
  align-items: center;
  justify-content: center;
  display: flex;
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
  color: #fff;
  // width: 220rpx;
  background: $light-color;
  position: fixed;
  top: 25%;
  right: 0;
  height: 60rpx;
  z-index: 9;
}
</style>
