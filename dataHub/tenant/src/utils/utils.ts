import { parse } from 'querystring'
import moment from 'moment'

const reg = /(((^https?:(?:\/\/)?)(?:[-;:&=+$,\w]+@)?[A-Za-z0-9.-]+(?::\d+)?|(?:www.|[-;:&=+$,\w]+@)[A-Za-z0-9.-]+)((?:\/[+~%/.\w-_]*)?\??(?:[-+=&;%@.\w_]*)#?(?:[\w]*))?)$/

export const isUrl = (path: string): boolean => reg.test(path)

export const isAntDesignPro = (): boolean => {
  if (ANT_DESIGN_PRO_ONLY_DO_NOT_USE_IN_YOUR_PRODUCTION === 'site') {
    return true
  }
  return window.location.hostname === 'preview.pro.ant.design'
}

// 给官方演示站点用，用于关闭真实开发环境不需要使用的特性
export const isAntDesignProOrDev = (): boolean => {
  const { NODE_ENV } = process.env
  if (NODE_ENV === 'development') {
    return true
  }
  return isAntDesignPro()
}

export const getPageQuery = () => parse(window.location.href.split('?')[1])

export function transformDate(str: string | number | Date, format: string = 'YYYY-MM-DD HH:mm:SS') {
  return moment(str).format(format)
}

export function regFunction(value: string | number, replaceValue = ',') {
  return String(value).replace(/(?=(\B\d{3})+$)/g, replaceValue)
}

export function validateDate(value: string | number, type: 'positive' | 'negative' = 'positive') {
  const rules = new Map()
    .set('positive', /^\d+(\.\d+)?$/g) // 正数
    .set('negative', /^((-\d+(\.\d+)?)|(0+(\.0+)?))$/g) // 负数

  const regExp = new RegExp(rules.get(type))
  return regExp.test(String(value))
}

export function getDay(format = 'YYYY-MM-DD HH:mm:ss') {
  const start = new Date()
  start.setDate(start.getDate() - 1)
  const startDate = transformDate(start, format)
  const endDate = transformDate(new Date(), format)
  return { startDate, endDate }
}

export function getWeek(format = 'YYYY-MM-DD HH:mm:ss') {
  const now = new Date()
  const nowTime = moment(now).format('HH:mm:ss')
  const before = moment().subtract(7, 'days').calendar()

  const startDate = `${before} ${nowTime}`
  const endDate = transformDate(now, format)
  return { startDate, endDate }
}

export function getMonth(format = 'YYYY-MM-DD HH:mm:ss') {
  const now = new Date()
  const nowTime = moment(now).format('HH:mm:ss')
  const before = moment().subtract(30, 'days').calendar()

  const startDate = `${before} ${nowTime}`
  const endDate = transformDate(now, format)
  return { startDate, endDate }
}

export function mobileCheck() {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

/**
 * 千分位格式化
 * @param num 数字
 */
export function formatThousands(num: number) {
  return num.toLocaleString('zh-Hans-CN')
}

/**
 * 格式化成带单位的量级
 * @param num 数字
 */
export function formatToUnit(num: number) {
  if (num >= 1000000) {
    return '100万+'
  }

  if (num >= 100000) {
    return '10万+'
  }

  if (num >= 10000) {
    return '1万+'
  }

  if (num >= 1000) {
    return '1000+'
  }

  return '少于1000'
}

export function escapeRegExp(text: string) {
  return text.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&')
}
