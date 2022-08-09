let isDev = process.env.NODE_ENV === "development";

const UUID = isDev ? "uuid_key_dev" : "uuid_key";
const HAS_LOGIN = isDev ? "has_login_key_dev" : "has_login_key";
const ACCESS_TOKEN = isDev ? "access_token_key_dev" : "access_token_key";
const REFRESH_TOKEN = isDev ? "refresh_token_key_dev" : "refresh_token_key";
const USER_INFO = isDev ? "user_info_obj_dev" : "user_info_obj";
const FACE_LOGIN = isDev ? "face_login_dev" : "face_login";
const FINGER_LOGIN = isDev ? "finger_login_dev" : "finger_login";
const CART_BACKBTN = isDev ? "cart_backbtn_dev" : "cart_backbtn";
const AFTERSALE_DATA = isDev ? "aftersale_data_dev" : "aftersale_data";
export default {
  // 写入热门搜索时间戳
  setHotWords(val) {
    uni.setStorageSync("hotWords", val);
  },
  // 获取热门搜索时间戳
  getHotWords() {
    return uni.getStorageSync(`hotWords`);
  },
  //写入 展示还是不展示
  setShow(val) {
    uni.setStorageSync("show", val);
  },
  getShow() {
    if (uni.getStorageSync(`show`) === "" || uni.getStorageSync(`show`) === undefined) {
      return true;
    }
    return uni.getStorageSync(`show`);
  },
  // 获取face id登录
  getFaceLogin() {
    return uni.getStorageSync(FACE_LOGIN);
  },
  // 写入face id
  setFaceLogin(val) {
    uni.setStorageSync(FACE_LOGIN, val);
  },
  // 获取指纹登录
  getFingerLogin() {
    return uni.getStorageSync(FINGER_LOGIN);
  },
  // 写入指纹登录
  setFingerLogin(val) {
    uni.setStorageSync(FINGER_LOGIN, val);
  },
  // 写入用户信息
  setUserInfo(val) {
    uni.setStorageSync(USER_INFO, val);
  },
  // 获取用户信息
  getUserInfo() {
    return uni.getStorageSync(USER_INFO);
  },
  // 写入uuid
  setUuid(val) {
    uni.setStorageSync(UUID, val);
  },
  // 获取uuid
  getUuid() {
    return uni.getStorageSync(UUID);
  },
  // 写入登录
  setHasLogin(val) {
    uni.setStorageSync(HAS_LOGIN, val);
  },
  // 获取是否登录
  getHasLogin() {
    return uni.getStorageSync(HAS_LOGIN);
  },
  // 删除uuid
  removeUuid() {
    uni.removeStorageSync(UUID);
  },
  // 写入accessToken
  setAccessToken(val) {
    uni.setStorageSync(ACCESS_TOKEN, val);
  },
  // 获取accessToken
  getAccessToken() {
    return uni.getStorageSync(ACCESS_TOKEN);
  },
  // 后退购物车
  setCartBackbtn(val) {
    uni.setStorageSync(CART_BACKBTN, val);
  },

  // 删除token
  removeAccessToken() {
    uni.removeStorageSync(ACCESS_TOKEN);
  },
  // 写入刷新token
  setRefreshToken(val) {
    uni.setStorageSync(REFRESH_TOKEN, val);
  },
  // 获取刷新token
  getRefreshToken() {
    return uni.getStorageSync(REFRESH_TOKEN);
  },
  // 删除token
  removeRefreshToken() {
    uni.removeStorageSync(REFRESH_TOKEN);
  },

  setAfterSaleData(val) {
    uni.setStorageSync(AFTERSALE_DATA, val);
  },

  getAfterSaleData() {
    return uni.getStorageSync(AFTERSALE_DATA);
  },
  // 删除token
  removeAfterSaleData() {
    uni.removeStorageSync(AFTERSALE_DATA);
  },
};
