<template>
  <div>
    <Row class="header">
      <img class="logo" :src="storeSideLogo" />
    </Row>
  </div>
</template>

<script>
import { getBaseSite } from "@/api/common.js";
export default {
  name: "header",
  data(){
    return{
      storeSideLogo: require('@/assets/logo.png')
    }
  },
  methods: {
    init() {
      if (
        !localStorage.getItem("sellersiteName") ||
        !localStorage.getItem("sellerlogoImg") ||
        !localStorage.getItem("seller_expiration_time")
      ) {
        this.getSite();
      } else {
        // 如果缓存过期，则获取最新的信息
        if (new Date() > localStorage.getItem("seller_expiration_time")) {
          this.getSite();
          return;
        } else {
          this.storeSideLogo = localStorage.getItem("sellerlogoImg");
          window.document.title = localStorage.getItem("sellersiteName");
          //动态获取icon
          let link =
            document.querySelector("link[rel*='icon']") ||
            document.createElement("link");
          link.type = "image/x-icon";
          link.href = localStorage.getItem("sellerlogoImg");
          link.rel = "shortcut icon";
          document.getElementsByTagName("head")[0].appendChild(link);
        }
      }
    },
    getSite() {
      //获取基本站点信息
      getBaseSite().then((res) => {
        if (res.success && res.result.settingValue) {
          let data = JSON.parse(res.result.settingValue);
          // 过期时间
          var expirationTime = new Date().setHours(new Date().getHours() + 1);
          // 存放过期时间
          localStorage.setItem("seller_expiration_time", expirationTime);
          // 存放信息
          localStorage.setItem("sellersiteName", data.siteName);
          localStorage.setItem("sellerlogoImg", data.storeSideLogo);
        console.log(data,'datadadada')
          window.document.title = data.siteName;
          //动态获取icon
          let link =
            document.querySelector("link[rel*='icon']") ||
            document.createElement("link");
          link.type = "image/x-icon";
          link.href = data.storeSideLogo;
          link.rel = "shortcut icon";
          document.getElementsByTagName("head")[0].appendChild(link);
        }
      });
    },
  },

  mounted() {
    this.init();
  },
};
</script>

<style lang="scss" scoped>
.header {
  margin-bottom: 6vh;
  align-items: center;
  display: flex;
  justify-content: center !important;
}
.logo {
  width: 440px;
  height: 158px;
}
</style>
