<template>
  <div>
    <Row class="header">
      <img :src="domainLogo" class="logo" width="220px" />
    </Row>
  </div>
</template>

<script>
import { getBaseSite } from "@/api/common.js";
export default {
  data() {
    return {
      domainLogo: require("@/assets/logo.png"),
    };
  },
  methods: {
    init() {
      if (
        !localStorage.getItem("icon") ||
        !localStorage.getItem("title") ||
        !localStorage.getItem("icontitle_expiration_time")
      ) {
        this.getSite();
      } else {
        // 如果缓存过期，则获取最新的信息
        if (new Date() > localStorage.getItem("icontitle_expiration_time")) {
          this.getSite();
          return;
        } else {
          this.domainLogo = localStorage.getItem("icon");
          let link =
            document.querySelector("link[rel*='icon']") ||
            document.createElement("link");
          link.type = "image/x-icon";
          link.href = localStorage.getItem("icon");
          link.rel = "shortcut icon";
          document.getElementsByTagName("head")[0].appendChild(link);
          window.document.title = localStorage.getItem("title") + " - 运营后台";
        }
      }
    },
    getSite() {
      //获取domainLogo
      getBaseSite().then((res) => {
        const { domainLogo, siteName } = JSON.parse(res.result.settingValue);
        this.domainLogo = domainLogo;
        // 过期时间
        var expirationTime = new Date().setHours(new Date().getHours() + 1);
        // 存放过期时间
        localStorage.setItem("icontitle_expiration_time", expirationTime);
        // 存放信息
        localStorage.setItem("icon", domainLogo);
        localStorage.setItem("title", siteName);
        let link =
          document.querySelector("link[rel*='icon']") ||
          document.createElement("link");
        link.type = "image/x-icon";
        link.href = domainLogo;
        link.rel = "shortcut icon";
        document.getElementsByTagName("head")[0].appendChild(link);
        window.document.title = siteName + " - 运营后台";
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
  text-align: center;
  display: flex;
  justify-content: center !important;
}
.logo {
  width: 440px;
  height: 158px;
}
</style>
