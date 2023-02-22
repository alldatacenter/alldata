import store from '@/store'

// 过滤文件类型
const fileTypeFilter = function(data) {
  let val = ''
  store.state.fileTypes.forEach(item => {
    if(item.value === data) val = item.name
  })
  return val
}

// 处理时间格式
const timeFormat = (value, format) => {
  let date = new Date(value);
  let y = date.getFullYear();
  let m = date.getMonth() + 1;
  let d = date.getDate();
  let h = date.getHours();
  let min = date.getMinutes();
  let s = date.getSeconds();
  let result = "";
  if (format == undefined) {
    result = `${y}-${m < 10 ? "0" + m : m}-${d < 10 ? "0" + d : d} ${
      h < 10 ? "0" + h : h
    }:${min < 10 ? "0" + min : min}:${s < 10 ? "0" + s : s}`;
  }
  if (format == "yyyy-MM-dd") {
    result = `${y}-${m < 10 ? "0" + m : m}-${d < 10 ? "0" + d : d}`;
  }
  if (format == "yyyy-MM") {
    result = `${y}-${m < 10 ? "0" + m : m}`;
  }
  if (format == "MM-dd") {
    result = ` ${m < 10 ? "0" + m : m}-${d < 10 ? "0" + d : d}`;
  }
  if (format == "hh:mm") {
    result = ` ${h < 10 ? "0" + h : h}:${min < 10 ? "0" + min : min}`;
  }
  if (format == "hh:mm:ss") {
    result = ` ${h < 10 ? "0" + h : h}:${min < 10 ? "0" + min : min}:${s < 10 ? "0" + s : s}`;
  }
  if (format == "yyyy") {
    result = `${y}`;
  }
  if (format == "yyyy-MM-dd hh:mm:ss") {
    result = `${y}-${m < 10 ? "0" + m : m}-${d < 10 ? "0" + d : d}  ${h < 10 ? "0" + h : h}:${min < 10 ? "0" + min : min}:${s < 10 ? "0" + s : s}`;
  }
  if (format == "年月日") {
    result = `${y}年${m}月${d}日`;
  }
  return result;
};

// 过滤小数点位数
const tofixed = function(val, num = 2) {
  let result = ''
  if(isNaN(val)) {
    result = val.toFixed(num)
  }else {
    result = Number(val).toFixed(num)
  }
  return result
}

export { timeFormat, fileTypeFilter, tofixed }
