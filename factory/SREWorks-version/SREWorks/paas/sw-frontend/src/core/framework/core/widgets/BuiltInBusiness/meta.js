/**
 * @author caoshuaibiao
 * @date 2021/8/23 20:02
 * @Description:组件属性定义
 */
export default {
    "id": "BuiltInBusiness",
    "type": "BuiltInBusiness",
    "name": "BuiltInBusiness",
    "title": "内置业务组件",
    "info": {
        "author": {
            "name": "",
            "url": "",
        },
        "description": "内置的代码实现的业务组件。",
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
            "type": "BuiltInBusiness",
            "config": {
                "businessType": "",
                "businessConfig": {},
            },
        },
        "schema": {
            "type": "object",
            "properties": {
                "businessType": {
                    "description": "选择内置的业务组件类型",
                    "title": "业务组件",
                    "required": true,
                    "type": "string",
                    "x-component": "Select",
                    "x-component-props": {
                        "options": [
                            {"value": "DESIGNER_WORKBENCH", "label": "FlyAdmin前端开发"},
                        ],
                    }
                },
                "businessConfig": {
                    "description": "选择的业务组件的配置",
                    "title": "业务配置",
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