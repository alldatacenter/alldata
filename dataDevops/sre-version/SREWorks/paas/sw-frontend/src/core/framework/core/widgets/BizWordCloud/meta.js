export default {
  "id": "BizWordCloud",
  "type": "BizWordCloud",
  "name": "BizWordCloud",
  "title": "词云图",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "bizCharts图表库词云图",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass": "BizWordCloud"
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
    "docs": "<a target='_blank' href='https://bizcharts.taobao.com/product/BizCharts4/category/77/page/145'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "BizWordCloud",
      "config": {
        "size": "small",
        "header": "",
        "footer": "",
        "content": "内容不必填",
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
        "theme": {
          "description": "主题",
          "title": "主题",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{ "value": "light", "label": "本白" }, { "value": "dark", "label": "亮黑" }],
          },
        },
        "height": {
          "description": "高度",
          "title": "高度",
          "required": false,
          "type": "string",
          "initValue": 300,
          "x-component": "INPUT_NUMBER",
        },
        "width": {
          "description": "宽度",
          "title": "宽度",
          "required": false,
          "type": "string",
          "x-component": "INPUT_NUMBER",
        },
        "period": {
          "description": "数据刷新周期(毫秒)",
          "title": "刷新周期(毫秒)",
          "required": false,
          "type": "string",
          "x-component": "INPUT_NUMBER",
        },
        "appendPadding": {
          "description": "设置图表的上右下做四个方位的边距间隔，如10,0,0,10以逗号分隔",
          "title": " 边距",
          "required": false,
          "type": "string",
          "x-component": "Input",
        },
        "backgroundColor": {
          "description": "设置词云图背景颜色",
          "title": "背景颜色",
          "required": false,
          "type": "string",
          "x-component": "COLOR_PICKER"
        },
        "randomNum": {
          "description": "自定义所使用的随机函数，其值可以是一个 [0, 1) 区间中的值，当值为0时，居左上角显示；当值为0.5时居中显示；当值为1时居右下角显示。 默认配置： 默认使用的是浏览器内置的 Math.random，也就是每次渲染，单词的位置都不一样。",
          "title": "布局位置",
          "required": false,
          "type": "string",
          "x-component": "INPUT_NUMBER",
        },
        "advancedConfig": {
          "description": "图表高级自定义配置，参考bizcharts官方配置",
          "title": "自定义配置",
          "required": false,
          "type": "string",
          "initValue": "function advancedConfig(widgetData){\n  return {}\n}",
          "x-component": "ACEVIEW_JAVASCRIPT"
        }
      },
    },
    "supportItemToolbar": true,
    "dataMock": {},
  },
  "catgory": "charts",
};