/**
 * Created by caoshuaibiao on 2020/12/17.
 * 挂件加载器
 */
import React from 'react';
import { getBuiltInWidget, getBuiltInWidgetMetaMapping } from './BuiltInWidgets';

//挂件缓存
const widgetCache = {};
/**
 * 把框架库/第三方库注入到挂件(主要对非内置挂件)
 * @param name
 * @param component
 */
function injectToWidget(name, component) {

}


class WidgetLoader {

    loadWidget(model) {
        const NotFound = () => (<span>未定义组件</span>);
        // const widget = getBuiltInWidget(model);
        let widget;
        if(model.type && window['REMOTE_COMP_LIST'].includes(model.type) && window[model.type]) {
            widget = window[model.type][model.type]
        } else {
            widget = getBuiltInWidget(model);
        }
        if (widget) {
            return Promise.resolve(widget);
        } 
        return Promise.resolve(null);
    }

    getWidgetMeta(model) {
        let meta; 
         if(model.type && window['REMOTE_COMP_LIST'].includes(model.type) && window[model.type]){
            meta =  window[model.type][model.type+"Meta"]
        } else {
            meta = getBuiltInWidgetMetaMapping()[model.type];
        }
        if (meta) {
            return Promise.resolve(meta);
        }
        return Promise.resolve(null);
    }

}


const loader = new WidgetLoader();


export default loader;