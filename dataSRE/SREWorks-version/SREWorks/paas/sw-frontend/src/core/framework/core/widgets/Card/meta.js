/**
 * Created by wangkaihua on 2021/04/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "Card",
  "type": "Card",
  "name": "Card",
  "title": "内嵌网格列表",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "一种常见的卡片内容区隔模式。",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"Card"
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
    "docs": "<a target='_blank' href='https://3x.ant.design/components/list-cn/'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "List",
      "config": {
        "flex": 6,
        "bordered": true,
        "listItem": {
          "avatar": "avatar",
          "title": "title",
          "auth": "auth",
          "date": "date",
          "description": "description",
        },
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "flex": {
          "description": "一行显示几个卡片，默认6个",
          "title": "列数",
          "x-validateType": "number",
          "required": true,
          "x-component": "Input",
          "type": "string",
        },
        "border": {
          "description": "",
          "title": "是否显示边框",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": true, "label": "是"}, {"value": false, "label": "否"}],
          },
        },
        "listItem": {
          "type": "object",
          "title": "行信息属性",
          "properties": {
            "avatar": {
              "description": "图片的字段名",
              "title": "图片信息",
              "required": false,
              "x-component": "Input",
              "type": "string",
            },
            "title": {
              "description": "标题的字段名",
              "title": "标题",
              "required": false,
              "x-component": "Input",
              "type": "string",
            },
            "description": {
              "description": "",
              "title": "描述文字的字段名",
              "required": false,
              "x-component": "Input",
              "type": "string",
            },
            "auth": {
              "description": "",
              "title": "作者的字段名",
              "required": false,
              "x-component": "Input",
              "type": "string",
            },
            "date": {
              "description": "",
              "title": "时间的字段名",
              "required": false,
              "x-component": "Input",
              "type": "string",
            },
          },
        },
      },
    },
    "supportItemToolbar": false,
    "dataMock": {},
  },
  "catgory": "base",
};