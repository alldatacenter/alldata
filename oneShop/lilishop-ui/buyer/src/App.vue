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
    this.init();
  },
  methods:{
    init(){
      if(!storage.getItem("siteName")||!storage.getItem("logoImg")||!storage.getItem("sitelogo_expiration_time")) {
        this.getSite();
      }else{
        // 如果缓存过期，则获取最新的信息
        if (new Date() > storage.getItem("sitelogo_expiration_time")) {
          this.getSite();
          return;
        }else{
          window.document.title = storage.getItem("siteName");
          //动态获取icon
          let link =document.querySelector("link[rel*='icon']") ||document.createElement("link");
            link.type = "image/x-icon";
            link.href = storage.getItem("logoImg");
            link.rel = "shortcut icon";
            document.getElementsByTagName("head")[0].appendChild(link);
        }
      }
      
    },
    getSite(){
      //获取基本站点信息
      getBaseSite().then((res) => {
        if (res.success && res.result.settingValue) {
          let data = JSON.parse(res.result.settingValue);
          // 过期时间
          var expirationTime = new Date().setHours(new Date().getHours() + 1);
          // 存放过期时间
          storage.setItem("sitelogo_expiration_time", expirationTime);
          // 存放信息
          storage.setItem('siteName', data.siteName);
          storage.setItem('logoImg', data.buyerSideLogo);

          window.document.title = data.siteName;
          //动态获取icon
          let link =document.querySelector("link[rel*='icon']") ||document.createElement("link");
          link.type = "image/x-icon";
          link.href = data.buyerSideLogo;
          link.rel = "shortcut icon";
          document.getElementsByTagName("head")[0].appendChild(link);
        }
      });
    }
  }
};
</script>
<style lang="scss">
#app {
  @include background_color($light_background_color);
}
</style>
