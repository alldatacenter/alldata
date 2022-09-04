/**
 * Created by caoshuaibiao on 2018/12/8.
 * 运维模块的内容区,为兼容原有后台设计做的一层内容区适配
 */

import React from 'react';
import { List, Card, Cascader, Tabs, Spin, Collapse } from 'antd';
import { Route, withRouter, Switch, Redirect } from 'dva/router';
import oamTreeService from '../services/oamTreeService';
import OamWidgets from './OamWidgets';
import OamContentMenuBar from './OamContentMenuBar';
import OamCustomActionBar from './OamCustomActionBar';
import '../style.less'
import { connect } from 'dva';
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import httpClient from '../../utils/httpClient';

@connect(() => ({
}))
class OamContent extends React.Component {

    constructor(props) {
        super(props);
        let { menuRouteData } = this.props;
        this.state = {
            routeData: null,
            nodeId: props.nodeId,
            contentData: null,
            filters: [],
            actions: [],
            nodeParams: null,
            loading: true,
            parameters: {},
            defaultFilterPath: [],
            currentActionMix: '',
            openCount: 0,
            rootTabs: true,
            needCheckRoot: menuRouteData && menuRouteData.type === 'custom' && menuRouteData.isRootNode,
            hasChecked: false
        }
    }

    componentWillMount() {
        let { menuRouteData, match, nodeConfig = {}, dispatch } = this.props, { nodeId, needCheckRoot } = this.state, urlParams = util.getUrlParams();
        //每个内容区为一个参数域,需要重置参数域
        dispatch({ type: 'node/resetParamContext' });
        //内容是树的类型渲染时直接加载树无需下面的查询
        if (needCheckRoot) {
            oamTreeService.getTreeMeta(menuRouteData.nodeId).then(mate => {
                let currentRootId = mate.current.nodeId;
                oamTreeService.getNodeTabs(currentRootId, {}).then(nodeConf => {
                    if (nodeConf.tabs.length) {
                        //this.loadNodeFilters(currentRootId);
                    } else {
                        this.setState({
                            rootTabs: false
                        })
                    }
                    this.setState({
                        hasChecked: true
                    })
                });
            });
        } else {
            //this.loadNodeFilters(nodeId);
            this.loadContentData(nodeId, {});
        }
    }

    loadNodeFilters = (nodeId) => {
        let defaultFilterPath = [], { nodeConfig = {} } = this.props;
        oamTreeService.getNodeFilters(nodeId).then(filterData => {
            let defaultFilter = false, defalutNodeList = [], filterNodeIds = [], urlSearchParams = util.getUrlParams(), allExistMapping = {};
            const transformFilter = function (filterData = [], perfix) {
                filterData = filterData || [];
                filterData.forEach(fd => {
                    if (!fd.value) {
                        fd.value = fd.label;
                    }
                    //级联多选组件要求节点id必须是唯一的,重入的时候只有value值,因此用叶子value作为id(前提是叶子节点value无重复,如果存在重复将出现bug)
                    if (filterNodeIds.includes(fd.value)) {
                        fd.id = perfix + "|" + fd.name + "|" + fd.label + "|" + fd.value;
                    } else {
                        fd.id = fd.value;
                    }
                    filterNodeIds.push(fd.id);
                    //判断参数中的所有过滤器的值是不是在当前节点的过滤器中存在
                    if (urlSearchParams[fd.name] && urlSearchParams[fd.name] === fd.value) {
                        allExistMapping[fd.name] = fd.value;
                    }
                    if (!defaultFilter) {
                        defaultFilterPath.push(fd.value);
                        if (!fd.children || fd.children.length === 0) {
                            defaultFilter = [fd];
                        }
                        defalutNodeList.push(fd);
                    }
                    if (fd.children) {
                        transformFilter(fd.children, perfix + "|" + fd.label);
                        if (fd.children.length === 0 && !defaultFilter) {
                            defaultFilter = [fd];
                            defalutNodeList.push(fd)
                        }
                    } else {
                        if (!defaultFilter) {
                            defaultFilter = [fd];
                            defalutNodeList.push(fd);
                        }
                    }
                    if (fd.children && fd.children.length === 0) delete fd.children;

                })
            };
            let defaultValue = filterData.defaultValue;
            filterData = filterData.options || filterData;
            if (filterData.length > 0) {
                //生成兼容多选单选的过滤器树数据
                transformFilter(filterData, "");
                //约定树的叶子节点名称为过滤器的重入参数名称,defalutNodeList 最后一个节点的名称
                let nodePath = [], filterName = defalutNodeList[defalutNodeList.length - 1].name, { filterDefaultValue, filterMode } = nodeConfig;
                let initFilterValue = urlSearchParams[filterName] || filterDefaultValue || defaultValue;
                //过滤器存在初始值时获取初始值节点路径
                if (initFilterValue) {
                    if (filterMode === "single") {
                        util.getTreeValuePathByNodeValue(filterData, initFilterValue, nodePath);
                        if (nodePath.length > 0) {
                            defaultFilterPath = nodePath.map(n => n.value);
                            defalutNodeList = nodePath;
                        }
                    } else if (filterMode === "multiple") {
                        defaultFilterPath = initFilterValue.split(",");
                        defalutNodeList = defaultFilterPath.map(v => {
                            return { name: filterName, value: v }
                        })
                    }
                }
            }
            this.setState({ filters: filterData, defaultFilterPath: defaultFilterPath });
            this.handleFilterChanged(defalutNodeList);
        });
    };

