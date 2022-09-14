/**
 * Created by caoshuaibiao on 2020/12/21.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义 
 */
export default  {
    "id": "PageHeaderLayout",
    "type": "PageHeaderLayout",
    "name": "PageHeaderLayout",
    "title": "PageHeader布局",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "带PageHeader的布局组件,支持所在节点生成子节点的菜单及路由",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icon.svg'),
            "fontClass":"PageHeaderLayout"
        },
        "build": {
            "time": "",
            "repo": "",
            "branch": "",
            "hash": ""
        },
        "screenshots": [],
        "updated": "",
        "version": ""
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "config": {
                "title":"页面的主题标题",
                "description":"页面的其他描述信息",
                "bgUrl":"",
                "topMenuType":"menu",
                "wrapper":"default",
                "statistics":[],
            },
            "type":"PageHeaderLayout"
        },
        "schema": {
            "type": "object",
            "properties": {
                "topMenuType": {
                    "description": "顶部菜单展示方式",
                    "title": "菜单展示方式",
                    "required": false,
                    "type": "string",
                    "x-component": "Radio",
                    "x-component-props":{
                        "options":[{"value":"menu","label":"Menu"},{"value":"link","label":"Link"}]
                    }
                },
                "description": {
                    "description": "布局组件Header显示的描述信息",
                    "title": "描述",
                    "required": false,
                    "x-component": "Input",
                    "x-visibleExp":"config.topMenuType==='menu'",
                    "type": "string"
                },
                "paddingInner": {
                    "description": "适配边距(单位:vw)",
                    "title": "适配边距(vw)",
                    "required": false,
                    "x-component": "Input",
                    "x-visibleExp":"config.topMenuType==='menu'",
                    "type": "string"
                },
                "bgUrl": {
                    "description": "背景图片url",
                    "title": "背景图片url",
                    "required": false,
                    "x-component": "Input",
                    "x-visibleExp":"config.topMenuType==='menu'",
                    "type": "string"
                },
                "logoUrl": {
                    "description": "图标url",
                    "title": "图标url",
                    "required": false,
                    "x-component": "Input",
                    "x-visibleExp":"config.topMenuType==='menu'",
                    "type": "string"
                },
                "statistics":{
                    "type": "string",
                    "title": "统计数值",
                    "required": false,
                    "description": "参数有label（参数的名称）,dataIndex(参数key), unit(参数的单位)",
                    "x-component": "EditTable",
                    "x-component-props": {
                        "columns": [
                            {
                                "editProps": {
                                    "required": true,
                                    "type": 1,
                                    "inputTip": "参数的名称",
                                },
                                "dataIndex": "label",
                                "title": "名称",
                            },
                            {
                                "editProps": {
                                    "required": true,
                                    "inputTip": "参数key",
                                    "type": 1,
                                },
                                "dataIndex": "dataIndex",
                                "title": "标识",
                            },
                            {
                                "editProps": {
                                    "required": false,
                                    "type": 1,
                                },
                                "dataIndex": "unit",
                                "title": "单位",
                            },
                        ],
                    },
                }

            }
        },
        "supportToolbar":true,
        "dataMock": {}
    },
    "catgory": "layout"
};