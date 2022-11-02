export default {
    "id": "BizTreemapChart",
    "type": "BizTreemapChart",
    "name": "BizTreemapChart",
    "title": "矩形树图",
    "info": {
      "author": {
        "name": "",
        "url": "",
      },
      "description": "bizCharts图表库矩形树图",
      "links": [],
      "logos": {
        "large": "",
        "small": require("./icon.svg"),
        "fontClass":"BizTreemapChart"
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
      "docs": "<a target='_blank' href='https://bizcharts.taobao.com/product/BizCharts4/category/77/page/140'>组件文档地址</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
      "defaults": {
        "type": "BizTreemapChart",
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
            "type": "string",
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
            "initValue":"10,0,0,10",
            "type": "string",
            "x-component": "Input",
          },
          "chartTitle": {
            "description": "设置矩形树图的标题",
            "title": "标题",
            "required": false,
            "type": "string",
            "x-component": "Input",
            "x-component-props": {
              "placeholder": "请输标题",
            },
          },
          "isLegend": {
            "description": "默认展示图例，选择否则隐藏",
            "title": "是否展示图例",
            "required": false,
            "type": "string",
            "initValue":true,
            "x-component": "Radio",
            "x-component-props": {
              "options": [{"value": false, "label": "否"}, {"value": true, "label": "是"}],
            },
          },
          "legendPosition": {
            "description": "图表的图例位置",
            "title": "图例位置",
            "required": false,
            "type": "string",
            "x-component": "Select",
            "x-component-props": {
              "options": [{"value": "top-left", "label": "top-left"}, {"value": "top-center", "label": "top-center"},{"value": "top-right", "label": "top-right"},
              {"value": "bottom-center", "label": "bottom-center"},{"value": "bottom-left", "label": "bottom-left"},{"value": "bottom-right", "label": "bottom-right"},{"value": "left-top", "label": "left-top"},{"value": "left-center", "label": "left-center"},{"value": "left-bottom", "label": "left-bottom"},{"value": "right-top", "label": "right-top"},{"value": "right-center", "label": "right-center"},{"value": "right-bottom", "label": "right-bottom"}],
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