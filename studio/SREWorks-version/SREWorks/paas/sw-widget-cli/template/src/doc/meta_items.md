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
  "appendPadding": {
    "description": "设置图表的上右下做四个方位的边距间隔，如10,0,0,10以逗号分隔",
    "title": " 边距",
    "required": false,
    "type": "string",
    "x-component": "Input",
  },
  "advancedConfig": {
    "description": "图表高级自定义配置，参考bizcharts官方配置",
    "title": "自定义配置",
    "required": false,
    "type": "string",
    "initValue": "function advancedConfig(widgetData){\n  return {}\n}",
    "x-component": "ACEVIEW_JAVASCRIPT"
  },
  "legendPosition": {
    "description": "图表的图例位置",
    "title": "图例位置",
    "required": false,
    "type": "string",
    "x-component": "Select",
    "x-component-props": {
      "options": [{ "value": "top-left", "label": "top-left" }, { "value": "top-center", "label": "top-center" }, { "value": "top-right", "label": "top-right" },
      { "value": "bottom-center", "label": "bottom-center" }, { "value": "bottom-left", "label": "bottom-left" }, { "value": "bottom-right", "label": "bottom-right" }, { "value": "left-top", "label": "left-top" }, { "value": "left-center", "label": "left-center" }, { "value": "left-bottom", "label": "left-bottom" }, { "value": "right-top", "label": "right-top" }, { "value": "right-center", "label": "right-center" }, { "value": "right-bottom", "label": "right-bottom" }],
    },
  },
  "customProps": {
    "description": "请填写自定义组件的注入数据对象",
    "title": "数据对象",
    "required": false,
    "type": "string",
    "x-component": "JSON"
  },
  "cardActions": {
    "description": "JSXrender，此处可以填写一个或一组超链接",
    "title": "更多操作",
    "required": true,
    "x-component": "Text",
    "initValue": '<a href="#">详情</a>',
    "type": "string",
  },
  "backgroundColor": {
    "description": "设置词云图背景颜色",
    "title": "背景颜色",
    "required": false,
    "type": "string",
    "x-component": "COLOR_PICKER"
  },       
   "formatList": {
    "type": "string",
    "title": "参数转换",
    "required": true,
    "description": "参数有label（参数的名称）,dataIndex(参数key),span（占据的位置，默认1列）,href（参数后变为链接形式），render（可配置自定义渲染内容）",
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
      "inputTip":"占据的位置，默认1列",
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
  }
  
}
