/**
 * Created by caoshuaibiao on 2021/3/2.
 * 单一的过滤器,过滤的表单项只能选择一个进行查询,形式为前面为表单项选择,后面为表单项的展现形式(输入、下拉、多选等等)
 */
 export default  {
    "id": "FILTER_TAB",
    "type": "FILTER_TAB",
    "name": "FILTER_TAB",
    "title": "tab过滤项",
    "info": {
        "author": {
            "name": "",
            "url": ""
        },
        "description": "tab型过滤器，切换tab可对目标数据进行分类过滤",
        "links": [],
        "logos": {
            "large": "",
            "small": require('./icons/filter_tab.svg'),
            "fontClass":'FILTER_TAB'
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
            "type":"FILTER_TAB",
            "config":{
                "title": "TAB过滤项"
            }
        },
        "schema": {},
        "dataMock": {}
    },
    "catgory": "filter"
};