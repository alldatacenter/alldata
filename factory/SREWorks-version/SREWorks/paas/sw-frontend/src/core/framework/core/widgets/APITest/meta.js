export default {
    "id": "API_TEST",
    "type": "API_TEST",
    "name": "API_TEST",
    "title": "智能服务测试",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "智能服务测试",
        "links": [],
        "logos": {
            "large": "",
            "small": require("./icon.svg"),
            "fontClass":"BuiltInBusiness"
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
            "type": "API_TEST",
            "config": {
                "businessConfig": {
                    "sceneCode":"",
                    "detectorCode":""
                },
            },
        },
        "schema": {
            "type": "object",
            "properties": {
                "businessConfig": {
                    "description": "业务字段配置",
                    "title": "业务字段配置",
                    "required": false,
                    "type": "string",
                    "x-component": "JSON"
                }
            },
        },
        "dataMock": {},
    },
    "catgory": "biz",
};