/**
 * @author caoshuaibiao
 * @date 2021/7/9 16:35
 * @Description:组件定义描述
 */
export default  {
    "id": "Iframe",
    "type": "Iframe",
    "name": "Iframe",
    "title": "Iframe",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "iframe嵌入其他站点页面",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icon.svg'),
            "fontClass":"Iframe"
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
            "type":"Iframe",
            "config":{
                "url": ""
            }
        },
        "schema": {
            "type": "object",
            "properties": {
                "url": {
                    "description": "填写嵌入页面url",
                    "title": "页面URL",
                    "pattern": "",
                    "required": true,
                    "x-component": "Input",
                    "type": "string"
                },
                "customHeight": {
                    "description": "iframe高度，无配置高度则高度自适应充满视窗",
                    "title": "自定义高度(px)",
                    "pattern": "",
                    "required": false,
                    "x-component": "Input",
                    "type": "string"
                }
            }
        },
        "dataMock": {}
    },
    "catgory": "base"
};