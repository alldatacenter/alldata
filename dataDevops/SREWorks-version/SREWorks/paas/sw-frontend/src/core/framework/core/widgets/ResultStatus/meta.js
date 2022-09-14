export default {
  "id": "ResultStatus",
  "type": "ResultStatus",
  "name": "ResultStatus",
  "title": "结果页",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "用于展示页面状态的页面",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass": "ResultStatus"
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
    "docs": "<a target='_blank' href='https://ant.design/components/result-cn/'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "ResultStatus",
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
        "status": {
          "description": "状态",
          "title": "状态",
          "required": false,
          "type": "string",
          "x-component": "Select",
          "x-component-props": {
            "options": [{ "value": "success", "label": "success" }, 
            { "value": "warning", "label": "warning" }, 
            { "value": "403", "label": "403" }, 
            { "value": "404", "label": "404" },
            { "value": "500", "label": "500" },
            { "value": "error", "label": "error" }, 
            { "value": "info", "label": "info" }
          ],
          },
          "initValue": "success"
        },
        "statusTitle": {
          "description": "状态描述标题",
          "title": "标题",
          "required": false,
          "type": "string",
          "x-component": "Input",
        },
        "subStatusTitle": {
          "description": "状态描述副标题",
          "title": "副标题",
          "required": false,
          "type": "string",
          "x-component": "Input",
        },
        "customIcon": {
          "description": "自定义图标区,支持JSXRener",
          "title": "自定义图标",
          "required": false,
          "type": "string",
          "x-component": "Input",
        },
        "extra": {
          "description": "结果页的操作区，支持JSXRender",
          "title": "操作区",
          "required": false,
          "type": "string",
          "x-component": "TEXTAREA"
        },
        "childrenContent": {
          "description": "结果页的内容区，支持JSXRender",
          "title": "内容区",
          "required": false,
          "type": "string",
          "x-component": "TEXTAREA"
        }
      },
    },
    "supportItemToolbar": true,
    "dataMock": {},
  },
  "catgory": "base",
};