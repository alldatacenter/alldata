export default {
    "id": "JsonEditor",
    "type": "JsonEditor",
    "name": "JsonEditor",
    "title": "编辑器文本展示",
    "info": {
      "author": {
        "name": "",
        "url": "",
      },
      "description": "用于常见格式的文本查看",
      "links": [],
      "logos": {
        "large": "",
        "small": require("./icon.svg"),
        "fontClass":"JsonEditor"
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
      "docs": "<a target='_blank' href='https://www.npmjs.com/package/react-ace'>组件文档地址</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
      "defaults": {
        "type": "JsonEditor",
        "config": {
        "url": ""
        },
      },
      "schema": {
        "type": "object",
        "properties": {
          "langType": {
            "description": "",
            "title": "文本格式类型",
            "required": false,
            "type": "string",
            "x-component": "Radio",
            "defaultValue": 'json',
            "x-component-props": {
            "options": [{"value": "javascript", "label": "javascript"}, {"value": "python", "label": "python"},{"value": "yaml", "label": "yaml"},{"value": "json", "label": "json"},{"value": "java", "label": "Java"},{"value": "text", "label": "text"}],
          },
        },
          "readOnly": {
            "description": "",
            "title": "是否只读",
            "required": false,
            "type": "string",
            "x-component": "Radio",
            "x-component-props":{
              "options": [{"value": true, "label": "是"}, {"value": false, "label": "否"},],
            }
          },
          "heightNum": {
            "description": "填充数字类型，例如500，600，等根据需求衡量",
            "title": "文本容器高度",
            "required": false,
            "type": "number",
            "validateType":'number',
            "x-component": "Input",
          },
          "url": {
            "description": "",
            "title": "React-ace链接地址",
            "required": false,
            "x-component": "Text",
            "type": "string",
          },
        },
      },
      "supportItemToolbar":false,
      "dataMock": {},
    },
    "catgory": "base",
  };