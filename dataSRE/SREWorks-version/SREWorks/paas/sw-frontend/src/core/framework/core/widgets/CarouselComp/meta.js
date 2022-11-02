export default {
  "id": "CarouselComp",
  "type": "CarouselComp",
  "name": "CarouselComp",
  "title": "走马灯",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "走马灯，用于页面中部描述性说明",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"CarouselComp"
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
      "type": "CarouselComp",
      "config": {
        "message": "动态专题介绍组件",
        "showIcon": false,
        "alertType": "success",
        "closable": false,
        "closeText": "关闭",
        "icon":"",
        "description": "动态专题介绍组件",
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "backgroundImgList": {
          "description": "多背景图片,每次可上传多张，上传操作为覆盖更新操作,增删操作即:重新上传",
          "title": "多背景图片",
          "required": false,
          "type": "string",
          "x-component": "IMAGE_UPLOAD_MULTI",
          "initValue": "",
        },
        "carouselHeight": {
          "description": "高度,百分比或者数字,如 30%,200,",
          "title": "高度",
          "required": false,
          "type": "string",
          "initValue":'100%',
          "x-component": "Input",
        },
        "carouselWidth": {
          "description": "宽度，百分比或者数字,如 100%,200",
          "title": "宽度",
          "required": false,
          "initValue":'100%',
          "type": "string",
          "x-component": "Input",
        },
        "autoPlay": {
          "description": "是否自动切换",
            "title": "自动切换",
            "required": false,
            "type": "string",
            "initValue":true,
            "x-component": "Radio",
            "x-component-props": {
              "options": [{"value": false, "label": "否"}, {"value": true, "label": "是"}],
            },
        },
        "dots": {
          "description": "是否展示指示点",
          "title": "是否展示指示点",
          "required": false,
          "initValue":true,
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": false, "label": "否"}, {"value": true, "label": "是"}],
          },
        },
        "dotPosition": {
          "description": "指示点位置",
          "title": "指示点位置",
          "required": false,
          "type": "string",
          "x-component": "Select",
          "x-component-props": {
            "options": [{"value": "top", "label": "top"}, {"value": "bottom", "label": "bottom"},
            {"value": "left", "label": "left"},{"value": "right", "label": "right"}],
          },
        },
        "links": {
          "description": "图片超链接，添加顺序同图片顺序",
          "title": "图片超链接",
          "required": false,
          "type": "string",
          "x-component": "HANDLE_TAG",
        },
        "easing": {
          "description": "动画效果",
          "title": "动画效果",
          "required": false,
          "type": "string",
          "initValue":"linear",
          "x-component": "Input",
        },
        "effect": {
          "description": "动画效果函数",
          "title": "动画效果函数",
          "required": false,
          "type": "string",
          "x-component": "Select",
          "x-component-props": {
            "options": [{"value": "scrollx", "label": "scrollx"}, {"value": "fade", "label": "fade"}],
          },
        }
      },
    },
    "supportItemToolbar":false,
    "dataMock": {},
  },
  "catgory": "staticComp",
};