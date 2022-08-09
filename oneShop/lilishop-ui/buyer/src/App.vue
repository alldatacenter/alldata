<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
import storage from "@/plugins/storage";
import { getBaseSite } from "@/api/common.js";
export default {
  name: "App",

  mounted() {
    //获取基本站点信息
    getBaseSite().then((res) => {
      if (res.success && res.result.settingValue) {
        let data = JSON.parse(res.result.settingValue);
        this.$store.commit("SET_LOGOIMG", data.buyerSideLogo);
        this.$store.commit("SET_SITENAME", data.siteName);
        storage.setItem("siteName", data.siteName);
        window.document.title = data.siteName;
        //动态获取icon
        let link =
          document.querySelector("link[rel*='icon']") ||
          document.createElement("link");
        link.type = "image/x-icon";
        link.href = data.buyerSideLogo;
        link.rel = "shortcut icon";
        document.getElementsByTagName("head")[0].appendChild(link);
      }
    });
  },
};
</script>
<style lang="scss">
#app {
  @include background_color($light_background_color);
}
</style>
