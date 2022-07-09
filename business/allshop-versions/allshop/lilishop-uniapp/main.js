import Vue from "vue";
import App from "./App";
import * as filters from "./utils/filters.js"; // global filter
import uView from "uview-ui";
import store from "./store";


/**
 * 仅在h5中显示唤醒app功能
 * 在h5页面手动挂载
 * 
 */
// #ifdef H5
import airBtn from "@/components/m-airbtn/index.vue";
let btn = Vue.component("airBtn", airBtn); //全局注册
document.body.appendChild(new btn().$mount().$el);
// #endif


/**
 * 全局filters
 */

Object.keys(filters).forEach((key) => {
  Vue.filter(key, filters[key]);
});

// 引入Vuex
Vue.prototype.$store = store;
Vue.use(uView);
Vue.config.productionTip = false;


/**
 * 注意！
 * 此处将常用的颜色嵌入到原型链上面
 * 颜色使用驼峰命名对应 uni.scss中全局颜色变量名
 * 如需更换主题请修改此处以及uni.scss中的全局颜色
 */
// 主题色
Vue.prototype.$mainColor = "#ff3c2a";
// 高亮主题色
Vue.prototype.$lightColor = "#ff6b35";
// 辅助高亮颜色
Vue.prototype.$aiderLightColor = "#ff9f28";


App.mpType = "app";

const app = new Vue({
  ...App,
});
app.$mount();
