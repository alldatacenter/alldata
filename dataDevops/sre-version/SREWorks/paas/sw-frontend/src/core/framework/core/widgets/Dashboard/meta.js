export default {
    "id": "Dashboard",
    "type": "Dashboard",
    "name": "Dashboard",
    "title": "仪表盘",
    "info": {
      "author": {
        "name": "",
        "url": "",
      },
      "description": "bizCharts图表库仪表盘",
      "links": [],
      "logos": {
        "large": "",
        "small": require("./icon.svg"),
        "fontClass":"Dashboard"
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
        "type": "Dashboard",
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
          "theme": {
            "description": "主题",
            "title": "主题",
            "required": false,
            "type": "string",
            "x-component": "Radio",
            "initValue": "light",
            "x-component-props": {
              "options": [{"value": "light", "label": "本白"}, {"value": "dark", "label": "亮黑"}],
            },
          },
          "height": {
            "description": "高度",
            "title": "高度",
            "required": false,
            "initValue":400,
            "type": "string",
            "x-component": "INPUT_NUMBER",
          },
          "width": {
            "description": "宽度",
            "title": "宽度",
            "required": false,
            "initValue":400,
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
            "initValue":"10,0,0,10",
            "type": "string",
            "x-component": "Input",
          },
          "chartTitle": {
            "description": "设置仪表盘的标题",
            "title": "标题",
            "required": false,
            "type": "string",
            "x-component": "Input",
            "x-component-props": {
              "placeholder": "请输入标题",
            },
          },
          "chartContent": {
            "description": "设置仪表盘的数值说明",
            "title": "数据说明",
            "required": false,
            "type": "string",
            "x-component": "Input",
            "x-component-props": {
              "placeholder": "请输入数据说明",
            },
          },
          "minNum": {
            "description": "设置仪表盘刻度范围最小值",
            "title": "刻度最小值",
            "required": false,
            "type": "string",
            "x-component": "INPUT_NUMBER",
            "validateType": "number",
            "initValue": 0,
            "x-component-props": {
              "placeholder": "请输入最小刻度数值如：0",
            },
          },
          "maxNum": {
            "description": "设置仪表盘刻度范围最大值",
            "title": "刻度最大值",
            "required": false,
            "type": "string",
            "x-component": "INPUT_NUMBER",
            "validateType": "number",
            "initValue": 100,
            "x-component-props": {
              "placeholder": "请输入最小刻度数值如：100",
            },
          },
          "advancedConfig":{
            "description": "图表高级自定义配置，参考bizcharts官方配置",
            "title": "自定义配置",
            "required": false,
            "type": "string",
            "initValue":"function advancedConfig(widgetData){\n  return {}\n}",
            "x-component": "ACEVIEW_JAVASCRIPT"
          }
        },
      },
      "supportItemToolbar":true,
      "dataMock": {},
    },
    "catgory": "charts",
  };