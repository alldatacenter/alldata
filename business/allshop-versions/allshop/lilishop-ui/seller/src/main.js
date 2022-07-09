import Vue from "vue";
import ViewUI from "view-design";
import "./styles/theme.less";

import "core-js/stable";
import vueQr from "vue-qr";

import App from "./App";
import { router } from "./router/index";
import store from "./store";
import {
  getRequest,
  postRequest,
  putRequest,
  deleteRequest,
  importRequest,
  uploadFileRequest
} from "@/libs/axios";
import { setStore, getStore, removeStore } from "@/libs/storage";

import i18nBox from '@/views/lili-components/i18n-translate'
import util from "@/libs/util";


import VueLazyload from "vue-lazyload";

import * as filters from "@/utils/filters"; // global filter

import { md5 } from "@/utils/md5.js";
const { aMapSecurityJsCode } = require("@/config");
// 高德安全密钥
if (aMapSecurityJsCode) {
  window._AMapSecurityConfig = {
    securityJsCode: aMapSecurityJsCode,
  };
}
Vue.config.devtools = true;
Vue.config.productionTip = false;
Vue.use(VueLazyload, {
  error: require("./assets/img-error.png"),
  loading: require("./assets/loading2.gif")
});

Vue.use(ViewUI);
Vue.component('i18nBox',i18nBox)
Vue.component("vue-qr", vueQr); //此处将vue-qr添加为全局组件

// 挂载全局使用的方法
Vue.prototype.getRequest = getRequest;
Vue.prototype.postRequest = postRequest;
Vue.prototype.putRequest = putRequest;
Vue.prototype.deleteRequest = deleteRequest;
Vue.prototype.importRequest = importRequest;
Vue.prototype.uploadFileRequest = uploadFileRequest;
Vue.prototype.setStore = setStore;
Vue.prototype.getStore = getStore;
Vue.prototype.removeStore = removeStore;
Vue.prototype.md5 = md5;
const PC_URL = BASE.PC_URL; // 跳转买家端地址 pc端
const WAP_URL = BASE.WAP_URL; // 跳转买家端地址 wap端
Vue.prototype.linkTo = function(goodsId, skuId) {
  // 跳转买家端商品
  window.open(
    `${PC_URL}/goodsDetail?skuId=${skuId}&goodsId=${goodsId}`,
    "_blank"
  );
};
Vue.prototype.wapLinkTo = function(goodsId, skuId) {
  // app端二维码
  return `${WAP_URL}/pages/product/goods?id=${skuId}&goodsId=${goodsId}`;
};

Array.prototype.remove = function(from, to) {
  var rest = this.slice((to || from) + 1 || this.length);
  this.length = from < 0 ? this.length + from : from;
  return this.push.apply(this, rest);
};

Object.keys(filters).forEach(key => {
  Vue.filter(key, filters[key]);
});
/* eslint-disable no-new */
new Vue({
  el: "#app",
  router,
  store,
  render: h => h(App),
  data: {
    currentPageName: ""
  },
  mounted() {
    // 初始化菜单
    util.initRouter(this);

    this.currentPageName = this.$route.name;
    // 显示打开的页面的列表
    this.$store.commit("setOpenedList");
    this.$store.commit("initCachepage");
  }
});
