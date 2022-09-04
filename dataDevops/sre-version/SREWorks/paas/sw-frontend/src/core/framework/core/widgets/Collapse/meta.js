/**
 * Created by wangkaihua on 2021/06/28.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "Collapse",
  "type": "Collapse",
  "name": "Collapse",
  "title": "折叠面板",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "提供平级的区域将大块内容进行收纳和展现，保持界面整洁。",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"Collapse"
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
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "config": {
        "title": "页面的主题标题",
        "description": "页面的其他描述信息",
        "expandIconPosition":"",
        "accordion": false,
        "bordered": true,
        "panes": [{
          "title": "tab1",
          "key": "tab1",
          "icon": "",
          "customPanelStyle":{}
        }, {
          "title": "tab2",
          "key": "tab2",
          "customPanelStyle":{}
        }],
      },
      "type": "PageHeaderLayout",
    },
    "schema": {
      "type": "object",
      "properties": {
        "size": {
          "description": "大小，提供 large default 和 small 三种大小",
          "title": " 大小",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": "large", "label": "大"},
              {"value": "default", "label": "默认"},
              {"value": "small", "label": "小"},
            ],
          },
        },
        "accordion":{
          "description": "手风琴，每次只打开一个 tab",
          "title": " 手风琴模式",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": false, "label": "否"},
              {"value": true, "label": "是"},
            ],
          },
        },
        "bordered":{
          "description": "",
          "title": " 带边框",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": false, "label": "否"},
              {"value": true, "label": "是"},
            ],
          },
        },
        "expandIconPosition": {
          "description": "设置图标位置： left, right",
          "title": " 图标位置",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": "left", "label": "左"},
              {"value": "right", "label": "右"}],
          },
        },
        "panes": {
          "type": "string",
          "title": "选项卡内容设置",
          "required": false,
          "description": "",
          "x-component": "EditTable",
          "x-component-props": {
            "columns": [
              {
                "editProps": {
                  "required": true,
                  "type": 1,
                },
                "dataIndex": "title",
                "title": "选项卡头显示文字",
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "标识",
                  "type": 1,
                },
                "dataIndex": "key",
                "title": "标识",
              },
              {
                "editProps": {
                  "required": false,
                  "inputTip": "",
                  "type": 1,
                },
                "dataIndex": "icon",
                "title": "图标",
              },
              {
                "editProps":{
                  "inputTip": " 例 __app_id__ === sreworks",
                  "type": 1,
                },
                "dataIndex": "disabledExp",
                "title": "隐藏表达式",
              },
              {
                "editProps":{
                  "inputTip": " 例 __app_id__ === sreworks",
                  "type": 1,
                },
                "title": "是否显示箭头表达式",
                "dataIndex":"showArrowExp"
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "",
                  "optionValues": "$(__select_blocks__)",
                  "type": 3,
                },
                "dataIndex": "block",
                "title": "区块",
              }
            ],
          },
        },

      },
    },
    "dataMock": {},
  },
  "catgory": "layout",
};