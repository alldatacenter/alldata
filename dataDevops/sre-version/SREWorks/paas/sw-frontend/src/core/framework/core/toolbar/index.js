/**
 * Created by caoshuaibiao on 2021/3/19.
 * 工具栏
 */
import React, { Component } from 'react';
import ToolBar from '../../ToolBar';
import { Spin, message } from 'antd';
import Constants from '../../model/Constants';
import WidgetModel from '../../model/WidgetModel';
import WidgetCard from '../WidgetCard';
import _ from 'lodash';

import '../index.less';
import TabToolBar from '../../TabToolBar';

class ToolBarAdapter extends Component {

    constructor(props) {
        super(props);
        this.actionMapping = {};
        this.filterWidget = false;
        //把新的模型toolbar定义转换为原有工具栏定义
        const { widgetModel, widgetConfig, configIndex = "toolbar", hasLeftTab = false } = props;
        const { nodeModel } = widgetModel;
        let { toolbar } = widgetConfig, toolbarData = null, docs = null, actionBar = null;
        let { filter = '', type,label='', actionList = [], docList = [], customRender } = widgetConfig[configIndex] || {};
        let vActions = [], hActions = [];
        if (docList.length > 0) {
            docs = {
                "docs": [
                    ...docList
                ],
                "name": "user_guide",
                "label": "帮助"
            }
        }
        for (let a = 0; a < actionList.length; a++) {
            let { icon, name, label, layout, block, type } = actionList[a];
            this.actionMapping[block] = actionList[a];
            let adata = {
                ...actionList[a],
                name: name || block,
                btnType: type,
                icon,
                label
            }
            if (layout === "vertical") {
                vActions.push(adata);
            } else {
                hActions.push(adata);
            }
        }
        if (vActions.length > 0 && hActions.length > 0) {
            actionBar = {
                "type": type,
                "layout": "horizontal",
                actions: [
                    ...hActions,
                    {
                        "btnType": "",
                        "label": label || "更多",
                        "children": [
                            ...vActions
                        ],
                        "icon": ""
                    },
                ]
            }
        } else if (vActions.length > 0 && hActions.length === 0) {
            actionBar = {
                "type": type,
                "layout": "vertical",
                actions: [
                    {
                        "btnType": "",
                        "label": label || "操作",
                        "children": [
                            ...vActions
                        ],
                        "icon": ""
                    },
                ]
            }
        } else if (vActions.length === 0 && hActions.length > 0) {
            actionBar = {
                "type": type,
                "size": "default",
                "layout": "horizontal",
                actions: [
                    ...hActions
                ]
            }
        }

        if (docs) {
            actionBar = actionBar || {
                "type": type,
                "layout": "horizontal",
                actions: []
            };
            actionBar.layout = "horizontal";
            actionBar.actions.unshift(docs);
        }
        if (actionBar) {
            toolbarData = {
                actionBar: actionBar
            }
        }
        //添加到工具栏上的过滤器一定是区块
        if (filter) {
            let filterId = filter
            let blockDef = nodeModel.getBlocks().filter(blockMeta => blockMeta.elementId === filterId)[0];
            let noTabFilterArr = hasLeftTab ? Constants.FILTERS.filter(item => item === 'FILTER_TAB') : Constants.FILTERS.filter(item => item !== 'FILTER_TAB');
            if (blockDef) {
                let { elements = [] } = blockDef;
                let filterBar;
                if (elements && elements.length && elements[0].type === "FluidGrid") {
                    let { rows } = elements[0]['config'];
                    rows && rows.forEach(row => {
                        filterBar = row.elements && row.elements.filter(ele => noTabFilterArr.includes(ele.type))[0];
                    })
                } else {
                    filterBar = elements.filter(ele => noTabFilterArr.includes(ele.type))[0];
                }
                //基于过滤器的数据生成过滤器挂件
                if (filterBar) {
                    let fnm = new WidgetModel(filterBar);
                    fnm.setNodeModel(nodeModel);
                    this.filterWidget = <WidgetCard key={filterBar.config.uniqueKey || 'unknown'} {...props} widgetModel={fnm} />
                } else {
                    this.filterWidget = (<span></span>)
                }
            }
            toolbarData = toolbarData || {};
        }

        this.state = {
            toolbarData: Object.assign({}, toolbarData, { customRender: customRender }),
        }
    }

    handleOpenAction = (action, params, callback) => {
        let { openAction } = this.props;
        openAction && openAction(this.actionMapping[action], params, callback);
    };

    render() {
        const { toolbarData } = this.state;
        const { hasLeftTab = false } = this.props;
        if (!toolbarData) {
            return <div />
        }
        if (hasLeftTab) {
            return (
                <TabToolBar {...this.props}  {...toolbarData} filterWidget={this.filterWidget} openAction={this.handleOpenAction} />
            )
        }
        return (
            <ToolBar {...this.props}  {...toolbarData} filterWidget={this.filterWidget} openAction={this.handleOpenAction} />
        );
    }
}

export default ToolBarAdapter;
