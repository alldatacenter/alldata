/**
 * Created by wangkaihua on 2021/04/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "Alert",
  "type": "Alert",
  "name": "Alert",
  "title": "警告提示",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "警告提示，展现需要关注的信息。",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"Alert",
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
      "type": "Alert",
      "config": {
        "message": "警告提示内容",
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
        "message": {
          "description": "警告提示内容",
          "title": "警告提示内容",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "description": {
          "description": "警告提示的辅助性文字介绍",
          "title": "辅助性文字",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "showIcon": {
          "description": "边框",
          "title": "边框",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": true, "label": "显示"}, {"value": false, "label": "不显示"}],
          },
        },
        "icon":{
          "description": "showIcon 为 true 时有效",
          "title": "自定义图标",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "closable": {
          "description": "关闭按钮",
          "title": "关闭按钮",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": true, "label": "显示"}, {"value": false, "label": "不显示"}],
          },
        },
        "closeText":{
          "description": "自定义关闭按钮",
          "title": "关闭按钮文字",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "alertType": {
          "description": "info，banner 模式下默认值为 warning",
          "title": "类型",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": "success", "label": "success"},
              {"value": "info", "label": "info"},
              {"value": "warning", "label": "warning"},
              {"value": "error", "label": "error"},
            ],
          },
        },
      },
    },
    "dataMock": {},
  },
  "catgory": "base",
};