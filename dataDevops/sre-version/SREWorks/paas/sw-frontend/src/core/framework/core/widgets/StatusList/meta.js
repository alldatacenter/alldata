/**
 * Created by wangkaihua on 2021/05/17.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "StatusList",
  "type": "StatusList",
  "name": "StatusList",
  "title": "圆圈状态列表",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "用圆圈形式展示数据当前状态",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"StatusList"
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
    "docs": "",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "StatusList",
      "config": {
        "warningExp": "",
        "successExp": "",
        "processExp": "",
        "defaultExp": "",
        "toolTip": "",
        "background": "#000",
        "href": "",
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        // "background": {
        //   "description": "默认黑色",
        //   "title": "背景颜色",
        //   "required": false,
        //   "type": "string",
        //   "x-component": "Radio",
        //   "x-component-props": {
        //     "options": [{"value": "#000", "label": "黑色"}, {"value": "#fff", "label": "白色"}],
        //   },
        // },
        "warningExp": {
          "description": "eg:row.status === 'error'",
          "title": "警告表达式",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "successExp": {
          "description": "eg:row.status === 'success'",
          "title": "成功表达式",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "processExp": {
          "description": "eg:row.status === 'process'",
          "title": "进程中表达式",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "defaultExp": {
          "description": "",
          "title": "默认表达式",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "href": {
          "description": "支持动态取值$(row.参数名)",
          "title": "跳转链接",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "toolTip": {
          "description": "填写提示信息的参数名",
          "title": "提示信息",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
      },
    },
    "supportItemToolbar": false,
    "dataMock": {},
  },
  "catgory": "base",
};