/**
 * Created by wangkaihua on 2021/05/17.
 * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
 */
export default {
  "id": "RankingList",
  "type": "RankingList",
  "name": "RankingList",
  "title": "排名列表",
  "info": {
    "author": {
      "name": "",
      "url": "",
    },
    "description": "最基础的排名列表展示",
    "links": [],
    "logos": {
      "large": "",
      "small": require("./icon.svg"),
      "fontClass":"RankingList"
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
    "docs": "",
  },
  "state": "",
  "latestVersion": "1.0",
  "configSchema": {
    "defaults": {
      "type": "RankingList",
      "config": {
        "label": "label",
        "value": "number",
        "largest": 5,
        "sort": "number",
        "importColor": "#ffc53d",
        "importCount": 3,
      },
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
        "label": {
          "description": "文字信息的对应字段",
          "title": "内容",
          "required": true,
          "x-component": "Input",
          "type": "string",
        },
        "customTooltip": {
          "description": "用于定制的tooltip字段",
          "title": "定制tooltip",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "value": {
          "description": "值的对应字段名",
          "title": "值1",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "backupValue": {
          "description": "值2为备用展示字段",
          "title": "值2",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "href": {
          "description": "支持动态取值$(row.参数名)",
          "title": "跳转链接",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "importColor": {
          "description": "默认黄色",
          "title": "重点名次颜色",
          "required": false,
          "x-component": "Input",
          "type": "string",
        },
        "importCount": {
          "description": "默认3个",
          "title": "重点名次数量",
          "required": false,
          "x-component": "Input",
          "type": "number",
        },
        "largest": {
          "description": "最多显示几条数据，填写数字【默认5条】",
          "title": "最多显示",
          "required": false,
          "x-component": "Input",
          "type": "number",
        },
        "sort": {
          "description": "请填写依赖排序的字段名",
          "title": "排序",
          "required": false,
          "x-component": "Input",
          "type": "number",
        },
      },
    },
    "supportItemToolbar": true,
    "dataMock": {},
  },
  "catgory": "base",
};