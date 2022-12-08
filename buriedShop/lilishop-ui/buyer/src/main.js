import Vue from "vue";
import App from "./App";
import router from "./router";
import ViewUI from "view-design";
import "./assets/styles/theme.less";
// import './assets/iconfont/iconfont.css';
import * as filters from "./plugins/filters";
import store from "@/vuex/store";
import storage from "@/plugins/storage";
// 全局引入封装组件
import { InstallAll } from "@/components/global.js";

const { aMapSecurityJsCode, title } = require("@/config");
Vue.use(ViewUI);
Vue.use(InstallAll);
Vue.config.productionTip = false;

Object.keys(filters).forEach((key) => {
  Vue.filter(key, filters[key]);
});

// 高德安全密钥
if (aMapSecurityJsCode) {
  window._AMapSecurityConfig = {
    securityJsCode: aMapSecurityJsCode,
  };
}

router.beforeEach((to, from, next) => {
  ViewUI.LoadingBar.start();
  window.document.title = to.meta.title === undefined ? title : to.meta.title;
  next();
});

router.afterEach((route) => {
  ViewUI.LoadingBar.finish();
});

Vue.prototype.linkTo = function (url) {
  if (url.substr(0, 1) === "/") {
    // 非外部链接，没有origin，只有路由地址
    if (router.mode === "hash") {
      window.open(location.origin + "/#" + url, "_blank");
    } else {
      window.open(location.origin + url, "_blank");
    }
  } else {
    // 外部链接，完整的url地址
    window.open(url, "_blank");
  }
};
// 联系客服
Vue.prototype.connectCs = function (
  sign = "37ef9b97807d03c6741298ed4eb5b536d2d238e08a3c00fb01fe48f03a569974c99ad767e72c04b3165ef29aca2c488b505fe4ca"
) {
  const url = "https://yzf.qq.com/xv/web/static/chat/index.html?sign=" + sign;
  window.open(url, "_blank");
};
Vue.prototype.Cookies = storage;
/* eslint-disable no-new */
new Vue({
  router,
  store,
  render: (h) => h(App),
}).$mount("#app");
