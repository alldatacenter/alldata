// /**
//  * Created by wangkaihua on 2021/04/26.
//  * 组件的元数据信息定义,包括类型、logo、配置Schema等，是组件的描述信息定义
//  */
// export default {
//   "id": "StepBar",
//   "type": "StepBar",
//   "name": "StepBar",
//   "title": "步骤条",
//   "info": {
//     "author": {
//       "name": "",
//       "url": "",
//     },
//     "description": "垂直展示的时间流信息。",
//     "links": [],
//     "logos": {
//       "large": "",
//       "small": require("./icon.svg")
//     },
//     "build": {
//       "time": "",
//       "repo": "",
//       "branch": "",
//       "hash": "",
//     },
//     "screenshots": [],
//     "updated": "",
//     "version": "",
//     "docs": "<a target='_blank' href='https://3x.ant.design/components/alert-cn/'>组件文档地址</a>",
//   },
//   "state": "",
//   "latestVersion": "1.0",
//   "configSchema": {
//     "defaults": {
//       "type": "StepBar",
//       "config": {
//         "itemObject":{
//           "titleTime": "<div><div>$(row.producer)告警信息</div></div>",
//           "description": "<div>来自$(row.ip)的$(row.name)状态变为<antd.Tag color='red'>$(row.status)</antd.Tag></div>",
//           "date": "time",
//           "dateFormat": "YYYY-MM-DD",
//           "tag": "producer",
//           "tagColorMap": {
//             "alimonitor":"#108ee9",
//             "test":"#f50"
//           },
//           "color": "status",
//           "circleMap": {
//             "success": "green",
//           },
//         },
//         "mode": "left",
//         "reverse": false,
//       },
//     },
//     "schema": {
//       "type": "object",
//       "properties": {
//         "mode":{
//           "description": "通过设置 mode 可以改变时间轴和内容的相对位置",
//           "title": "内容显示",
//           "required": false,
//           "type": "string",
//           "x-component": "Radio",
//           "x-component-props": {
//             "options": [{"value": "left", "label": "靠左显示"}, {"value": "right", "label": "靠右显示"}, {"value": "alternate", "label": "交叉显示"}],
//           },
//         },
//         "innerPosition":{
//           "description": "通过设置该选项可以改变时间轴在外部容器的相对位置",
//           "title": "渲染位置",
//           "required": false,
//           "type": "string",
//           "x-component": "Radio",
//           "x-component-props": {
//             "options": [{"value": "left", "label": "left"}, {"value": "right", "label": "right"}, {"value": "center", "label": "center"}],
//           },
//         },
//         "reverse": {
//           "title": "节点排序",
//           "required": false,
//           "type": "string",
//           "x-component": "Radio",
//           "x-component-props": {
//             "options": [{"value": true, "label": "排序"}, {"value": false, "label": "不排序"}],
//           },
//         },
//         "itemObject": {
//           "type": "object",
//           "title": "行信息属性",
//           "properties": {
//             "titleTime":{
//               "description": "row为行数据，获取变量数据如 $(row.title)",
//               "title": "标题",
//               "required": true,
//               "x-component": "Input",
//               "type": "string",
//             },
//             "date": {
//               "description": "请填写数据中日期的参数名，支持毫秒或者秒",
//               "title": "日期转换",
//               "required": true,
//               "x-component": "Input",
//               "type": "string",
//             },
//             "dateFormat": {
//               "description": "",
//               "title": "时间格式",
//               "required": false,
//               "type": "string",
//               "x-component": "Radio",
//               "x-component-props": {
//                 "options": [
//                   {"value": "YYYY-MM-DD", "label": "YYYY-MM-DD"},
//                   {"value": "YYYY-MM-DD HH:mm", "label": "YYYY-MM-DD HH:mm"},
//                   {"value": "YYYY-MM-DD HH:mm:ss", "label": "YYYY-MM-DD HH:mm:ss"},
//                 ],
//               },
//             },
//             "tag": {
//               "description": "请填写数据中标签的参数名",
//               "title": "标签转换",
//               "required": false,
//               "x-component": "Input",
//               "type": "string",
//             },
//             "tagColorMap":{
//               "description": "依据【标签转换】中的值进行转换",
//               "title": "标签颜色转换",
//               "required": false,
//               "x-component": "JSON",
//               "type": "string",
//             },
//             "color": {
//               "description": "请填写数据中颜色所依赖的参数名",
//               "title": "圆圈颜色字段",
//               "required": false,
//               "x-component": "Input",
//               "type": "string",
//             },
//             "circleMap": {
//               "description": "依据【圆圈颜色字段】中的值进行转换,指定圆圈颜色 blue, red, green, gray，或自定义的色值",
//               "title": "圆圈颜色转换",
//               "required": false,
//               "x-component": "JSON",
//               "type": "string",
//             },
//           },
//         },
//       },
//     },
//     "supportItemToolbar":true,
//     "dataMock": {},
//   },
//   "catgory": "base",
// };