import * as regular from './regular'

export {
  regular
}


/**
 * 数组对象深拷贝
 * @param obj
 * @returns {*}
 */
export default function cloneObj(obj) {
  let str = null
  let newobj = obj.constructor === Array ? [] : {}
  if (typeof obj !== 'object') {
    return
  } else if (window && window.JSON) {
    str = JSON.stringify(obj) // 系列化对象
    newobj = JSON.parse(str) // 还原
  } else {
    for (var i in obj) {
      newobj[i] = typeof obj[i] === 'object' ? cloneObj(obj[i]) : obj[i]
    }
  }
  return newobj
}