    loadContentData = (nodeId, parameters) => {
        this.setState({
            loading: true
        });
        let { history, match, urlRequireNodeId } = this.props;
        httpClient.all([
            oamTreeService.getNodeTabs(nodeId, parameters),
            oamTreeService.getNodeActions(nodeId, parameters),
            //oamTreeService.getNodeParams(nodeId,parameters)
        ])
            .then(dataSet => {
                let cdata = dataSet[0], actions = dataSet[1].elements, nodeParams = {}, stepActions = [];
                //增加分布Action数据的组装
                stepActions = actions.filter(act => act.config.actionType === 'STEPS');
                if (stepActions.length) {
                    stepActions.forEach(sa => {
                        let stepNameList = sa.config.steps.split(","), stepList = [];
                        for (let s = 0; s < stepNameList.length; s++) {
                            for (let a = actions.length - 1; a >= 0; a--) {
                                if (actions[a].config.name === stepNameList[s]) {
                                    stepList.push(...actions.splice(a, 1));
                                }
                            }
                        }
                        sa.config.stepActions = stepList;
                    })
                }
                //由获取的内容数据生成路由
                let routeData = {
                    children: [],
                    icon: "",
                    label: cdata.id,
                    name: cdata.id,
                    path: match.path,
                };
                Object.assign(nodeParams, parameters, { __frontend_route_path: match.path });
                let inActionPanelTab = [], routeTab = [];
                cdata.tabs.forEach(tab => {
                    if (tab.config.displayInAction) {
                        inActionPanelTab.push(tab);
                    } else {
                        routeTab.push(tab);
                    }
                });
                routeData.children = routeTab.map(child => {
                    //analysis.registerPath(match.path+"/"+child.name,child.label);
                    return {
                        icon: child.icon || "",
                        label: child.label,
                        name: child.name,
                        path: match.path + "/" + child.name,
                        config: Object.assign({}, child.config),
                        component: () => <OamWidgets {...this.props} metaData={child}
                            config={cdata}
                            widgets={child.components}
                            layout={child.layout}
                            nodeId={cdata.nodeId}
                            defaultNodeParams={nodeParams}
                            parameters={parameters}
                            openAction={this.handleOpenAction}
                            modulePath={routeData.path + "/" + child.name}
                            actions={actions}
                        />
                    }
                });
                //let oldNodeId = this.getParams()["nodeId"];
                //增加路径对比,处理查询回来的数据时页面已经切走但组件未mount,又被拉回问题
                if (window.location.hash.includes(this.props.location.pathname)) {
                    let urlObject = Object.assign({}, util.getUrlParams(), parameters, { nodeId: nodeId });
                    //console.log("urlRequireNodeId------->",urlRequireNodeId);
                    let newRegion = nodeParams.meta_region || nodeParams.region, oldRegion = urlObject.region;
                    if (newRegion && oldRegion && newRegion !== oldRegion) {
                        urlObject.region = newRegion;
                    }
                    if (urlRequireNodeId === false) {
                        delete urlObject.nodeId;
                    }
                    history.push({
                        pathname: this.props.location.pathname,
                        search: util.objectToUrlParams(urlObject)
                    });
                    this.setState({
                        contentData: cdata,
                        routeData: routeData,
                        actions: actions,
                        nodeParams: nodeParams,
                        loading: false,
                        inActionPanelTab: inActionPanelTab
                    });
                }
            });
    };

    handleNodeChanged = (nodeId) => {
        this.loadContentData(nodeId);
    };

    handleFilterChanged = (leafList) => {
        //console.log("leafList----->",leafList);
        let { nodeId } = this.state, filterParams = {};
        if (leafList) {
            if (leafList.length > 0) {
                leafList.forEach(l => {
                    let paramName = l.name, paramValue = l.value;
                    if (filterParams[paramName]) {
                        filterParams[paramName].push(paramValue);
                    } else {
                        filterParams[paramName] = [paramValue]
                    }
                });
                Object.keys(filterParams).forEach(key => {
                    filterParams[key] = filterParams[key].join(",");
                })
            }
            this.setState({
                parameters: filterParams
            });
        }
        //console.log("filterParams------>",filterParams);
        this.loadContentData(nodeId, filterParams);
    };

