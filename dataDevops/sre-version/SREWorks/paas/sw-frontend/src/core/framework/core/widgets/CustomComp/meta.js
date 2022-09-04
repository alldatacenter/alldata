export default {
    "id": "CustomComp",
    "type": "CustomComp",
    "name": "CustomComp",
    "title": "自定义组件",
    "info": {
        "author": {
            "name": "",
            "url": "",
        },
        "description": "自定义组件",
        "links": [],
        "logos": {
            "large": "",
            "small": require("./icon.svg"),
            "fontClass":""
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
            "type": "CustomComp",
            "config": {
                "businessType": "",
                "customProps": {},
            },
        },
        "schema": {
            "type": "object",
            "properties": {
                "compName": {
                    "description": "自定义组件的类别",
                    "title": "组件名称",
                    "required": false,
                    "type": "string",
                    "initValue": '',
                    "x-component": "DISABLED_INPUT",
                },
                "height": {
                    "description": "请填写自定义组件的高度",
                    "title": "高度",
                    "required": false,
                    "type": "string",
                    "initValue": 240,
                    "x-component": "Input"
                },
                "customProps": {
                    "description": "请填写自定义组件的注入数据对象",
                    "title": "数据对象",
                    "required": false,
                    "type": "string",
                    "x-component": "JSON"
                },
                "compDescribtion": {
                    "description": "自定义组件的使用说明",
                    "title": "组件说明",
                    "required": false,
                    "type": "string",
                    "initValue": '',
                    "x-component": "DISABLED_TEXTAREA"
                },
            },
        },
        "dataMock": {},
    },
    "catgory": "custom",
};