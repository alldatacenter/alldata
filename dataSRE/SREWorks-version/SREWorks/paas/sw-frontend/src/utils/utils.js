import moment from 'moment';
import _ from 'lodash';
import qs from "qs";
import Mustache from 'mustache';
import shortid from "shortid";
import cacheRepository from "./cacheRepository";
import properties from '../properties';

const jsonexport = require("jsonexport/dist");



// input '?a=1&b=2&c=3' exclude=[c],
// output {a: 1, b:2}
export const stringToObject = (str, exclude = []) => {
    if (str === '') return {};
    return str.split('?')[1].split('&').reduce((prev, item) => {
        if (!item) return prev;
        let key = item.split('=')[0];
        //如果该key不需要
        if (exclude.includes(key)) { return prev };
        let value = item.split('=')[1];
        prev[key] = value;
        return prev;
    }, {})
};


/**
 * 把js对象转换为url参数字符串
 * @param object
 */
export const objectToUrlParams = (object) => {
    return qs.stringify(object, { encode: false });
};
/**
 * 获取当前页面的url参数
 *
 */
export const getUrlParams = () => {
    let hash = window.location.hash;
    if (hash.indexOf("?") > -1) {
        return qs.parse(hash.split("?")[1]);
    }
    return {};
};
/**
 * 字符串模板渲染,以变量值替换模板字符串中的"$()"占位字符串
 * @param tplStr 模板字符串
 * @param values  变量集合
 */
export const renderTemplateString = (tplStr, values) => {
    const tags = Mustache.tags;
    const escape = Mustache.escape;
    Mustache.tags = ['$(', ')'];
    Mustache.escape = v => v;
    const rendered = Mustache.render(tplStr, values);
    Mustache.tags = tags;
    Mustache.escape = escape;
    return rendered;
};
/**
 * 渲染json模板对象,注意返回的json对象为全新对象,非传入的对象引用
 * @param jsonObj  存在$()待渲染变量占位的json对象
 * @param paramsSet   替换值集合
 * @param noValueToEmpty  值集合中不存在要替换的变量时是否把变量替换为空字符串
 */
export const renderTemplateJsonObject = (jsonObj, paramsSet, noValueToEmpty = true) => {
    let keys = Object.keys(paramsSet);
    return JSON.parse(JSON.stringify(jsonObj), function (key, value) {
        if (typeof value === 'string' && value.includes("$")) {
            let paramKey = value.replace("$(", "").replace(")", "");
            let renderValue = _.get(paramsSet, paramKey);
            if ((typeof renderValue) !== "undefined") {
                return renderValue;
            }
            //不存在值时直接替换为空字符串,可以直接对字符串进行模板渲染即可
            if (noValueToEmpty) {
                return renderTemplateString(value, paramsSet)
            } else {
                //不存变量时保留变量占位。需要判断混合了变量占位字符串中是否存在变量值存在,存在则进行替换,不存在保留
                //提取变量占位符,存在的变量进行替换不存在的保持原样
                let replaceVars = value.match(/\$\(([^)]*)\)/g) || [];
                replaceVars.forEach(varStr => {
                    let paramKey = varStr.replace("$(", "").replace(")", "");
                    let renderValue = _.get(paramsSet, paramKey);
                    if ((typeof renderValue) !== "undefined") {
                        let varTmp = "\\$\\(" + paramKey + "\\)";
                        let rex = new RegExp(varTmp, 'g');
                        if (renderValue === null) {
                            renderValue = "";
                        }
                        value = value.replace(rex, renderValue);
                    }
                });
                return value;
            }
            return value
        }
        return value;
    });
};

/**
 * 获取树上指定值节点值的节点路径。要求节点值必须唯一
 * @param treeNode
 * @param nodeValue
 * @param nodePath
 * @returns {boolean}
 */
export const getTreeValuePathByNodeValue = (treeNode, nodeValue, nodePath) => {
    if (!treeNode || !nodeValue) {
        return false;
    }
    if (!Array.isArray(treeNode)) treeNode = [treeNode];
    for (let t = 0; t < treeNode.length; t++) {
        let cNode = treeNode[t];
        nodePath.push(cNode);
        if (cNode.value === nodeValue) {
            return true;
        }
        if (cNode.children && cNode.children.length) {
            if (getTreeValuePathByNodeValue(cNode.children, nodeValue, nodePath)) {
                return true
            }
        }
        //回溯
        nodePath.pop();
    }
};
/**
 * 获取新的x-biz-app值
 * @returns {string}
 */
export const getNewBizApp = (appId) => {
    let urlParams = getUrlParams(), app = window.location.hash.split("/")[1];
    //定义期的全部为开发态,先以url中存在app_id为标识，后续可能会进行重构修改
    if (urlParams.app_id && app === 'flyadmin') {
        return `${urlParams.app_id},${properties.defaultNamespace},${properties.defaultStageId}`;
    } else {
        if (!appId) {
            appId = app;
        }
        return cacheRepository.getAppBizId(appId);
    }

};



/**
 * 导出csv文件
 * @param columns
 * @param oriData
 * @param filename
 */
export const exportCsv = (columns, oriData, filename = "export.csv") => {
    if (Array.isArray(columns) && columns.length > 0) {

        let exportData = oriData.map(item => {
            let exp = {};
            columns.forEach(column => {
                let { dataIndex, label, render } = column;
                if (item.hasOwnProperty(dataIndex)) {
                    exp[label] = item[dataIndex];
                }
            });
            return exp;
        });
        jsonexport(exportData, function (err, csv) {
            if (err) return console.log(err);
            let blob = new Blob(['' + csv], { type: 'text/csv;charset=utf-8;' });
            if (navigator.msSaveOrOpenBlob) {
                navigator.msSaveOrOpenBlob(blob, filename);
            } else {
                let url = URL.createObjectURL(blob);
                let downloadLink = document.createElement('a');
                downloadLink.href = url;
                downloadLink.download = filename;
                document.body.appendChild(downloadLink);
                downloadLink.click();
                document.body.removeChild(downloadLink);
                URL.revokeObjectURL(url);
            }
        });

    }
};


export const generateId = () => {
    return shortid.generate();
};