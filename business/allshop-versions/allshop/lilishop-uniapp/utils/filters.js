import Foundation from "./Foundation.js";
import storage from "@/utils/storage.js";
/**
 * 金钱单位置换  2999 --> 2,999.00
 * @param val
 * @param unit
 * @param location
 * @returns {*}
 */
export function unitPrice(val, unit, location) {
  if (!val) val = 0;
  let price = Foundation.formatPrice(val);
  if (location === "before") {
    return price.substr(0, price.length - 3);
  }
  if (location === "after") {
    return price.substr(-2);
  }
  return (unit || "") + price;
}

/**
 * 脱敏姓名
 */

export function noPassByName(str) {
  if (null != str && str != undefined) {
    if (str.length <= 3) {
      return "*" + str.substring(1, str.length);
    } else if (str.length > 3 && str.length <= 6) {
      return "**" + str.substring(2, str.length);
    } else if (str.length > 6) {
      return str.substring(0, 2) + "****" + str.substring(6, str.length);
    }
  } else {
    return "";
  }
}

/**
 * 处理unix时间戳，转换为可阅读时间格式
 * @param unix
 * @param format
 * @returns {*|string}
 */
export function unixToDate(unix, format) {
  let _format = format || "yyyy-MM-dd hh:mm:ss";
  const d = new Date(unix * 1000);
  const o = {
    "M+": d.getMonth() + 1,
    "d+": d.getDate(),
    "h+": d.getHours(),
    "m+": d.getMinutes(),
    "s+": d.getSeconds(),
    "q+": Math.floor((d.getMonth() + 3) / 3),
    S: d.getMilliseconds(),
  };
  if (/(y+)/.test(_format))
    _format = _format.replace(
      RegExp.$1,
      (d.getFullYear() + "").substr(4 - RegExp.$1.length)
    );
  for (const k in o)
    if (new RegExp("(" + k + ")").test(_format))
      _format = _format.replace(
        RegExp.$1,
        RegExp.$1.length === 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length)
      );
  return _format;
}

/**
 * 13888888888 -> 138****8888
 * @param mobile
 * @returns {*}
 */
export function secrecyMobile(mobile) {
  mobile = String(mobile);
  if (!/\d{11}/.test(mobile)) {
    return mobile;
  }
  return mobile.replace(/(\d{3})(\d{4})(\d{4})/, "$1****$3");
}

/**
 * 清除逗号
 *
 */
export function clearStrComma(str) {
  str = str.replace(/,/g, ""); //取消字符串中出现的所有逗号
  return str;
}

/**
 * 判断用户是否登录
 * @param val  如果为auth则判断是否登录
 * 如果传入 auth 则为判断是否登录
 */
export function isLogin(val) {
  let userInfo = storage.getUserInfo();
  if (val == "auth") {
    return userInfo.id ? true : false;
  } else {
    return storage.getUserInfo();
  }
}

/**
 * 验证是否登录如果没登录则去登录
 * @param {*} val
 * @returns
 */

export function forceLogin() {
  let userInfo = storage.getUserInfo();
  if (!userInfo.id) {
    // #ifdef MP-WEIXIN

    uni.navigateTo({
      url: "/pages/passport/wechatMPLogin",
    });

    // #endif

    // #ifndef MP-WEIXIN

    uni.navigateTo({
      url: "/pages/passport/login",
    });

    //  #endif
  }
}

/**
 * 获取当前加载的页面对象
 * @param val
 */
export function getPages(val) {
  const pages = getCurrentPages(); //获取加载的页面
  const currentPage = pages[pages.length - 1]; //获取当前页面的对象
  const url = currentPage.route; //当前页面url

  return val ? currentPage : url;
}

/**
 * 跳转到登录页面
 */
export function navigateToLogin(type = "navigateTo") {
  /**
   * 此处进行条件编译判断
   * 微信小程序跳转到微信小程序登录页面
   * H5/App跳转到普通登录页面
   */
  // #ifdef MP-WEIXIN
  uni[type]({
    url: "/pages/passport/wechatMPLogin",
  });
  // #endif
  // #ifndef MP-WEIXIN
  uni[type]({
    url: "/pages/passport/login",
  });
  //  #endif
}

/**
 * 服务状态列表
 */
export function serviceStatusList(val) {
  let statusList = {
    APPLY: "申请售后",
    PASS: "通过售后",
    REFUSE: "拒绝售后",
    BUYER_RETURN: "买家退货，待卖家收货",
    SELLER_RE_DELIVERY: "商家换货/补发",
    SELLER_CONFIRM: "卖家确认收货",
    SELLER_TERMINATION: "卖家终止售后",
    BUYER_CONFIRM: "买家确认收货",
    BUYER_CANCEL: "买家取消售后",
    WAIT_REFUND: "等待平台退款",
    COMPLETE: "完成售后",
  };
  return statusList[val];
}

/**
 * 订单状态列表
 */
export function orderStatusList(val) {
  let orderStatusList = {
    UNDELIVERED: "待发货",
    UNPAID: "未付款",
    PAID: "已付款",
    DELIVERED: "已发货",
    CANCELLED: "已取消",
    COMPLETED: "已完成",
    COMPLETE: "已完成",
    TAKE: "待核验",
  };
  return orderStatusList[val];
}
