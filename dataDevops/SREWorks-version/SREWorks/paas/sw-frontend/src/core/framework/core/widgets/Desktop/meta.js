export default {
    "id": "OPS_DESKTOP",
    "type": "OPS_DESKTOP",
    "name": "OPS_DESKTOP",
    "title": "桌面管理",
    "info": {
        "author": {
            "name": "",
            "url": "",
        },
        "description": "桌面管理",
        "links": [],
        "logos": {
            "large": "",
            "small": require("./icon.svg"),
            "fontClass": "BuiltInBusiness"
        },
        "build": {
            "time": "",
            "repo": "",
            "branch": "",
            "hash": "",
        },
        "screenshots": [],
        "updated": "",
        "version": "",
        "docs": "<a target='_blank' href='https://3x.ant.design/components/alert-cn/'>组件文档地址</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "type": "Desktop",
            "config": {
                "searchConfig": true
            },
        },
        "schema": {
            "type": "object",
            "properties": {
                "searchConfig": {
                    "description": "搜索框可根据业务需求配置是否展示",
                    "title": "搜索框",
                    "required": false,
                    "type": "string",
                    "x-component": "Radio",
                    "initValue":true,
                    "x-component-props": {
                        "options": [
                            { "value": false, "label": "无" },
                            { "value": true, "label": "有" }
                        ],
                    },
                }
            },
        },
        "dataMock": {},
    },
    "catgory": "biz",
};