/**
 * Created by caoshuaibiao on 2021/3/2.
 * 单一的过滤器,过滤的表单项只能选择一个进行查询,形式为前面为表单项选择,后面为表单项的展现形式(输入、下拉、多选等等)
 */
export default {
    "id": "FILTER_SINGLE",
    "type": "FILTER_SINGLE",
    "name": "FILTER_SINGLE",
    "title": "单过滤项",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "单过滤项,每次只能进行一个过滤项过滤,可进行切换过滤项",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icons/filter-single.svg'),
            "fontClass":'FILTER_SINGLE'
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
            "type": "FILTER_SINGLE",
            "config": {
                "title": "单过滤项"
            }
        },
        "schema": {},
        "dataMock": {}
    },
    "catgory": "filter"
};