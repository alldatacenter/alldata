import Vue from "vue";
import Vuex from "vuex";
import storage from "@/utils/storage";

Vue.use(Vuex);

const store = new Vuex.Store({
  state: {
    verificationKey: "", //获取key表示验证通过
    distributionId:"", //分销员Id 如果当前账户从未登录过时记录
    hasLogin: storage.getHasLogin(),
    userInfo: storage.getUserInfo(),
    uuid: storage.getUuid(),
    token: "",
  },
  mutations: {
    login(state, userInfo) {
      state.userInfo = userInfo || {};
      state.userName =
        userInfo.Name || userInfo.Nickname || userInfo.Username || "匿名用户";
      state.hasLogin = true;
    },
    logout(state) {
      state.userName = "";
      state.hasLogin = false;
    },
  },
  actions: {},
});

export default store;
