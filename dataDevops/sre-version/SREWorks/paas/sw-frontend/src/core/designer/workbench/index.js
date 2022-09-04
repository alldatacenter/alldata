/**
 * Created by caoshuaibiao on 2020/11/3.
 * 前端开发工作台
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Layout, Menu, Breadcrumb, Card, Spin, message, Switch } from 'antd';
import MenuNavigation from "./MenuNavigation";
import NodeNavigation from "./NodeNavigation";
import NodeModel from '../../framework/model/NodeModel';
import FormBlockEditor from '../editors/FormBlockEditor';
import BlockEditor from '../editors/BlockEditor';
import PageEditor from '../editors/PageEditor';
import { page_template_meta, template_app_id } from '../editors/TemplateConstant';
import appMenuTreeService from '../../services/appMenuTreeService';
import _ from 'lodash';
import uuidv4 from 'uuid/v4';
import PageModel from '../../framework/model/PageModel';
import Bus from '../../../utils/eventBus';

import './index.less';
import service from '../../framework/legacy/widgets/flyadmin/service';
const noDataImg = require('appAssets/img/no-data.png');
const { Content, Sider } = Layout;

export default class Workbench extends React.Component {
    static defaultProps = {
        nodeGroupData: []
    };;
    constructor(props) {
        super(props);
        this.state = {
            menuFold: false,
            tabFold: false,
            currentEditorModel: false,
            contentLoading: false,
            nodeTypeId: null,
            compKey: uuidv4(),
            nodeGroupData: props.nodeGroupData
        }
        this.cloneGroupData = []
        this.childRef = React.createRef()
    }

    componentDidMount() {
        Bus.on('returnTreeData', (res) => {
            this.findTargetNodeAndRefresh(res);
        })
    }
    componentWillUnmount(){
        Bus.off('returnTreeData')
    }
    // 遍历树形结构，定位激活节点
    findTargetNodeAndRefresh = (treeData) => {
        let nodeTypeId = this.state.nodeTypeId;
        let nodeData = [];
        let pathNodeMapping = {};
        const genPathNodeMapping = (nodeItem) => {
            pathNodeMapping[nodeItem.nodeTypePath] = nodeItem;
            let { children } = nodeItem;
            if (children && children.length) {
                children.forEach(child => {
                    genPathNodeMapping(child)
                })
            }
        };
        genPathNodeMapping(treeData[0]);
        nodeData = pathNodeMapping[nodeTypeId];
        this.loadNodeModel(nodeTypeId,nodeData,true);
    }
    recursionFind(treeData, nodeTypeId) {
        let nodeData = [], execFlag = true
        treeData.forEach(item => {
            if (item.nodeTypePath === nodeTypeId) {
                execFlag = false
            }
        })
    }
    loadNodeModel = (nodeTypeId, nodeData,goFlag=false) => {
        if ((nodeTypeId === this.state.nodeTypeId) && !goFlag) {
            return;
        }
        this.setState({
            nodeTypeId: null,
            nodeData: nodeData,
            contentLoading: true
        });
        if (nodeTypeId) {
            let nodeModel = new NodeModel({ nodeId: nodeTypeId });
            nodeModel.load('dev').then(result => {
                let { originNodeTypeId } = this.state;
                this.setState({
                    nodeModel: nodeModel,
                    currentEditorData: nodeModel.getPageModel(),
                    editorType: "MAIN_PAGE",
                    editorKey: "MAIN_PAGE",
                    nodeTypeId: nodeTypeId,
                    contentLoading: false,
                    originNodeTypeId: originNodeTypeId ? originNodeTypeId : nodeTypeId,
                    nodeGroupData: nodeModel.getGroupData()
                });
            })
        }
    };

    onMainPageClick = () => {
        let { nodeModel } = this.state;
        this.setState({
            currentEditorData: nodeModel.getPageModel(),
            editorKey: "MAIN_PAGE",
            editorType: "MAIN_PAGE"
        });
    };

    onNodeClick = (nodeTypeId, nodeData) => {
        this.setState({
            nodeTypeId: nodeTypeId
        })
        this.loadNodeModel(nodeTypeId, nodeData);
    };

    handleSaveItem = (item) => {
        let { nodeModel } = this.state;
        this.setState({
            contentLoading: true,
        });
        nodeModel.saveItem(item).then(result => {
            this.setState({
                contentLoading: false,
                nodeGroupData: nodeModel.getGroupData(),
            });
        });
    };

    handleAddItem = (item) => {
        let { nodeModel } = this.state;
        nodeModel.addItem(item);
        this.setState({
            nodeGroupData: nodeModel.getGroupData()
        });
    };

    handleDeleteItem = (item) => {
        let { nodeModel } = this.state;
        this.setState({
            contentLoading: true,
        });
        nodeModel.removeItem(item).then(result => {
            this.setState({
                contentLoading: false,
                nodeGroupData: nodeModel.getGroupData(),
                currentEditorData: nodeModel.getPageModel(),
                editorKey: "MAIN_PAGE",
                editorType: "MAIN_PAGE"
            });
        });

    };

    handleItemClick = (item) => {
        this.setState({
            currentEditorData: item,
            editorKey: item.id,
            editorType: item.type
        });
    };

    handleSaveMainPage = () => {
        let { nodeModel } = this.state;
        this.setState({
            contentLoading: true
        });
        nodeModel.savePageModel().then(result => {
            this.setState({
                contentLoading: false
            });
        }).catch(err => {
            message.error(JSON.stringify(err));
            this.setState({
                contentLoading: false
            });
        });
    };
    saveAsTemplate = (templateServiceType) => {
        let { nodeModel, nodeGroupData } = this.state;
        let unKey = uuidv4();
        let newParam = nodeModel.getPageModel().toJSON();
        let cloneParams = _.cloneDeep(newParam);
        this.cloneGroupData = _.cloneDeep(nodeGroupData);
        cloneParams.name = unKey;
        cloneParams.label = unKey;
        cloneParams.id = unKey;
        cloneParams.nodeTypePath = page_template_meta.parentNodeTypePath + "::" + templateServiceType;
        let appIdReg = new RegExp(this.props.appId, 'g');
        let cloneParamsStr = JSON.stringify(cloneParams);
        let regArr = []
        if (this.cloneGroupData[0] && this.cloneGroupData[0].items) {
            this.cloneGroupData[0].items.forEach((item, i) => {
                let oldUuid = item.name;
                let newBlockId = uuidv4();
                let newToolBarBlockId = template_app_id + ":BLOCK:" + newBlockId;
                let toolBarBlockIdRegExp = new RegExp(this.props.appId + ":BLOCK:" + oldUuid, 'g');
                cloneParamsStr = cloneParamsStr.replace(toolBarBlockIdRegExp, newToolBarBlockId);
                item.name = newBlockId;
                item.id = newBlockId;
                regArr.push({
                    oldBlockId: toolBarBlockIdRegExp,
                    newBlockId: newToolBarBlockId
                })
            })
        }
        cloneParams = JSON.parse(cloneParamsStr.replace(appIdReg, template_app_id));
        let cloneGroupDataStr = JSON.stringify(this.cloneGroupData[0].items);
        regArr.forEach(item => {
            cloneGroupDataStr = cloneGroupDataStr.replace(item.oldBlockId, item.newBlockId)
        })
        this.cloneGroupData[0].items = JSON.parse(cloneGroupDataStr);
        this.setState({
            contentLoading: true
        });
        appMenuTreeService.saveMainPage(cloneParams).then(res => {
            this.saveAsBlocks(templateServiceType, cloneParams);
            this.setState({
                contentLoading: false
            });
        }).finally(error => {
            this.setState({
                contentLoading: false
            });
        });

    }
    saveAsBlocks = (templateServiceType) => {
        let paramsArray = [];
        this.cloneGroupData[0] && this.cloneGroupData[0].items.forEach(item => {
            let pageModelItem = new PageModel(item);
            let pageJsonItem = pageModelItem.toJSON();
            let blockConfig = { label: pageModelItem.label, name: pageModelItem.name || pageModelItem.id, category: pageModelItem.category, tags: pageModelItem.tags };
            let obj = {
                ...item,
                ...pageJsonItem,
                ...blockConfig
            }
            let params = {
                appId: template_app_id,
                name: item.name,
                nodeTypePath: page_template_meta.parentNodeTypePath + "::" + templateServiceType,
                id: item.id,
                elementId: template_app_id + ':BLOCK:' + item.name,
            }
            obj = Object.assign(obj, params)
            obj.config = {
                ...obj
            }
            paramsArray.push(obj);
        })
        paramsArray.forEach(obj => {
            let { nodeTypePath } = obj;
            appMenuTreeService.saveElement(obj).then(res => {
                let { elementId } = res;
                appMenuTreeService.attachNode(nodeTypePath, elementId);
            });
        })
    }
    handleContentResize = () => {
        /*setTimeout(function () {
            let event = new Event('resize');
            window.dispatchEvent(event);
        },500)*/
    };
    // 从模板创建
    createFromTemplate = (nodeId, nodeData) => {
        let {nodeTypeId} = this.state;
        let params = {
            sourceNodeTypePath: nodeId,
            targetNodeTypePath: this.state.nodeTypeId,
        }
        appMenuTreeService.createFromTemplateByClone(params).then(res => {
            Bus.emit('refreshDirTree', nodeTypeId);
        })
    }
    // 重置模板uuid 并保存
    resetTemplate = (nodeTypeId) => {
        let { nodeModel } = this.state;
        this.setState({
            nodeModel: nodeModel,
            currentEditorData: nodeModel.getPageModel,
            nodeGroupData: nodeModel.getGroupData
        })
    }
    render() {
        let { menuFold, tabFold, currentEditorData, editorType, nodeGroupData, nodeTypeId, editorKey, contentLoading, nodeModel, nodeData } = this.state, { stageHeight = 660, style, ...otherProps } = this.props;
        let contentHeight = stageHeight - 40;
        return (
            <Layout className="abm_frontend_designer" style={{ height: stageHeight }}>
                <Sider trigger={null} collapsible collapsed={menuFold} width={170} collapsedWidth={32}>
                    {menuFold && <div className="fold-name">菜单节点树</div>}
                    <div style={{ display: menuFold ? "none" : "" }}>
                        <div style={{ height: 36 }} className="menu-tree-header">
                            <div style={{ marginTop: -5, display: 'flex', width: '100%', justifyContent: 'space-between' }}>
                                {!menuFold && <div style={{ width: "100%", textAlign: "center", opacity: 0.65 }}>
                                    <h4>菜单节点树</h4>
                                </div>}
                            </div>
                        </div>
                        <div style={{ overflowY: "auto", height: contentHeight }}>
                            <MenuNavigation key={this.state.compKey} ref={this.childRef} appId={this.props.appId || "front-dev"} menuFold={menuFold} onNodeClick={this.onNodeClick} />
                        </div>
                    </div>
                </Sider>
                <div onClick={(e) => { this.setState({ menuFold: !menuFold }, () => this.handleContentResize()) }} className="collapsed-btn-front-menu globalBackground">
                    <LegacyIcon type={menuFold ? 'right' : 'left'} />
                </div>
                <Content className={!nodeTypeId ? "globalBackground" : ""} style={{ height: stageHeight }}>
                    <Spin spinning={contentLoading}>
                        {
                            nodeTypeId &&
                            <Layout>
                                <Sider style={{ marginLeft: 2 }} trigger={null} collapsible collapsed={tabFold}
                                    collapsedWidth={32} width={181}>
                                    {tabFold && <div className="fold-name">页面设计器</div>}
                                    <div style={{ display: tabFold ? "none" : "" }}>
                                        <div style={{ height: 36 }} className="menu-tree-header">
                                            <div style={{
                                                marginTop: -5,
                                                display: "flex",
                                                width: "100%",
                                                justifyContent: "space-between",
                                            }}>
                                                <div style={{ width: "100%", textAlign: "center", opacity: 0.65 }}>
                                                    <h4>页面设计器</h4>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="node-navigation" style={{ overflowY: "auto", height: contentHeight, overflowX: "hidden" }}>
                                            <NodeNavigation mainPageClick={this.onMainPageClick}
                                                onAddItem={this.handleAddItem}
                                                onItemClick={this.handleItemClick}
                                                nodeGroupData={nodeGroupData}
                                            />
                                        </div>
                                    </div>
                                </Sider>
                                <div onClick={(e) => {
                                    this.setState({ tabFold: !tabFold }, () => this.handleContentResize());
                                }} className="collapsed-btn-front-menu globalBackground">
                                    <LegacyIcon type={tabFold ? "right" : "left"} />
                                </div>
                                <Content className="globalBackground"
                                    style={{ height: stageHeight, zIndex: 100, marginLeft: 2 }}>
                                    {editorType === "MAIN_PAGE" &&
                                        <PageEditor {...otherProps}
                                            contentLoading={contentLoading}
                                            height={stageHeight}
                                            nodeData={nodeData}
                                            routeData={nodeData}
                                            pageModel={nodeModel.getPageModel()} key={editorKey}
                                            nodeModel={nodeModel}
                                            saveAs={this.saveAsTemplate}
                                            createFromTemplate={this.createFromTemplate}
                                            onSave={this.handleSaveMainPage} />}
                                    {editorType === "FORM" &&
                                        <FormBlockEditor {...otherProps} editorData={currentEditorData}
                                            nodeModel={nodeModel}
                                            onSave={this.handleSaveItem} key={editorKey}
                                            onDelete={this.handleDeleteItem}
                                        />
                                    }
                                    {editorType === "BLOCK" &&
                                        <BlockEditor {...otherProps}
                                            nodeData={nodeData}
                                            height={stageHeight}
                                            editorData={currentEditorData}
                                            onDelete={this.handleDeleteItem}
                                            onSave={this.handleSaveItem}
                                            nodeModel={nodeModel}
                                            key={editorKey} />}
                                </Content>
                            </Layout>
                        }
                        {
                            !nodeTypeId && <div className="no-data globalBackground">
                                <div className="img-wrapper">
                                    <img width={200} src={noDataImg} />
                                </div>
                                <div className="no-data-text">
                                    暂无数据，请点击左边菜单节点树进行配置哦
                                </div>
                            </div>
                        }
                    </Spin>
                </Content>
            </Layout>

        );
    }
}