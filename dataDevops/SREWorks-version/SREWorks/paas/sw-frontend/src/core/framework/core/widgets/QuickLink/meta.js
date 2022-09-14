/**
 * Created by caoshuaibiao on 2020/12/21.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义 
 */
export default  {
    "id": "QuickLink",
    "type": "QuickLink",
    "name": "QuickLink",
    "title": "快捷链接",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "快捷链接组件",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icon.svg'),
            "fontClass":"QuickLink"
        },
        "build": {
            "time": "",
            "repo": "",
            "branch": "",
            "hash": ""
        },
        "screenshots": [],
        "updated": "",
        "version": "",
        "docs":"#### 使用场景 \r \n  页面需要放置其他功能/页面 快捷链接时使用 \r \n #### 配置项 \r \n links 为定义的链接集合,可以配置为变量,也支持直接配置成固定数组,元素定义如下 \r \n " +
        " <ul> <li> * icon : 链接的图标,支持antd中的字符串格式及图片url格式,不存在的时候取标题第一个字</li>" +
        " <li> * title : 链接显示的标题,不能为空 </li>" +
        " <li> * href : 链接地址,不能为空 </li>" +
        " <li> * description : 链接的说明,可以为空 </li></ul>"
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "type":"QuickLink",
            "config":{
                "links":[
                    {
                        "icon":"aliyun",
                        "title":"阿里云",
                        "href":"https://www.iconfont.cn/home/index"
                    }
                ]
            }
        },
        "schema": {
            "type": "object",
            "properties": {
                "links":{
                    "description": "若为动态集合,可前往源JSON中直接填写占位变量,格式为$(链接集合变量名)。",
                    "title": "链接集合",
                    "required": false,
                    "x-component": "EditTable",
                    "type": "string",
                    "x-component-props":{
                        "columns": [
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                    "inputTip":"帮助文档的标题",
                                },
                                "dataIndex": "title",
                                "title": "标题"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "inputTip":"帮助文档的链接路径",
                                    "type": 1
                                },
                                "dataIndex": "href",
                                "title": "链接路径"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "inputTip":"链接的描述",
                                    "type": 1
                                },
                                "dataIndex": "description",
                                "title": "描述"
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                    "inputTip":"antd3中的icon名或者图片地址或图片base64",
                                },
                                "dataIndex": "icon",
                                "title": "图标"
                            },

                        ]
                    }
                }
            }
        },
        "dataMock": {}
    },
    "catgory": "base"
};