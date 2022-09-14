export default {
    "id": "BChart",
    "type": "BChart",
    "name": "BChart",
    "title": "基础条带图",
    "info": {
      "author": {
        "name": "",
        "url": "",
      },
      "description": "bizCharts图表库折线图",
      "links": [],
      "logos": {
        "large": "",
        "small": require("./icon.svg"),
        "fontClass":"BChart"
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
      "docs": "<a target='_blank' href='#/help/book/documents/ho617k.html#85-基础条带图'>组件文档地址</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
      "defaults": {
        "type": "BChart",
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
          "chartType": {
            "description": "基础条带类图表：折线图，条形图，柱状图",
            "title": "基础图表类别",
            "required": false,
            "type": "string",
            "x-component": "Select",
            "initValue": "Line",
            "x-component-props": {
              "options": [{"value": "Line", "label": "折线图"}, {"value": "Interval", "label": "柱状图"},{"value": "transposeInterval", "label": "条形图"},
              {"value": "Point", "label": "点图"}],
              "defaultValue":"Line"
            },
          },
          "theme": {
            "description": "主题",
            "title": "主题",
            "required": false,
            "type": "string",
            "initValue":'light',
            "x-component": "Radio",
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
            "initValue":'',
            "type": "string",
            "x-component": "Input",
          },
          "chartTitle": {
            "description": "设置图表的标题",
            "title": "标题",
            "required": false,
            "type": "string",
            "x-component": "Input",
          },
          "xField": {
            "description": "设置图表横坐标key，为数据返回data字段",
            "title": "x坐标轴字段",
            "required": true,
            "type": "string",
            "initValue": 'year',
            "x-component": "Input",
          },
          "yField": {
            "description": "设置图表纵坐标key，为数据返回data字段",
            "title": "y坐标轴字段",
            "required": true,
            "type": "string",
            "initValue": 'value',
            "x-component": "Input",
          },
          "seriesField": {
            "description": "设置图表分组字段",
            "title": "分组字段",
            "required": false,
            "type": "string",
            "x-component": "Input",
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
            "description": "基础条带类图表的图例位置",
            "title": "图例位置",
            "required": false,
            "type": "string",
            "x-component": "Select",
            "x-component-props": {
              "options": [{"value": "top-left", "label": "top-left"}, {"value": "top-center", "label": "top-center"},{"value": "top-right", "label": "top-right"},
              {"value": "bottom-center", "label": "bottom-center"},{"value": "bottom-left", "label": "bottom-left"},{"value": "bottom-right", "label": "bottom-right"},{"value": "left-top", "label": "left-top"},{"value": "left-center", "label": "left-center"},{"value": "left-bottom", "label": "left-bottom"},{"value": "right-top", "label": "right-top"},{"value": "right-center", "label": "right-center"},{"value": "right-bottom", "label": "right-bottom"}],
            },
          },
          "isSlider": {
            "description": "是否展示缩放条",
            "title": "缩放条",
            "required": false,
            "type": "string",
            "initValue":false,
            "x-component": "Radio",
            "x-component-props": {
              "options": [{"value": false, "label": "否"}, {"value": true, "label": "是"}],
            },
          },
          "colorType":{
            "description": "图表颜色，在制定颜色情况下默认自适应给色",
            "title": "图表颜色",
            "required": false,
            "type": "string",
            "x-component": "COLOR_PICKER"
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