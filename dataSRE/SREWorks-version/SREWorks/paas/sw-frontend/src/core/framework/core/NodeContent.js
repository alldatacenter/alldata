/**
 * Created by caoshuaibiao on 2021/2/7.
 * 节点内容区,根据节点的定义生成的页面,节点页面中也可能存在子节点页面
 */
import React from 'react';
import { List, Card, Cascader, Tabs, Spin, Collapse } from 'antd';
import { Route, withRouter, Switch, Redirect } from 'react-router-dom';
import service from '../../services/appMenuTreeService';
import PageContent from './PageContent';
import * as util from '../../../utils/utils';
import NodeModel from '../model/NodeModel';
import { connect } from 'dva';

import './index.less';

@connect(() => ({
    currentUser: global.currentUser,
}))
class NodeContent extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            pageModel: null,
            loading: false,
            nodeParams: {},
            nodeModel: null,
        };
    }

    componentWillMount() {
        let { nodeId, dispatch } = this.props;
        dispatch({ type: 'node/resetParamContext' });
        this.loadContentData(nodeId);
    }

    loadContentData = (nodeId) => {
        this.setState({
            loading: true
        });
        let { match, userParams = {}, dispatch, nodeData, currentUser } = this.props, nodeParams = {};
        let nodeModel = new NodeModel({ nodeId: nodeData.nodeTypePath });
        //TODO 增加页面数据源的数据获取放入nodeParams
        Promise.all([nodeModel.load(), service.getNodeParams(nodeId)])
            .then(dataSet => {
                //由获取的内容数据生成路由
                Object.assign(nodeParams, dataSet[1], { __frontend_route_path: match.path, __currentUser__: currentUser }, userParams, util.getUrlParams());
                dispatch({ type: 'node/initParams', initParams: { nodeParams: nodeParams } });
                this.setState({
                    nodeParams: dataSet[1],
                    pageModel: nodeModel.getPageModel(),
                    loading: false
                });

            });
    };


    render() {
        let { loading, pageModel, nodeParams } = this.state, { nodeData, routeData } = this.props;
        if (loading) return <Spin><div style={{ width: '100%', height: '50vh' }} /></Spin>;
        return (
            <PageContent {...this.props} initNodeParams={{ ...nodeParams }} nodeData={nodeData} routeData={routeData} pageModel={pageModel} pageLayoutType={nodeData.config.pageLayoutType} />
        )

    }

}

export default withRouter(NodeContent)

