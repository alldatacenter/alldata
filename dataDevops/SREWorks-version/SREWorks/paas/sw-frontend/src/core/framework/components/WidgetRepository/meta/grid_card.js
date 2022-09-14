export default {
    "id": "GRID_CARD",
    "type": "GRID_CARD",
    "name": "GRID_CARD",
    "title": "网格卡片",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "卡片以网格形式进行布局,每个卡片显示一条数据",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icons/grid_card.svg'),
            "fontClass":'GRID_CARD'
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
        "docs": "<a target='_blank' href='#/help/book/documents/ho617k.html#82网格卡片'>组件配置说明</a>",
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "config": {
                "flex": 2,
                "title": "title",
                "toolTip": "name",
                "grid": { gutter: 9, column: 3 },
                "rowActions": {
                    "type": "",
                    "layout": "",
                    "actions": [
                        {
                            "label": "test",
                            "name": "Action1",
                            "icon": "setting"
                        }
                    ]
                },
                "api": {
                    "url": "",
                    "paging": false,
                },
                "columns": [
                    {
                        "dataIndex": "name",
                        "label": "姓名"
                    },
                    {
                        "dataIndex": "age",
                        "label": "年龄"
                    }
                ],
                "icon": "icon"
            },
            "type": "GRID_CARD"
        },
        "schema": {
            "type": "object",
            "properties": {
                "layoutSelect": {
                    "description": "布局式样",
                    "title": "布局式样",
                    "required": false,
                    "type": "string",
                    "x-component": "Radio",
                    "initValue": "advance",
                    "x-component-props": {
                        "options": [{ "value": "base", "label": "平铺" }, { "value": "advance", "label": "主次" }],
                    },
                },
                "paging": {
                    "description": "设定网格卡片分页,配置分页需要在请求参数中添加分页参数",
                    "title": "分页",
                    "required": false,
                    "type": "string",
                    "x-component": "Radio",
                    "x-component-props": {
                        "options": [{ "value": true, "label": "是" }, { "value": false, "label": "否" }],
                        "defaultValue": false,
                        "initValue": false
                    }
                }
            },
        },
        "dataMock": {},
        "supportToolbar":true,
    },
    "catgory": "base"
};