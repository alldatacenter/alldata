export default  {
    "id": "SRESearch",
    "type": "SRESearch",
    "name": "SRESearch",
    "title": "SRESearch",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "搜索基础组件",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icon.svg'),
            "fontClass":"SRESearch"
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
        "docs":""
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "type":"SRESearch",
            "config":{
                "url": ""
            }
        },
        "schema": {
            "type": "object",
            "properties": {
                "url": {
                    "description": "填写关联推荐url",
                    "title": "关联推荐url",
                    "pattern": "",
                    "required": true,
                    "x-component": "Input",
                    "type": "string"
                },
                "keyType": {
                    "description": "检索字段key,在url内引用可以$(*)的方式使用",
                    "title": "检索字段key",
                    "pattern": "",
                    "required": true,
                    "x-component": "Input",
                    "type": "string"
                },
                "btnText": {
                    "description": "填写查询按钮标题，默认为'search'",
                    "title": "查询按钮标题",
                    "pattern": "",
                    "required": false,
                    "x-component": "Input",
                    "type": "string"
                },
                "complength": {
                    "description": "组件长度",
                    "title": "组件长度(px)",
                    "pattern": "",
                    "required": false,
                    "x-component": "Input",
                    "type": "number"
                },
                "compPadding": {
                    "description": "组件上下边距",
                    "title": "组件上下边距(px)",
                    "pattern": "",
                    "required": false,
                    "x-component": "Input",
                    "type": "number"
                },
                "needSuggest": {
                    "description": "是否需要关联推荐",
                    "title": "是否推荐",
                    "pattern": "",
                    "required": false,
                    "x-component": "Radio",
                    "type": "string",
                    "x-component-props": {
                      "options": [
                        {"value": true, "label": "是"},
                        {"value": false, "label": "否"},
                      ],
                    },
                },
            }
        },
        "dataMock": {}
    },
    "catgory": "biz"
};