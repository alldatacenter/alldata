export default {
  /**
   * @description 配置显示在浏览器标签的title
   */
  title: "lilishop",

  /**
   * @description token在Cookie中存储的天数，默认1天
   */
  cookieExpires: 1,
  /**
   * @description 是否使用国际化，默认为false
   *              如果不使用，则需要在路由中给需要在菜单中展示的路由设置meta: {title: 'xxx'}
   *              用来在菜单中显示文字
   */
  useI18n: true,

  

  /**
   * @description api请求基础路径
   */
  api_dev: {
    common: "https://common-api.pickmall.cn",
    buyer: "https://buyer-api.pickmall.cn",
    seller: "https://store-api.pickmall.cn",
    manager: "https://admin-api.pickmall.cn"
    // common: 'http://192.168.0.100:8890',
    // buyer: 'http://192.168.0.100:8888',
    // seller: 'http://192.168.0.100:8889',
    // manager: 'http://192.168.0.100:8887'
  },
  api_prod: {
    common: "https://common-api.pickmall.cn",
    buyer: "https://buyer-api.pickmall.cn",
    seller: "https://store-api.pickmall.cn",
    manager: "https://admin-api.pickmall.cn"
  },
  /**
   *  @description api请求基础路径前缀
   */
  baseUrlPrefix: "/store",
  /**
   * @description 需要加载的插件
   */
  plugin: {
    "error-store": {
      showInHeader: true, // 设为false后不会在顶部显示错误日志徽标
      developmentOff: true // 设为true后在开发环境不会收集错误信息，方便开发中排查错误
    }
  }
};
