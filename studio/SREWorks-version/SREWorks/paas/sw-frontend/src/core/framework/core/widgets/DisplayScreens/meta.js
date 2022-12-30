export default {
    "id": "DisplayScreens",
    "type": "DisplayScreens",
    "name": "DisplayScreens",
    "title": "大屏组件",
    "info": {
      "author": {
        "name": "",
        "url": "",
      },
      "description": "大屏组件适用于图表组的大屏布局展示",
      "links": [],
      "logos": {
        "large": "",
        "small": require("./icon.svg"),
        "fontClass":"DisplayScreens"
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
      "docs": "<a target='_blank' href='https://bizcharts.taobao.com/product/BizCharts4/category/77/page/143'>组件文档地址</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
      "defaults": {
        "type": "DisplayScreens",
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
          "screenTitle": {
            "description": "看板标题",
            "title": "看板标题",
            "required": false,
            "type": "string",
            "x-component": "Input",
            "initValue": "SREWorks大屏",
          },
          "backgroundImg": {
            "description": "背景图片",
            "title": "背景图片",
            "required": false,
            "type": "string",
            "x-component": "ImageUpload",
            "initValue": "",
          },
          "displayType": {
            "description": "布局",
            "title": "布局",
            "required": false,
            "type": "string",
            "x-component": "Radio",
            "initValue": "displayOne",
            "x-component-props": {
              "options": [{"value": "displayOne", "label": "布局一"}, 
              {"value": "displayTwo", "label": "布局二"},
              // {"value": "displayThree", "label": "布局三"}
              ],
            },
          },
        },
      },
      "supportItemToolbar":false,
      "dataMock": {},
    },
    "catgory": "layout",
  };
