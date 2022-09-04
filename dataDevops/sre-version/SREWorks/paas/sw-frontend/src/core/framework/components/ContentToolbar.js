/**
 * Created by caoshuaibiao on 2020/12/2.
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List } from 'antd';
import Constants from '../model/Constants';



export default class ContentToolBar extends React.Component {

    constructor(props) {
        super(props);
    }

    handleToolClick = (toolType, payload) => {
        const { containerModel } = this.props;
        containerModel.events.emit(toolType, payload)
    };

    render() {
        const size = "small";
        return (
            <div>
                {/*<Tooltip title="添加组件"><Button icon="plus" size={size} style={{marginLeft:6}} onClick={()=>this.handleToolClick(Constants.TOOL_TYPE_ADD)} >添加元素</Button></Tooltip>*/}
                {/*<Tooltip title="添加工具栏"><Button icon="build" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="添加过滤器"><Button icon="filter" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="页面设置"><Button icon="setting" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="页面函数"><Button icon="file-text" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="页面数据源"><Button icon="database" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="查看JSON"><Button icon="eye" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="预览"><Button icon="caret-right" size={size} style={{marginLeft:6}}/></Tooltip>
                <Tooltip title="保存"><Button icon="save" size={size} style={{marginLeft:6}}/></Tooltip>*/}
            </div>
        );
    }
}
