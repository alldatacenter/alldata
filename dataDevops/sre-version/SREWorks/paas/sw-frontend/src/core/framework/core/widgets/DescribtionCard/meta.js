/**
 * Created by wangkaihua on 2021/04/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "DescribtionCard",
  "type": "DescribtionCard",
  "name": "DescribtionCard",
  "title": "主题介绍组件",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "主题介绍组件，用于页面中部描述性说明",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"DescribtionCard"
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
      "type": "DescribtionCard",
      "config": {
        "message": "专题介绍组件",
        "showIcon": false,
        "alertType": "success",
        "closable": false,
        "closeText": "关闭",
        "icon":"",
        "description": "专题介绍组件",
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "describeLayout": {
          "description": "专题描述卡片的布局",
          "title": "布局类型",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "initValue": 'cardLayout',
          "x-component-props": {
            "options": [{"value": 'cardLayout', "label": "布局一"}, {"value": 'normalLayout', "label": "布局二"}],
          },
        },
        "cardTitle": {
          "description": "栏目标题",
          "title": "标题",
          "required": false,
          "type": "string",
          "x-component": "Input",
        },
        "cardContent": {
          "description": "栏目正文描述内容",
          "title": "描述",
          "required": true,
          "x-component": "Text",
          "type": "string",
        },
        "cardActions": {
          "description": "JSXrender，此处可以填写一个或一组超链接",
          "title": "更多操作",
          "required": true,
          "x-component": "Text",
          "initValue": '<a href="#">详情</a>',
          "type": "string",
        }
      },
    },
    "dataMock": {},
  },
  "catgory": "staticComp",
};