/**
 * 一些常用的基础方法
 * whetherNavigate 登录后跳转判断
 * unixToDate    将unix时间戳转换为指定格式
 * dateToUnix    将时间转unix时间戳
 * deepClone     对一个对象进行深拷贝
 * formatPrice   货币格式化
 * secrecyMobile 手机号隐私保护
 * randomString  随机生成指定长度的字符串
 */

/**
 * 验证银行卡号
 */
export function checkBankno(bankno) {
  var lastNum = bankno.substr(bankno.length - 1, 1); //取出最后一位（与luhm进行比较）
  var first15Num = bankno.substr(0, bankno.length - 1); //前15或18位
  var newArr = [];

  for (var i = first15Num.length - 1; i > -1; i--) {
    //前15或18位倒序存进数组
    newArr.push(first15Num.substr(i, 1));
  }

  var arrJiShu = []; //奇数位*2的积 <9
  var arrJiShu2 = []; //奇数位*2的积 >9
  var arrOuShu = []; //偶数位数组
  for (var j = 0; j < newArr.length; j++) {
    if ((j + 1) % 2 == 1) {
      //奇数位
      if (parseInt(newArr[j]) * 2 < 9) arrJiShu.push(parseInt(newArr[j]) * 2);
      else arrJiShu2.push(parseInt(newArr[j]) * 2);
    } //偶数位
    else arrOuShu.push(newArr[j]);
  }

  var jishu_child1 = []; //奇数位*2 >9 的分割之后的数组个位数
  var jishu_child2 = []; //奇数位*2 >9 的分割之后的数组十位数
  for (var h = 0; h < arrJiShu2.length; h++) {
    jishu_child1.push(parseInt(arrJiShu2[h]) % 10);
    jishu_child2.push(parseInt(arrJiShu2[h]) / 10);
  }

  var sumJiShu = 0; //奇数位*2 < 9 的数组之和
  var sumOuShu = 0; //偶数位数组之和
  var sumJiShuChild1 = 0; //奇数位*2 >9 的分割之后的数组个位数之和
  var sumJiShuChild2 = 0; //奇数位*2 >9 的分割之后的数组十位数之和
  var sumTotal = 0;
  for (var m = 0; m < arrJiShu.length; m++) {
    sumJiShu = sumJiShu + parseInt(arrJiShu[m]);
  }
  for (var n = 0; n < arrOuShu.length; n++) {
    sumOuShu = sumOuShu + parseInt(arrOuShu[n]);
  }
  for (var p = 0; p < jishu_child1.length; p++) {
    sumJiShuChild1 = sumJiShuChild1 + parseInt(jishu_child1[p]);
    sumJiShuChild2 = sumJiShuChild2 + parseInt(jishu_child2[p]);
  }
  //计算总和
  sumTotal =
    parseInt(sumJiShu) +
    parseInt(sumOuShu) +
    parseInt(sumJiShuChild1) +
    parseInt(sumJiShuChild2);
  //计算Luhm值
  var k = parseInt(sumTotal) % 10 == 0 ? 10 : parseInt(sumTotal) % 10;
  var luhm = 10 - k;
  if (lastNum == luhm) {
    return true;
  } else {
    return false;
  }
}

/**
 * 登录后跳转判断
 * 计算出当前router路径
 * 1.如果跳转的链接为登录页面或跳转的链接为空页面。则会重新跳转到首页
 * 2.都不满足返回跳转页面
 * @param type  'default' || 'wx'  //返回地址会做判断默认为default
 */

export function whetherNavigate(type = "default") {
  let navigation = getCurrentPages()[getCurrentPages().length - (getCurrentPages().length ) ];
  if (getCurrentPages().length > 1) {
    console.log(navigation, getCurrentPages());
    if (navigation.route == "pages/passport/login") {
      navigationToBack(type);
    } else {
      if (!navigation.route || navigation.route == "undefined") {
        navigationToBack(type);
      } else {
        uni.navigateBack({
          delta: getCurrentPages().length,
        });
      }
    }
  } else {
    uni.switchTab({
      url: "/pages/tabbar/home/index",
    });
  }
}

/**
 * 将unix时间戳转换为指定格式
 * @param unix   时间戳【秒】
 * @param format 转换格式
 * @returns {*|string}
 */
export function unixToDate(unix, format) {
  if (!unix) return unix;
  let _format = format || "yyyy-MM-dd hh:mm:ss";
  const d = new Date(unix);
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
 * 将时间转unix时间戳
 * @param date
 * @returns {number} 【秒】
 */
export function dateToUnix(date) {
  let newStr = date.replace(/:/g, "-");
  newStr = newStr.replace(/ /g, "-");
  const arr = newStr.split("-");
  const datum = new Date(
    Date.UTC(
      arr[0],
      arr[1] - 1,
      arr[2],
      arr[3] - 8 || -8,
      arr[4] || 0,
      arr[5] || 0
    )
  );
  return parseInt(datum.getTime() / 1000);
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
 * 手机号隐私保护
 * 隐藏中间四位数字
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
 * 随机生成指定长度的字符串
 * @param length
 * @returns {string}
 */
export function randomString(length = 32) {
  const chars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  const maxPos = chars.length;
  let _string = "";
  for (let i = 0; i < length; i++) {
    _string += chars.charAt(Math.floor(Math.random() * maxPos));
  }
  return _string;
}

/**
 * 计算传秒数的倒计时【天、时、分、秒】
 * @param seconds
 * @returns {{day : *, hours : *, minutes : *, seconds : *}}
 */

export function countTimeDown(seconds) {
  const leftTime = (time) => {
    if (time < 10) time = "0" + time;
    return time + "";
  };
  return {
    day: leftTime(parseInt(seconds / 60 / 60 / 24, 10)),
    hours: leftTime(parseInt((seconds / 60 / 60) % 24, 10)),
    minutes: leftTime(parseInt((seconds / 60) % 60, 10)),
    seconds: leftTime(parseInt(seconds % 60, 10)),
  };
}

function navigationToBack(type) {
  if (type == "wx") {
    // console.log(getCurrentPages().length - 3)
    uni.navigateBack({
      delta: getCurrentPages().length,
    });
  } else {
    uni.switchTab({
      url: "/pages/tabbar/home/index",
    });
  }
}

/**
 * 计算当前时间到第二天0点的倒计时[秒]
 * @returns {number}
 */
export function theNextDayTime() {
  const nowDate = new Date();
  const time =
    new Date(
      nowDate.getFullYear(),
      nowDate.getMonth(),
      nowDate.getDate() + 1,
      0,
      0,
      0
    ).getTime() - nowDate.getTime();
  return parseInt(time / 1000);
}
module.exports = {
  unixToDate,
  dateToUnix,
  formatPrice,
  secrecyMobile,
  randomString,
  countTimeDown,
  theNextDayTime,
  whetherNavigate,
  checkBankno,
};
