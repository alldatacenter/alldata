/**
 * Created by wangkaihua on 2021/05/17.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "StatisticCard",
  "type": "StatisticCard",
  "name": "StatisticCard",
  "title": "指标卡",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "指标卡结合统计数值用于展示某主题的核心指标",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"StatisticCard"
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
    "docs": "数据可以直接使用$(参数名)获取",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "StatisticCard",
      "config": {
        "chartTitle": "指标名称",
        "total": "100000",
        "totalPrefix": "¥",
        "cStyle": {width: 268},
        "mainList": [
          {
            "label": "累计注册数",
            "description": "",
            "dataIndex": "key1",
            "unit": "万",
          },
          {
            "label": "本月注册数",
            "dataIndex": "key2",
            "unit": "万",
          },
        ],
        "footerList": [
          {
            "label": "累计注册数",
            "description": "",
            "dataIndex": "key1",
            "unit": "万",
          },
          {
            "label": "本月注册数",
            "dataIndex": "key2",
            "unit": "万",
          },
        ],
        "chartConfig": {
          "type": "TinyLineChart",
          "autoFit": false,
          "height": 80,
          "width": 220,
          "xField": "year",
          "yField": "value",
          "extraConfig": {},
        },
        "tooltip": "提示文案",
        "isOnlyChart": false,
        "chartData": [
          {year: "1991", value: 3},
          {year: "1992", value: 4},
          {year: "1993", value: 3.5},
          {year: "1994", value: 5},
          {year: "1995", value: 4.9},
          {year: "1996", value: 6},
          {year: "1997", value: 7},
          {year: "1998", value: 9},
          {year: "1999", value: 13},
        ],
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "chartTitle": {
          "description": "",
          "title": "指标名称",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "tooltip": {
          "description": "",
          "title": "提示文案",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "total": {
          "description": "",
          "title": "总数",
          "required": true,
          "x-component": "Input",
          "type": "string",
        },
        "totalPrefix": {
          "description": "",
          "title": "总数单位",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "cStyle": {
          "description": "",
          "title": "卡片样式",
          "required": false,
          "x-component": "JSON",
          "type": "string",
        },
        "isOnlyChart": {
          "description": "",
          "title": "仅显示图表",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": false, "label": "否"}, {"value": true, "label": "是"}],
          },
        },
        "chartConfig": {
          "type": "object",
          "title": "行信息属性",
          "properties": {
            "type": {
              "autoFit": false,
              "width": "",
              "description": "",
              "title": "图表类型",
              "required": false,
              "x-component": "Radio",
              "x-component-props": {
                "options": [
                  {"value": "TinyLineChart", "label": "折线图"},
                  {"value": "TinyAreaChart", "label": "面积图"},
                  {"value": "ColumnChart", "label": "柱状图"},
                  {"value": "ProgressChart", "label": "进度条"},
                  {"value": "LiquidChart", "label": "水波图"},
                ],
              },
              "type": "string",
            },
            "height": {
              "description": "",
              "title": "高度",
              "x-validateType": "number",
              "required": true,
              "x-component": "Input",
              "type": "string",
            },
            "width": {
              "description": "",
              "title": "宽度",
              "required": true,
              "x-validateType": "number",
              "x-component": "Input",
              "type": "string",
            },
            "min": {
              "description": "",
              "title": "最小值",
              "required": true,
              "x-visibleExp": "config.chartConfig.type === 'LiquidChart'",
              "x-validateType": "number",
              "x-component": "Input",
              "type": "string",
            },
            "max": {
              "description": "",
              "title": "最大值",
              "required": true,
              "x-visibleExp": "config.chartConfig.type === 'LiquidChart'",
              "x-validateType": "number",
              "x-component": "Input",
              "type": "string",
            },
            "chartValue": {
              "description": "",
              "title": "当前值",
              "required": true,
              "x-visibleExp": "config.chartConfig.type === 'LiquidChart'",
              "x-validateType": "number",
              "x-component": "Input",
              "type": "string",
            },
            "percent": {
              "description": "值域为 [0,1]",
              "title": "进度百分比",
              "required": true,
              "x-visibleExp": "config.chartConfig.type === 'ProgressChart'",
              "x-component": "Input",
              "type": "string",
            },
            "xField": {
              "description": "图表在 x 方向对应的数据字段名，一般对应一个离散字段。",
              "title": "x轴对应字段名",
              "required": false,
              "x-visibleExp": "config.chartConfig.type === 'TinyLineChart' || config.chartConfig.type === 'TinyAreaChart' " +
                "|| config.chartConfig.type === 'ColumnChart'",
              "x-component": "Input",
              "type": "string",
            },
            "yField": {
              "description": "图表在 y 方向对应的数据字段名，一般对应一个离散字段。",
              "title": "y轴对应字段名",
              "required": false,
              "x-visibleExp": "config.chartConfig.type === 'TinyLineChart' || config.chartConfig.type === 'TinyAreaChart'" +
                "|| config.chartConfig.type === 'ColumnChart'",
              "x-component": "Input",
              "type": "string",
            },
            "extraConfig": {
              "description": "",
              "title": "额外自定义配置",
              "required": false,
              "x-component": "JSON",
              "type": "string",
            },
          },
        },
        "chartData": {
          "description": "",
          "title": "图表数据",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "mainList": {
          "type": "string",
          "title": "主区域统计数值",
          "required": false,
          "description": "参数有label（参数的名称）,dataIndex(参数key), unit(参数的单位)",
          "x-component": "EditTable",
          "x-component-props": {
            "columns": [
              {
                "editProps": {
                  "required": true,
                  "type": 1,
                  "inputTip": "参数的名称",
                },
                "dataIndex": "label",
                "title": "名称",
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "参数key",
                  "type": 1,
                },
                "dataIndex": "dataIndex",
                "title": "标识",
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                },
                "dataIndex": "unit",
                "title": "单位",
              },
            ],
          },
        },
        "footerList": {
          "type": "string",
          "title": "主区域统计数值",
          "required": false,
          "description": "参数有label（参数的名称）,dataIndex(参数key), unit(参数的单位)",
          "x-component": "EditTable",
          "x-component-props": {
            "columns": [
              {
                "editProps": {
                  "required": true,
                  "type": 1,
                  "inputTip": "参数的名称",
                },
                "dataIndex": "label",
                "title": "名称",
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "参数key",
                  "type": 1,
                },
                "dataIndex": "dataIndex",
                "title": "标识",
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                },
                "dataIndex": "unit",
                "title": "单位",
              },
            ],
          },
        },
      },
    },
    "supportItemToolbar": false,
    "dataMock": {},
  },
  "catgory": "base",
};