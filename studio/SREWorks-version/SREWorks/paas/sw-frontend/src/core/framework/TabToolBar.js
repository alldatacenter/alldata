/**
 * Created by caoshuaibiao on 2019/9/6.
 * 挂件工具栏
 */
import React from 'react';
import { connect } from 'dva';

@connect(({ node }) => ({
    nodeParams: node.nodeParams
}))
export default class TabToolBar extends React.Component {
    render() {
        let { actionBar, nodeId, nodeParams, userParams, openAction, handleParamsChanged, filterWidget, customRender, ...contentProps } = this.props,
            barItems = [];
        if (filterWidget) {
            barItems.push(
                <div style={{ display: 'flex', alignItems: 'center', marginLeft: 2 }} key="__widget_filters">
                    {filterWidget}
                </div>
            )
        }
        return (
            <div style={{ display: 'flex', alignItems: 'center' }}>
                {barItems}
            </div>
        )
    }

}
