/**
 * Created by caoshuaibiao on 2021/3/2.
 * 过滤项作为一行,一行占满的时候自动换行
 */
export default {
    "id": "ACTION",
    "type": "ACTION",
    "name": "ACTION",
    "title": "操作",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "业务操作定义",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icons/action.svg'),
            "fontClass":'ACTION'
        },
        "build": {
            "time": "",
            "repo": "",
            "branch": "",
            "hash": ""
        },
        "screenshots": [],
        "updated": "",
        "version": ""
    },
    "state": "",
    "latestVersion": "1.0",
    "configSchema": {
        "defaults": {
            "type": "ACTION",
            "config": {
                "title": "操作"
            }
        },
        "schema": {},
        "dataMock": {}
    },
    "catgory": "action"
};