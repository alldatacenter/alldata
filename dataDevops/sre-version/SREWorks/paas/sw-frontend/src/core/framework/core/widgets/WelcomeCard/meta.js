/**
 * Created by wangkaihua on 2021/07/01.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "WelcomeCard",
  "type": "WelcomeCard",
  "name": "WelcomeCard",
  "title": "欢迎卡片",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "欢迎页组件，含产品介绍及引导操作等功能。",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"WelcomeCard"
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
    "docs": "<a target='_blank' href='https://3x.ant.design/components/list-cn/'>组件文档地址</a>",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "List",
      "config": {
        "flex": 4,
        "steps": [
          {
            title: "标题",
            description: "描述文案",
            icon: "",
            href: "",
          },
          {
            title: "标题",
            description: "描述文案",
            icon: "",
            href: "",
          },
          {
            title: "标题",
            description: "描述文案",
            icon: "",
            href: "",
          },
        ],
      },
    },
    "schema": {
      "type": "object",
      "properties": {
        "flex": {
          "description": "一行显示几个卡片，默认4个",
          "title": "列数",
          "x-validateType": "number",
          "required": true,
          "x-component": "Input",
          "type": "string",
        },
        "steps": {
          "type": "string",
          "title": "步骤卡片",
          "required": false,
          "description": "",
          "x-component": "EditTable",
          "x-component-props": {
            "columns": [
              {
                "editProps": {
                  "required": true,
                  "type": 1,
                  "inputTip": "",
                },
                "dataIndex": "title",
                "title": "标题",
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "",
                  "type": 1,
                },
                "dataIndex": "description",
                "title": "描述信息",
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                },
                "dataIndex": "icon",
                "title": "图标",
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                },
                "dataIndex": "buttonText",
                "title": "按钮文案",

              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                },
                "dataIndex": "href",
                "title": "跳转链接",

              },
              // {
              //   "editProps": {
              //     "required": true,
              //     "inputTip": "",
              //     "optionValues": "$(__select_blocks__)",
              //     "type": 3,
              //   },
              //   "dataIndex": "block",
              //   "title": "区块操作",
              // }
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