/**
 * Created by wangkaihua on 2021/04/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "CopyRight",
  "type": "CopyRight",
  "name": "CopyRight",
  "title": "版权组件",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "版权组件，用于页面底部声明",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"CopyRight"
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
      "type": "CopyRight",
      "config": {
        "message": "版权声明",
        "showIcon": false,
        "alertType": "success",
        "closable": false,
        "closeText": "关闭",
        "icon":"",
        "description": "版权声明",
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "spaceNumber": {
          "description": "左边距",
          "title": "左边距",
          "required": false,
          "initValue": 0,
          "type": "string",
          "x-component": "Input",

        },
        "copyRightLabel": {
          "description": "版权标志，如Copyright©",
          "title": "版权标志",
          "required": true,
          "x-component": "Input",
          "initValue": "Copyright©",
          "type": "string",
        },
        "validateTime": {
          "description": "版权效期，格式如2021，2014-2021等",
          "title": "版权效期",
          "required": true,
          "x-component": "Input",
          "initValue": 2021,
          "type": "string",
        },
        "companyOrGroupName": {
          "description": "版权归属，在此可以填写字符串，a标签链接，也可以添加react语法组件",
          "title": "版权归属",
          "required": true,
          "initValue": "SREWorks",
          "x-component": "Text",
          "type": "string",
          "x-component-props": {
            "placeholder": "支持JSXRender",
          },
        },
        "isFixed": {
          "description": "是否固定到页面底部，一般空页面适配固定到页面底部",
          "title": "固定到底部",
          "required": false,
          "type": "string",
          "x-component": "Radio",
          "initValue": false,
          "x-component-props": {
            "options": [{"value": false, "label": "否"}, {"value": true, "label": "是"}],
          },
        },
        // "copyStyle": {
        //   "description": "自定义式样,遵循react行内样格式",
        //   "title": "自定义式样",
        //   "required": false,
        //   "type": "string",
        //   "x-component": "MODAL_ACE",
        // }
      },
    },
    "dataMock": {},
  },
  "catgory": "staticComp",
};