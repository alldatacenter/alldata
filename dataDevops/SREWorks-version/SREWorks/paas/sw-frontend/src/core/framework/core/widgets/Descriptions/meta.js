/**
 * Created by wangkaihua on 2021/4/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default  {
  "id": "Descriptions",
  "type": "Descriptions",
  "name": "Descriptions",
  "title": "描述列表",
  "info": {
    "author": {
      "name": "",
      "url": ""
    },
    "description": "成组展示多个只读字段。",
    "links": [],
    "logos": {
      "large": "",
      "small": require('./icon.svg'),
      "fontClass":"Descriptions"
    },
    "build": {
      "time": "",
      "repo": "",
      "branch": "",
      "hash": ""
    },
    "screenshots": [],
    "updated": "",
    "version": "",
    "docs": "<a target='_blank' href='https://3x.ant.design/components/descriptions-cn/'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "config": {
        "formatList": [
          {
            "label":"名称",
            "span": 2,
            "description":"描述文字",
            "dataIndex": "key1",
            "href":"http://www.baidu.com"
          },
          {
            "label": "值2",
            "dataIndex": "key2",
            "render":""
          },
          {
            "label":"值3",
            "dataIndex": "key3",
          },
          {
            "label":"值4",
            "dataIndex": "key4",
          }
        ],
        "bordered": false,
        "colon": true,
        "layout": "horizontal",
        "labelStyle":{},
        "descriptionStyle": {
          "fontSize:": 16,
        },
        "column": {xxl: 4, xl: 3, lg: 3, md: 3, sm: 2, xs: 1},
      },
      "type":"Descriptions"
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
        "formatList": {
          "type": "string",
          "title": "参数转换",
          "required": true,
          "description": "参数有label（参数的名称）,dataIndex(参数key),span（包含列的数量，可理解为表格所占列宽比）,href（配置该参数后变为链接形式），render（可配置自定义渲染内容）",
          "x-component": "EditTable",
          "x-component-props":{
            "columns": [
              {
                "editProps": {
                  "required": true,
                  "type": 1,
                  "inputTip":"参数的名称",
                },
                "dataIndex": "label",
                "title": "名称"
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip":"参数key",
                  "type": 1
                },
                "dataIndex": "dataIndex",
                "title": "标识"
              },
              {
                "editProps": {
                  "required": false,
                  "inputTip":"ToolTip描述信息",
                  "type": 1
                },
                "dataIndex": "description",
                "title": "ToolTip描述信息"
              },
              {
                "editProps": {
                  "required": false,
                  "inputTip":"包含列的数量，默认1列",
                  "type": 1
                },
                "dataIndex": "span",
                "title": "span"
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                  "inputTip":"配置该参数后变为链接形式",
                },
                "width":170,
                "dataIndex": "href",
                "title": "跳转链接"
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                  "inputTip":"可配置自定义渲染内容",
                },
                "width":170,
                "dataIndex": "render",
                "title": "render"
              },
            ]
          }
        },
        "describeTitle": {
          "description": "页面标题",
          "title": "页面标题",
          "required": false,
          "x-component": "Input",
          "type": "string"
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
        "layout": {
          "description": "描述布局",
          "title": "label布局",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": "horizontal", "label": "水平"}, {"value": "vertical", "label": "垂直"}],
          },
        },
        "colon": {
          "description": "配置 Descriptions.Item 的 colon 的默认值",
          "title": "冒号",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "x-component-props": {
            "options": [{"value": true, "label": "显示"}, {"value": false, "label": "不显示"}],
          },
        },
        "labelStyle":{
          "description": "",
          "title": "标题样式",
          "required": false,
          "x-component": "JSON",
          "type": "string",
        },
        "descriptionStyle":{
          "description": "",
          "title": "内容样式",
          "required": false,
          "x-component": "JSON",
          "type": "string",
        }
      }
    },
    "supportItemToolbar":true,
    "dataMock": {}
  },
  "catgory": "base"
};