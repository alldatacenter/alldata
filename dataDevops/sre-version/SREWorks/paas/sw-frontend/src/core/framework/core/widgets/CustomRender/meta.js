/**
 * Created by caoshuaibiao on 2020/12/21.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义 
 */
export default  {
    "id": "CustomRender",
    "type": "CustomRender",
    "name": "CustomRender",
    "title": "JSX渲染器",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "jsx渲染块",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icon.svg'),
            "fontClass":"CustomRender"
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
        "docs":"### 组件MarkDown文档 <br/><div><span>html区域</span><code>json</code></div>"
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "type":"CustomRender",
            "config":{
                "renderPlaceholder": "填写标准的JSX"
            }
        },
        "schema": {
            "type": "object",
            "properties": {
                "jsxDom": {
                    "description": "填写标准的JSX",
                    "title": "JSX模板",
                    "pattern": "[a-z]",
                    "required": false,
                    "x-component": "TEXTAREA",
                    "type": "string"
                }
            }
        },
        "dataMock": {}
    },
    "catgory": "base"
};