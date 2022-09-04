export default {
  "id": "MarkdownComp",
  "type": "MarkdownComp",
  "name": "MarkdownComp",
  "title": "Markdown",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "Markdown组件",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"MarkdownComp"
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
    "docs": "<a target='_blank' href='https://3x.ant.design/components/alert-cn/'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "MarkdownComp",
      "config": {
        "message": "静态页面内容",
        "showIcon": false,
        "alertType": "success",
        "closable": false,
        "closeText": "关闭",
        "icon":"",
        "description": "辅助性文字",
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "paddingNumTop": {
          "description": "上边距",
          "title": "上边距",
          "required": false,
          "initValue":20,
          "x-component": "INPUT_NUMBER",
          "type": "string",
        },
        "paddingNumLeft": {
          "description": "左右边距",
          "title": "左右边距",
          "required": false,
          "initValue":20,
          "x-component": "INPUT_NUMBER",
          "type": "string",
        },
      },
    },
    "dataMock": {},
  },
  "catgory": "base",
};