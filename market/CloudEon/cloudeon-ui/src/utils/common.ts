/*
时间戳转日期格式
formatDate(时间戳, 返回格式)
formatDate(1640493771000, 'yyyy-MM-dd')
*/
type oldDateType = string | number
export function formatDate (oldDate: any, fmt: string) {
    let date = new Date()
    if(!oldDate) return
    if((typeof oldDate ==='string' && oldDate.indexOf('T') > -1)){
        date = new Date(oldDate)
    }
    else if (typeof oldDate === 'string' || typeof oldDate === 'number') {
      date = new Date(+oldDate)
    } else {
      date = oldDate
    }
    if (/(y+)/.test(fmt)) {
      fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length))
    }
    let o = {
      'M+': date.getMonth() + 1,
      'd+': date.getDate(),
      'h+': date.getHours(),
      'm+': date.getMinutes(),
      's+': date.getSeconds()
    }
    function padLeftZero (str: any) {
      return ('00' + str).substr(str.length)
    }
    for (let k in o) {
      if (new RegExp(`(${k})`).test(fmt)) {
        let str = o[k] + ''
        fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? str : padLeftZero(str))
      }
    }
    return fmt
  }