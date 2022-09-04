/**
 * Created by caoshuaibiao on 2021/3/2.
 * 类阿里云高级过滤器，是单挑展示,过滤项之间支持任意组合叠加等
 */
export default  {
    "id": "FILTER_MIX",
    "type": "FILTER_MIX",
    "name": "FILTER_MIX",
    "title": "高级过滤条",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "高级过滤条,显示为一个过滤项,过滤项可以进行切换、追加、替换等进行组合过滤",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icons/filter-mix.svg'),
            "fontClass":'FILTER_MIX'
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
            "type":"FILTER_MIX",
            "config":{
                "title": "高级过滤条"
            }
        },
        "schema": {},
        "dataMock": {}
    },
    "catgory": "filter"
};