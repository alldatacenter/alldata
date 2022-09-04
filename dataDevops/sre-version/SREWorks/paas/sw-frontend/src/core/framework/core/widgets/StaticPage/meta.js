/**
 * Created by wangkaihua on 2021/04/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "StaticPage",
  "type": "StaticPage",
  "name": "StaticPage",
  "title": "静态页面",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "静态页面，自由定义展示信息",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"StaticPage"
      
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
      "type": "StaticPage",
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