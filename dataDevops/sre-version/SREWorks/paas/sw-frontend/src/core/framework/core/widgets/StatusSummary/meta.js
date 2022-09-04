/**
 * Created by wangkaihua on 2021/4/26.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "StatusSummary",
  "type": "StatusSummary",
  "name": "StatusSummary",
  "title": "状态概览",
  "info": {
    "author": {
      "name": "",
      "url": ""
    },
    "description": "可展示各种数据的状态与数据。",
    "links": [],
    "logos": {
      "large": "",
      "small": require('./icon.svg'),
      "fontClass": "StatusSummary"
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
    "docs": "数据结构为key value",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "config": {
        "formatList": [
          {
            "href": "#/odps/oam/business/summary?region=cn-internal&nodeId=odps%7Cbusiness%7CI:regionManage:regionManage::regionSummary:regionSummary",
            "dataIndex": "summary.region_number",
            "unit": "",
            "label": "服务域"
          },
          {
            "href": "#/odps/oam/business/my_project?nodeId=odps%7Cbusiness%7CI:projectManage:projectManage::projectList:projectList",
            "dataIndex": "summary.project_number",
            "unit": "",
            "label": "项目"
          },
          {
            "href": "#/odps/oam/business/my_quota?nodeId=odps%7Cbusiness%7CI:quotaManage:quotaManage::quotaList:quotaList",
            "dataIndex": "summary.quota_number",
            "unit": "",
            "label": "配额组"
          },
          {
            "href": "#/odps/oam/business/instance_snapshot_info?nodeId=odps%7Cbusiness%7CI:instancemanagement:instancemanagement::instancesnapshoot:instancesnapshoot",
            "dataIndex": "summary.task_number",
            "unit": "",
            "label": "作业数"
          }
        ],
        "statusList": [
          {
            "label": "运行中",
            "dataIndex": "segment.Running"
          },
          {
            "label": "初始化",
            "dataIndex": "segment.Waiting_on_odps"
          },
          {
            "label": "等资源",
            "dataIndex": "segment.Waiting_on_fuxi"
          },
          {
            "label": "结束中",
            "dataIndex": "segment.Terminated"
          },

        ],
        "segmentTitle": "作业状态",
        "bordered": false,
        "colon": true,
        "layout": "horizontal",
        "labelStyle": {},
        "descriptionStyle": {
          "fontSize:": 16,
        },
        "column": { xxl: 4, xl: 3, lg: 3, md: 3, sm: 2, xs: 1 },
      },
      "type": "StatusSummary"
    },
    "schema": {
      "type": "object",
      "properties": {
        "minHeight": {
          "type": "string",
          "description": "数字类型,单位(px)",
          "title": "组件最小高度",
          "required": false,
          "initValue":100,
          "x-validateType": "number",
          "x-component": "Input",
        },
        "formatList": {
          "type": "string",
          "title": "数据展示",
          "required": true,
          "enableScroll": true,
          "description": "参数有label（参数的名称）,dataIndex(参数key),span（占据的位置，默认1列）,href（配置该参数后变为链接形式），render（可配置自定义渲染内容）",
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
                "title": "名称"
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "参数key",
                  "type": 1
                },
                "dataIndex": "dataIndex",
                "title": "标识"
              },
              {
                "editProps": {
                  "required": false,
                  "inputTip": "ToolTip描述信息",
                  "type": 1
                },
                "dataIndex": "description",
                "title": "ToolTip描述信息"
              },
              {
                "editProps": {
                  "required": false,
                  "inputTip": "",
                  "type": 1
                },
                "dataIndex": "unit",
                "title": "数据单位"
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                  "inputTip": "配置该参数后变为链接形式",
                },
                "dataIndex": "href",
                "title": "跳转链接"
              },
              {
                "editProps": {
                  "required": false,
                  "type": 1,
                  "inputTip": "可配置自定义渲染内容",
                },
                "dataIndex": "render",
                "title": "render"
              },
            ]
          }
        },
        "statusList": {
          "type": "string",
          "title": "状态参数列表",
          "required": false,
          "description": "参数有label（参数的名称）,dataIndex(参数key),color（圆圈显示颜色）",
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
                "title": "名称"
              },
              {
                "editProps": {
                  "required": true,
                  "inputTip": "参数key",
                  "type": 1
                },
                "dataIndex": "dataIndex",
                "title": "标识"
              },
              {
                "editProps": {
                  "required": false,
                  "inputTip": "",
                  "type": 1
                },
                "dataIndex": "color",
                "title": "圆圈颜色"
              }
            ]
          }
        },
        "segmentTitle": {
          "description": "",
          "title": "图表标题",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
      }
    },
    "supportItemToolbar": false,
    "dataMock": {}
  },
  "catgory": "base"
};