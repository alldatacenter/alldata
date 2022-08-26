
const Cookie = require('js-cookie')
module.exports = {
  title:Cookie.get('siteName') || 'lilishop', //配置显示在浏览器标签的title、底部信息、部分信息展示的值
  icpCard: "", // icp证
  company: {
    href: "https://pickmall.cn",
    name: "北京宏业汇成科技有限公司",
  }, //公司信息
  icpMessage: "京ICP备20009696号-1", //icp备案
  /**
   * 高德地图申请链接
   * https://lbs.amap.com/api/javascript-api/guide/abc/prepare
   * 添加成功后，可获取到key值和安全密钥jscode（自2021年12月02日升级，升级之后所申请的 key 必须配备安全密钥 jscode 一起使用)
   */
  //FIXME 请检查当前高德key创建的日期，如果2021年12月02日之前申请的 无需填写安全密钥
  aMapSecurityJsCode:"2bd0fbf621881f4c77be74f0e76495f3", // 高德web端js申请的安全密钥
  aMapKey: "7f11113750315d8543daaf5c3ba353ca", //高德web端js申请的api key
  enableCDN: true, //生产环境 是否启用cdn加载 vue等js
  port: 10000, //端口
};
