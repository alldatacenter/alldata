<template>
  <view class="web-view">
    <web-view :webview-styles="webviewStyles" :src="src"></web-view>
  </view>
</template>

<script>
import configs from "@/config/config";
import storage from "@/utils/storage";
export default {
  data() {
    return {
      configs,
      storage,
      webviewStyles: {
        progress: {
          color: this.$lightColor,
        },
      },
      src: "",
    };
  },
  onLoad(params) {
    params.IM ? (this.src = `${configs.imWebSrc}?token=${storage.getAccessToken()}&id=${params.IM}`): (this.src = decodeURIComponent(params.src));
    console.log(this.src);
  },
};
</script>

<style lang="scss" scoped>
.web-view {
  /deep/ .web-view {
    padding: 0;
    margin: 0;
  }
}
</style>
