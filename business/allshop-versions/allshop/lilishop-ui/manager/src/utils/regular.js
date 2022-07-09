/**
 * 各种正则表达式
 *
 * qq       qq号
 * name     汉字
 * mobile   手机号
 * email    电子邮箱
 * password 密码【6-20位】
 * integer  正整数【不包含0】
 * money    金钱
 * IDCard   身份证
 * userName 账户名称【汉字、字母、数字、“-”、“_”的组合】
 * URL      URL
 * TEL      固定电话
 */


//qq
export const qq = /^[1-9][0-9]{4,14}$/
//汉字
export const name = /^[\u4e00-\u9fa5]{0,}$/

// 手机号
export const mobile = /^0?(13[0-9]|14[0-9]|15[0-9]|16[0-9]|17[0-9]|18[0-9]|19[0-9])[0-9]{8}$/

// 电子邮箱
export const email = /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/

// 密码【6-20位】
export const password = /^[@A-Za-z0-9!#$%^&*.~,]{6,20}$/

// 正整数【不包含0】
export const integer = /^[1-9]\d*$/

// 正整数【包含0】
export const Integer = /^[0-9]\d*$/

// 折扣
export const discount = /^((0\.[1-9]{1})|(([1-9]{1})(\.\d{1})?))$/

// 0-100正整数
export const rate = /^([0-9]{1,2}|100)$/

// 金钱
export const money = /(^[1-9]([0-9]+)?(\.[0-9]{1,2})?$)|(^(0){1}$)|(^[0-9]\.[0-9]([0-9])?$)/


// 身份证
export const IDCard = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/

// 账户名称【汉字、字母、数字、“-”、“_”的组合】
export const userName = /[A-Za-z0-9_\-\u4e00-\u9fa5]$/

// URL
export const URL =
  /^(https?|ftp):\/\/([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/

// 固话
export const TEL = /0\d{2,3}-\d{7,8}/


// 正整数
export const INTEGER = {

    pattern: /^[0-9]\d{0,10}|0$/,
    message:'请输入正整数'
  }
// 正整数
export const NUMBER = {
    pattern: /^(\-|\+)?\d{0,10}$/,
    message:'请输入数字'
}
export const VARCHAR5 = {
  pattern:/^.{1,5}$/,
  message:'长度应该限制在1-5个字符'
}

export const VARCHAR20 = {
  pattern:/^.{1,20}$/,
  message:'长度应该限制在1-20个字符'
}

export const VARCHAR255 = {
  pattern:/^.{1,255}$/,
  message:'超出最大长度限制'
}

export const URL200 = {
  pattern:/[a-zA-z]+\:\/\/[^\s]{1,190}/,
  message:'请输入长度不超过200的URL地址'
}
export const REQUIRED = {
  required: true,
  message:'请填写参数'
}
