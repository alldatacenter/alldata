/**
 * 金钱单位置换  2999 --> 2,999.00
 * @param val
 * @param unit
 * @param location
 * @returns {*}
 */
export function unitPrice(val, unit, location) {
  let price = formatPrice(val);
  if (location === "before") {
    return price.substr(0, price.length - 3);
  }
  if (location === "after") {
    return price.substr(-2);
  }
  return (unit || "") + price;
}
// 转义 
export function enCode(v1) {
  var entry = {
    "&#39;": "'",
    "&lt;": "<",
    "&gt;": ">",
    "&#40;": "(",
    "&#41;": ")"
  };

  v1 = v1.replace(/(&#39;)|(&lt;)|(&gt;)|(&#40;)|(&#41;)|\s/gi, function($0) {
    return entry[$0] || $0;
  });

  return v1;
}


/**
 * 订单来源
 */
export function clientTypeWay(val) {
  if (val == "H5") {
    return "移动端";
  } else if (val == "PC") {
    return "PC端";
  } else if (val == "WECHAT_MP") {
    return "小程序端";
  } else if (val == "APP") {
    return "移动应用端";
  } else {
    return val;
  }
}

/**
 * 货币格式化
 * @param price
 * @returns {string}
 */
export function formatPrice(price) {
  if (typeof price !== "number") return price;
  return String(Number(price).toFixed(2)).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
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
    S: d.getMilliseconds()
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
//是否格式化
export function unixWhether(status) {
  switch (status) {
    case "YES":
      return "是";
    case "NO":
      return "否";
  }
}

export function unixSellerBillStatus(status_code) {
  switch (status_code) {
    case "OUT":
      return "已出账";
    case "CHECK":
      return "已对账";
    case "EXAMINE":
      return "已审核";
    case "PAY":
      return "已结算";
    case "COMPLETE":
      return "已完成";
  }
}
export function unixSwitchStatus(status_code) {
  switch (status_code) {
    case "OPEN":
      return "开启";
    case "CLOSE":
      return "关闭";
  }
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

export function formatDate(date, fmt) {
  if (/(y+)/.test(fmt)) {
    fmt = fmt.replace(
      RegExp.$1,
      (date.getFullYear() + "").substr(4 - RegExp.$1.length)
    );
  }
  let o = {
    "M+": date.getMonth() + 1,
    "d+": date.getDate(),
    "h+": date.getHours(),
    "m+": date.getMinutes(),
    "s+": date.getSeconds()
  };
  for (let k in o) {
    if (new RegExp(`(${k})`).test(fmt)) {
      let str = o[k] + "";
      fmt = fmt.replace(
        RegExp.$1,
        RegExp.$1.length === 1 ? str : padLeftZero(str)
      );
    }
  }
  return fmt;
}