    handleOpenAction = (actionName, actionParams, callBack) => {
        let count = this.state.openCount;
        this.setState({
            currentActionMix: { actionName: actionName, actionParams: actionParams, callBack: callBack },
            openCount: count + 1
        });
    };

    handleCloseAction = (action) => {
        let count = this.state.openCount;
        this.setState({
            currentActionMix: '',
            openCount: count + 1
        });
    };

    componentDidMount() {
        let { __OPEN_ACTION } = util.getUrlParams();
        if (__OPEN_ACTION) {
            this.handleOpenAction(__OPEN_ACTION)
        }
    }

    render() {
        let { menuRouteData, urlRequireNodeId, match, nodeConfig = {} } = this.props, { needCheckRoot, rootTabs, hasChecked, inActionPanelTab } = this.state, urlSearchParams = util.getUrlParams();
        //解决树根节点上挂载有页面的场景
        if (needCheckRoot && hasChecked) {

        }
        let { routeData, filters, actions, nodeParams, contentData, parameters, defaultFilterPath, currentActionMix, openCount, loading } = this.state, { menuBarHeader } = this.props;
        if (!routeData) return <Spin><div style={{ width: '100%', height: '100vh' }} /></Spin>;
        let props = this.props, children = routeData.children || [], defaultTab, nowPath = this.props.location.pathname;
        if (urlRequireNodeId === false) {
            delete urlSearchParams.nodeId;
        } else {
            urlSearchParams.nodeId = contentData.nodeId;
        }
        if (children.length > 0) {
            let displayTabs = children.filter(child => child.config && !child.config.hidden);
            if (displayTabs && displayTabs.length) {
                defaultTab = displayTabs[0].path;
            } else {
                defaultTab = routeData.children[0].path;
            }
            routeData.children.forEach(cp => {
                if (nowPath === cp.path || nowPath.indexOf(cp.path) >= 0) defaultTab = cp.path;
            });
        }
        let barHeader = menuBarHeader, filterMode = 'single';
        if (contentData.serviceTypeConfig) {
            if (contentData.serviceTypeConfig.hasOwnProperty("menuBarHeader")) {
                barHeader = contentData.serviceTypeConfig.menuBarHeader;
                if (barHeader === "false") {
                    barHeader = false;
                }
            }
            if (contentData.serviceTypeConfig.hasOwnProperty("filterMode")) {
                filterMode = contentData.serviceTypeConfig.filterMode;
            }

        }
        return (

            <Spin spinning={loading}>
                <OamContentMenuBar {...props} routeData={routeData} urlSearchParams={urlSearchParams}
                    nodeParams={nodeParams}
                    actions={actions.filter(action => action.config.position === 1)}
                    filters={filters}
                    handleNodeChanged={this.handleNodeChanged}
                    handleFilterChanged={this.handleFilterChanged}
                    defaultFilterPath={defaultFilterPath}
                    key={contentData.nodeId}
                    nodeId={contentData.nodeId}
                    menuBarHeader={barHeader}
                    filterMode={filterMode}
                    openAction={this.handleOpenAction}
                    hideActionHistory={actions.filter(act => ["JOB", "API", "FLOW"].includes(act.config.actionType)).length === 0 || nodeConfig.hideActionHistory === true}
                />
                <OamCustomActionBar key={openCount} {...props} openAction={this.handleOpenAction} inActionPanelTab={inActionPanelTab}
                    actions={actions} nodeParams={nodeParams} currentActionMix={currentActionMix}
                    onCloseAction={this.handleCloseAction}
                />
                <div id="__OAM_CONTENT__">
                    <Switch key={contentData.nodeId}>
                        {
                            routeData.children.map(child => {
                                return <Route key={child.path} path={child.path} component={child.component} />
                            })
                        }
                        {
                            defaultTab && <Route path={routeData.path} render={() => <Redirect to={
                                {
                                    pathname: defaultTab,
                                    search: util.objectToUrlParams(urlSearchParams)
                                }
                            }
                            />} />
                        }
                        {
                            routeData.path.length > nowPath.length ?
                                <Route path={nowPath} render={() => <Redirect to={
                                    {
                                        pathname: defaultTab,
                                        search: util.objectToUrlParams(urlSearchParams)
                                    }
                                }
                                />} /> : null
                        }
                    </Switch>
                </div>
            </Spin>
        )

    }

}

export default withRouter(OamContent)