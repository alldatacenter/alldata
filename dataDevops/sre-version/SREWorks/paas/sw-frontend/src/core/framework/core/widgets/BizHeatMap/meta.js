export default {
    "id": "BizHeatMap",
    "type": "BizHeatMap",
    "name": "BizHeatMap",
    "title": "热力图",
    "info": {
      "author": {
        "name": "",
        "url": "",
      },
      "description": "bizCharts图表库热力图",
      "links": [],
      "logos": {
        "large": "",
        "small": require("./icon.svg"),
        "fontClass":"BizHeatMap"
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
      "docs": "<a target='_blank' href='https://bizcharts.taobao.com/product/BizCharts4/category/77/page/138'>组件文档地址</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
      "defaults": {
        "type": "BizHeatMap",
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
            "x-component-props": {
              "options": [{"value": "light", "label": "本白"}, {"value": "dark", "label": "亮黑"}],
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
            "x-component":"INPUT_NUMBER",
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
          "xField": {
            "description": " 色块形状在 x 方向位置映射对应的数据字段名，一般对应一个分类字段",
            "title": "x方向字段",
            "required": true,
            "type": "string",
            "x-component": "Input",
          },
          "yField": {
            "description": "色块形状在 y 方向位置映射所对应的数据字段名，一般对应一个分类字段",
            "title": "y方向字段",
            "required": true,
            "type": "string",
            "x-component": "Input",
          },
          "colorField": {
            "description": " 色块颜色映射对应的数据字段名，一般对应一个连续字段",
            "title": "分色字段",
            "required": true,
            "type": "string",
            "x-component": "Input",
          },
          "sizeField": {
            "description": "指定色块形状大小映射的字段，要求必须为一个连续字段",
            "title": "热力面积字段",
            "required": true,
            "type": "string",
            "x-component": "Input",
          },
          "colors": {
            "description": "指定色块图颜色映射的色带颜色，数值中的值为色带节点的色值。 默认配置： 采用 theme 中的配色，如'#295599', '#3e94c0', '#78c6d0', '#b4d9e4', '#fffef0', '#f9cdac', '#ec7d92',以逗号分隔",
            "title": "颜色",
            "required": true,
            "type": "string",
            "x-component": "Input",
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