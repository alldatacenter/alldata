/**
 * Created by wangkaihua on 2021/04/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "List",
  "type": "List",
  "name": "List",
  "title": "复杂列表",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "最基础的列表展示，可承载文字、列表、图片、段落，常用于后台数据展示页面。",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"List"
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
    "docs": "<a target='_blank' href='#/help/book/documents/ho617k.html#81-复杂列表'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "List",
      "config": {
        "size": "small",
        "header": "",
        "footer": "",
        "content":"内容不必填",
        "bordered": false,
        "split": true,
        "itemLayout": "horizontal",
        "listItem": {
          "avatar": "icon",
          "title": "title",
          "description": "description"
        },
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "minHeight": {
          "type": "string",
          "description": "数字类型,单位(px)",
          "title": "组件最小高度",
          "required": false,
          "initValue":50,
          "x-validateType": "number",
          "x-component": "Input",
        },
        "size": {
          "description": "列表尺寸",
          "title": "列表尺寸",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": "default", "label": "默认"}, {"value": "middle", "label": "中等"}, {
              "value": "small",
              "label": "小",
            }],
          },
        },
        "bordered": {
          "description": "边框",
          "title": "边框",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": true, "label": "显示"}, {"value": false, "label": "不显示"}],
          },
        },
        "split": {
          "description": "分割线",
          "title": "分割线",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": true, "label": "显示"}, {"value": false, "label": "不显示"}],
          },
        },
        "itemLayout": {
          "description": "设置 List.Item 布局, 设置成 vertical 则竖直样式显示, 默认横排",
          "title": " 布局",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": "horizontal", "label": "横排"}, {"value": "vertical", "label": "竖排"}],
          },
        },
        "header": {
          "description": "支持自定义",
          "title": "头部信息",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "footer": {
          "description": "支持自定义",
          "title": "底部信息",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "content": {
          "description": "支持自定义",
          "title": "内容",
          "required": false,
          "x-component": "Text",
          "type": "string",
        },
        "emptyText": {
          "description": "支持空数据自定义文案",
          "title": "空数据文案",
          "required": false,
          "x-component": "Text",
          "initValue":"",
          "type": "string",
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
              "description": "描述信息的字段名",
              "title": "描述信息",
              "required": false,
              "x-component": "Input",
              "type": "string",
            }
          },
        },
      },
    },
    "supportItemToolbar":true,
    "supportToolbar":true,
    "dataMock": {},
  },
  "catgory": "base",
};