export default {
    widgetData: {}, // 组件已配备数据源，通过统一的数据源采集器获得的数据，可以是object，array,number,string等类型
    widgetConfig: {}, // 对应各组件定义的meta可配置属性字段，编辑后得到的属性value，供组件个性化渲染使用
    pageModel: {
        appId: 'testApp',//应用id
        nodeTypePath: 'testApp|app|T:pwIf7D9PC',//节点path
    },
    currentUser: {
        loginName: 'admin',//登录名
        lang: "zh_CN",//国际化语言类型
        nickname: "管理员",
        roles: [],// 角色
        userId: "empid::999999999",
        avatar: '',// 头像
    },
    nodeParams: {
        app_id: "testView2",
    },//节点参数阈，appId，数据源数据，过滤器字段等运行时参数字段都可以在nodeParams获取
}