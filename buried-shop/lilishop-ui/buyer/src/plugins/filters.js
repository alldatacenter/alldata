import * as Foundation from './Foundation.js';

/**
 * 金钱单位置换  2999 --> 2,999.00
 * @param val
 * @param unit
 * @param location
 * @returns {*}
 */
export function unitPrice (val, unit, location) {
  if (!val) val = 0;
  let price = Foundation.formatPrice(val);
  if (location === 'before') {
    return price.substr(0, price.length - 3);
  }
  if (location === 'after') {
    return price.substr(-2);
  }
  return (unit || '') + price;
}

/**
 * 处理unix时间戳，转换为可阅读时间格式
 * @param unix
 * @param format
 * @returns {*|string}
 */
export function unixToDate (unix, format) {
  if (!unix) return '';
  let _format = format || 'yyyy-MM-dd hh:mm:ss';
  const d = new Date(unix * 1000);
  const o = {
    'M+': d.getMonth() + 1,
    'd+': d.getDate(),
    'h+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds(),
    'q+': Math.floor((d.getMonth() + 3) / 3),
    S: d.getMilliseconds()
  };
  if (/(y+)/.test(_format)) _format = _format.replace(RegExp.$1, (d.getFullYear() + '').substr(4 - RegExp.$1.length));
  for (const k in o) {
    if (new RegExp('(' + k + ')').test(_format)) _format = _format.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)));
  };
  return _format;
}

/**
 * 替换地址栏 逗号分隔为空格分隔
 */
export function unitAddress (val) {
  if (!val) return '';
  return val.replace(/,/g, ' ');
}

/**
 * 13888888888 -> 138****8888
 * @param mobile
 * @returns {*}
 */
export function secrecyMobile (mobile) {
  mobile = String(mobile);
  if (!/\d{11}/.test(mobile)) {
    return mobile;
  }
  return mobile.replace(/(\d{3})(\d{4})(\d{4})/, '$1****$3');
}
