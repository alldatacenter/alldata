/**
 * Created by wangkaihua on 2021/6/8.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "Tabs",
  "type": "Tabs",
  "name": "Tabs",
  "title": "Tabs布局",
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
      "fontClass":"Tabs"
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
        "bgUrl": "",
        "topMenuType": "menu",
        "wrapper": "default",
        "tabType":"default",
        "tabPanes": [{
          "tab": "tab1",
          "key": "tab1",
          "icon": "",
        }, {
          "tab": "tab2",
          "key": "tab2",
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
        "tabType":{
          "description": "",
          "title": "tab 类型",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": "default", "label": "默认"},
              {"value": "capsule", "label": "胶囊"},
            ],
          },
        },
        "tabPosition": {
          "description": "页签位置，可选值有 top right bottom left",
          "title": " 页签位置",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [
              {"value": "top", "label": "上"},
              {"value": "bottom", "label": "下"},
              {"value": "left", "label": "左"},
              {"value": "right", "label": "右"}],
          },
        },
        "centered": {
          "description": "",
          "title": " 是否居中显示",
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
        "tabPanes": {
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
                "dataIndex": "tab",
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
                  "inputTip": " 例 __app_id__ === tesla",
                  "type": 1,
                },
                "dataIndex": "disabledExp",
                "title": "隐藏表达式",
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
              },
            ],
          },
        },

      },
    },
    "dataMock": {},
    "supportToolbar":true,
  },
  "catgory": "layout",
};