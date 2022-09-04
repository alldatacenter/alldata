export default {
  "id": "SwitchCard",
  "type": "SwitchCard",
  "name": "SwitchCard",
  "title": "左右结构静态组件",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "左右结构静态组件，左右布局，由图片/JSXRender组成",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"SwitchCard"
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
      "type": "SwitchCard",
      "config": {
        "message": "静态页面内容",
        "showIcon": false,
        "alertType": "success",
        "closable": false,
        "closeText": "关闭",
        "icon":"",
        "description": "辅助性文字",
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "imgPosition": {
          "description": "图片布局,图片和文字描述内容的位置布局",
          "title": "图片布局",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "initValue": "left",
          "x-component-props": {
            "options": [{"value": "left", "label": "居左"}, {"value": "right", "label": "居右"}],
          },
        },
        "backgroundImg": {
          "description": "背景图片",
          "title": "背景图片",
          "required": false,
          "type": "string",
          "x-component": "ImageUpload",
          "initValue": "",
        },
        "height": {
          "description": "区块高度",
          "title": "区块高度",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "jsxDom": {
          "description": "支持JSXrender",
          "title": "内容区",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
      },
    },
    "dataMock": {},
  },
  "catgory": "staticComp",
};