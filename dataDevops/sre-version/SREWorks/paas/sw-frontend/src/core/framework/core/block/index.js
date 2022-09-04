/**
 * Created by caoshuaibiao on 2021/3/7.
 * 区块渲染,用于页面中引入区块和工具栏中点击操作后触发的渲染
 */
import React, { Component } from 'react';
import { Spin, message } from 'antd';
import PageModel from '../../model/PageModel';
import PageContent from '../PageContent';


class Block extends Component {

    constructor(props) {
        super(props);
        this.state = {
            pageModel: null,
        }
    }

    componentWillMount() {
        const { widgetModel, widgetConfig } = this.props;
        const { nodeModel } = widgetModel;
        let { block, ...other } = widgetConfig;
        //分步表单,分步表单只能通过区块去定义
        if (block) {
            let blockDef = nodeModel.getBlocks().filter(blockMeta => blockMeta.elementId === block)[0], pageModel = null;
            if (blockDef) {
                pageModel = new PageModel(blockDef);
                pageModel.setNodeModel(nodeModel);
                this.setState({ pageModel });
            }
        }
    }

    render() {
        const { pageModel } = this.state, { mode, widgetModel } = this.props;
        if (!pageModel) {
            return <div style={{ width: "100%", height: "100%", justifyContent: "center", "align-items": "center", display: "flex" }}><h3>请选择要显示的区块</h3></div>
        }
        return (
            <PageContent {...this.props} pageModel={pageModel} />
        );
    }
}

export default Block;