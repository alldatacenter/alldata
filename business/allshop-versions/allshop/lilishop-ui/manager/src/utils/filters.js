/**
 * 金钱单位置换  2999 --> 2,999.00
 * @param val
 * @param unit
 * @param location
 * @returns {*}
 */
export function unitPrice(val, unit, location) {
  let price = formatPrice(val)
  if (location === 'before') {
    return price.substr(0, price.length - 3)
  }
  if (location === 'after') {
    return price.substr(-2)
  }
  return (unit || '') + price
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



let timer, flag;
/**
 * 节流原理：在一定时间内，只能触发一次
 *
 * @param {Function} func 要执行的回调函数
 * @param {Number} wait 延时的时间
 * @param {Boolean} immediate 是否立即执行
 * @return null
 */
 export function throttle(func, wait = 500, immediate = true) {
	if (immediate) {
		if (!flag) {
			flag = true;
			// 如果是立即执行，则在wait毫秒内开始时执行
			typeof func === 'function' && func();
			timer = setTimeout(() => {
				flag = false;
			}, wait);
		}
	} else {
		if (!flag) {
			flag = true
			// 如果是非立即执行，则在wait毫秒内的结束处执行
			timer = setTimeout(() => {
				flag = false
				typeof func === 'function' && func();
			}, wait);
		}

	}
};





let timeout = null;

/**
 * 防抖原理：一定时间内，只有最后一次操作，再过wait毫秒后才执行函数
 *
 * @param {Function} func 要执行的回调函数
 * @param {Number} wait 延时的时间
 * @param {Boolean} immediate 是否立即执行
 * @return null
 */
 export function debounce(func, wait = 500, immediate = false) {
	// 清除定时器
	if (timeout !== null) clearTimeout(timeout);
	// 立即执行，此类情况一般用不到
	if (immediate) {
		var callNow = !timeout;
		timeout = setTimeout(function() {
			timeout = null;
		}, wait);
		if (callNow) typeof func === 'function' && func();
	} else {
		// 设置定时器，当最后一次操作后，timeout不会再被清除，所以在延时wait毫秒后执行func回调方法
		timeout = setTimeout(function() {
			typeof func === 'function' && func();
		}, wait);
	}
}


/**
 * 货币格式化
 * @param price
 * @returns {string}
 */
export function formatPrice(price) {
  if (typeof price !== 'number') return price
  return String(Number(price).toFixed(2)).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 处理unix时间戳，转换为可阅读时间格式
 * @param unix
 * @param format
 * @returns {*|string}
 */
export function unixToDate(unix, format) {
  let _format = format || 'yyyy-MM-dd hh:mm:ss'
  const d = new Date(unix * 1000)
  const o = {
    'M+': d.getMonth() + 1,
    'd+': d.getDate(),
    'h+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds(),
    'q+': Math.floor((d.getMonth() + 3) / 3),
    S: d.getMilliseconds()
  }
  if (/(y+)/.test(_format)) _format = _format.replace(RegExp.$1, (d.getFullYear() + '').substr(4 - RegExp.$1.length))
  for (const k in o) if (new RegExp('(' + k + ')').test(_format)) _format = _format.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)))
  return _format
}
//是否格式化
export function unixWhether(status) {
  switch (status) {
      case 'YES':
        return "是"
      case 'NO':
        return "否"
  }
}

export function unixSellerBillStatus(status_code) {
  switch (status_code) {
    case 'OUT':
      return '已出账'
    case 'CHECK':
      return '已对账'
    case 'EXAMINE':
      return '已审核'
    case 'PAY':
      return '已结算'
    case 'COMPLETE':
      return '已完成'
  }
}
export function unixSwitchStatus(status_code) {
  switch (status_code) {
    case 'OPEN':
      return '开启'
    case 'CLOSE':
      return '关闭'
  }
}

/**
 * 13888888888 -> 138****8888
 * @param mobile
 * @returns {*}
 */
export function secrecyMobile(mobile) {
  mobile = String(mobile)
  if (!/\d{11}/.test(mobile)) {
    return mobile
  }
  return mobile.replace(/(\d{3})(\d{4})(\d{4})/, '$1****$3')
}


export function formatDate(date, fmt) {
  if (/(y+)/.test(fmt)) {
      fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length));
  }
  let o = {
      'M+': date.getMonth() + 1,
      'd+': date.getDate(),
      'h+': date.getHours(),
      'm+': date.getMinutes(),
      's+': date.getSeconds()
  };
  for (let k in o) {
      if (new RegExp(`(${k})`).test(fmt)) {
          let str = o[k] + '';
          fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? str : padLeftZero(str));
      }
  }
  return fmt;
};

// 楼层装修，选择链接处理跳转方式
export function formatLinkType (item) {
  const types = ['goods', 'category', 'shops', 'marketing', 'pages', 'other'] // 所有跳转的分类 依次为 商品、分类、店铺、活动、页面、其他
  let url = '';
  switch (item.___type) {
    case 'goods':
      url =  `/goodsDetail?skuId=${item.id}&goodsId=${item.goodsId}`;
      break;

    case 'category':
      url = `/goodsList?categoryId=${item.allId}`;
      break;

    case 'shops':
      url = `/merchant?id=${item.id}`;
      break;

    case 'marketing': // 暂无数据，需要后续修改
      url = `/seckill?id=${item.id}`;
      break;

    case 'pages': // 暂无数据，需要后续修改
      url = `/article?id=${item.id}`;
      break;

    case 'other':
      switch (item.title) {
        case '首页':
          url = '/';
          break;
        case '购物车':
          url = '/cart';
          break;
        case '我的订单':
          url = '/home/MyOrder';
          break;
        case '收藏商品':
          url = '/home/Favorites';
          break;
        case '个人中心':
          url = '/home';
          break;
        case '外部链接':
          url = item.url;
          break;
        case '秒杀频道':
          url = '/seckill';
          break;
        case '领券中心':
          url = '/coupon';
          break;
      }
      break;
  }
  return url;
}
