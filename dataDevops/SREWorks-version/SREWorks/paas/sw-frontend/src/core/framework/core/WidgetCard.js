/**
 * Created by caoshuaibiao on 2020/12/17.
 * 挂件卡片
 */
import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List, Row, Col, Menu, Divider, Radio } from 'antd';
import { PlusOutlined, CloseOutlined, CopyOutlined, RightOutlined, DownOutlined } from '@ant-design/icons';
import _ from "lodash";
import widgetLoader from './WidgetLoader';
import OamWidget from '../OamWidget';
import Constants from '../model/Constants';
import Filter from './filter';
import Action from './action';
import Block from './block';
import ToolBar from './toolbar';
import JSXRender from './JSXRender';

import './index.less';

export default class WidgetCard extends React.Component {

    constructor(props) {
        super(props);
        let { widgetModel, nodeParams, actionParams = {} } = props;
        this.dataSource = widgetModel && widgetModel.isVisible(nodeParams) ? widgetModel.getDataSource() : null;
        this.state = {
            loading: true,
            close: false,
            fold: false,
            dataLoading: this.dataSource !== null,
            refreshLoading: false,
            widgetData: null,
            runtimeConfig: widgetModel.getRuntimeConfig(Object.assign({}, nodeParams, actionParams))
        };
        this.WidgetComponent = null;
        this.gridPos = widgetModel.gridPos || widgetModel.config.gridPos || Constants.UNKNOWN_GRID_POS;
        this.wrapperType = widgetModel.getWrapperType();
    }

    componentWillMount() {
        const { widgetModel, nodeParams, actionParams } = this.props;
        widgetLoader.loadWidget(widgetModel).then(WidgetComponent => {
            this.WidgetComponent = WidgetComponent;
            this.setState({
                loading: false
            })
        });
        if (this.dataSource) {
            this.dataSource.query(Object.assign({}, nodeParams, actionParams)).then(widgetData => {
                this.setState({
                    runtimeConfig: widgetModel.getRuntimeConfig(Object.assign({}, widgetData, nodeParams)),
                    widgetData: widgetData,
                    dataLoading: false
                });
            });
        }
    }


    shouldComponentUpdate(nextProps, nextState) {
        let { widgetModel, nodeParams, cardHeight, location = {} } = this.props, nextLocation = nextProps.location || {};
        return !_.isEqual(this.state, nextState)
            || widgetModel.needReload(nodeParams, nextProps.nodeParams)
            || (cardHeight && nextProps.cardHeight !== cardHeight)
            || location.pathname !== nextLocation.pathname
            || nodeParams.___refresh_timestamp !== nextProps.nodeParams.___refresh_timestamp
    }

    componentWillReceiveProps(nextProps) {
        let { widgetModel, nodeParams } = this.props;
        if (widgetModel.needReload(nodeParams, nextProps.nodeParams) && this.dataSource) {
            if (widgetModel && widgetModel.type && Constants.EXCLUDE_COMP_TYPES.includes(widgetModel.type) && widgetModel.dataSourceMeta && widgetModel.dataSourceMeta.api) {
                this.setState({
                    runtimeConfig: widgetModel.getRuntimeConfig(Object.assign({}, this.state.widgetData, nextProps.nodeParams))
                })
                return false;
            }
            this.setState({
                refreshLoading: true,
            });
            this.dataSource.query(nextProps.nodeParams).then(widgetData => {
                this.setState({
                    widgetData: widgetData,
                    runtimeConfig: widgetModel.getRuntimeConfig(Object.assign({}, widgetData, nextProps.nodeParams)),
                    refreshLoading: false
                });
            });
        }
    }

    handleClose = () => {
        this.setState({
            close: true
        });
    }

    handleFold = () => {
        this.setState({
            fold: !this.state.fold
        });
    }
    handleParamsChanged = (paramData, outputs) => {
        let { dispatch } = this.props;
        dispatch && dispatch({ type: 'node/updateParams', paramData: paramData });
    };
    render() {
        let { widgetModel, nodeParams, actionParams = {}, mode, cardHeight, pageLayoutType } = this.props, { loading, dataLoading, widgetData, refreshLoading, runtimeConfig, close, fold } = this.state;
        let headerExist = false;
        if ((!widgetModel.isVisible(nodeParams)) || close) {
            return <div />
        }
        if (loading || dataLoading || !widgetModel.isReady(nodeParams)) return <Spin wrapperClassName="abm_frontend_widget_component_spin" />;
        const WidgetComponent = this.WidgetComponent;
        let { title } = widgetModel.config, cardContent, toolbarItem = null, { style, ...otherProps } = this.props;
        let foldItem = null;
        //过滤器之类不需要头和边框
        if (Constants.FILTERS.includes(widgetModel.type)) {
            cardContent = <Filter {...otherProps} widgetConfig={runtimeConfig} actionParams={Object.assign({}, actionParams, widgetData)} />;
        } else if (Constants.ACTIONS.includes(widgetModel.type)) {
            cardContent = <Action {...otherProps} widgetConfig={runtimeConfig} actionParams={Object.assign({}, actionParams, widgetData)} />;
        } else if (Constants.BLOCK === widgetModel.type) {
            cardContent = <Block {...otherProps} widgetConfig={runtimeConfig} actionParams={Object.assign({}, actionParams, widgetData)} widgetData={widgetData} />;
        } else if (WidgetComponent) {
            cardContent = <WidgetComponent {...otherProps} widgetConfig={runtimeConfig} actionParams={Object.assign({}, actionParams, widgetData)} widgetData={widgetData} />
        } else {
            if (this.wrapperType !== Constants.CARD_WRAPPER_NONE) {
                let { title, ...otherConfig } = runtimeConfig;
                runtimeConfig = otherConfig;
            }
            cardContent = <OamWidget {...otherProps} widget={{ type: widgetModel.type, config: runtimeConfig }} widgetConfig={runtimeConfig} widgetData={widgetData} />
        }
        cardContent = (
            <Spin spinning={refreshLoading} wrapperClassName="abm_frontend_widget_component_spin">
                {cardContent}
            </Spin>
        );
        const { nodeModel } = widgetModel;
        let filterConfig = {};
        if (widgetModel.hasToolbar()) {
            let modelDef = nodeModel.getBlocks() && nodeModel.getBlocks().filter(block => block.elementId === widgetModel.toolbar.filter)[0] || {};
            if (modelDef['elements'] && modelDef['elements'][0]['config'] && modelDef['elements'][0]['config']['rows'] && modelDef['elements'][0]['config']['rows'][0] && modelDef['elements'][0]['config']['rows'][0]['elements']) {
                filterConfig = modelDef['elements'][0]['config']['rows'][0]['elements'][0]['config'];
            }
            toolbarItem = <ToolBar {...this.props} handleParamsChanged={this.handleParamsChanged} widgetConfig={runtimeConfig} />
        }
        if (this.wrapperType === Constants.CARD_WRAPPER_NONE) {
            return cardContent;
        }
        let margin = (mode === Constants.WIDGET_MODE_EDIT ? (Constants.WIDGET_DEFAULT_MARGIN * 2) : -Constants.WIDGET_DEFAULT_MARGIN * 0.5);
        let calcHeight = (this.gridPos.h * (Constants.WIDGET_DEFAULT_ROW_HEIGHT + Constants.WIDGET_DEFAULT_MARGIN) - Constants.WIDGET_DEFAULT_MARGIN - margin);
        if (pageLayoutType === Constants.PAGE_LAYOUT_TYPE_FLUID && cardHeight !== 'auto') {
            calcHeight = cardHeight - margin;
        }
        let { backgroundColor, headerColor, cardBorder = true, foldEnable, hiddenEnable } = runtimeConfig, foldHandle = null, hiddenHandle = null;
        if (foldEnable) {
            foldHandle = <span><a style={{ color: headerColor && (this.wrapperType === Constants.CARD_WRAPPER_DEFAULT || this.wrapperType === Constants.CARD_WRAPPER_ADVANCED) ? "#fafafa" : undefined, position: 'relative', top: 7, right: 4 }} onClick={this.handleFold}>{fold ? <RightOutlined /> : <DownOutlined />}</a></span>
        }
        if (hiddenEnable) {
            hiddenHandle = <span><a style={{ color: headerColor && (this.wrapperType === Constants.CARD_WRAPPER_DEFAULT || this.wrapperType === Constants.CARD_WRAPPER_ADVANCED) ? "#fafafa" : undefined, position: 'relative', top: 7, right: 4 }} onClick={this.handleClose}><CloseOutlined /></a></span>
        }
        let toolbarLeft = null, toolbarRight = null;
        if (filterConfig.title && filterConfig.title === "TAB过滤项" && filterConfig.tabPosition && filterConfig.tabPosition === 'left') {
            toolbarLeft = <ToolBar {...this.props} handleParamsChanged={this.handleParamsChanged} hasLeftTab={true} widgetConfig={runtimeConfig} />
            if (foldHandle || hiddenHandle) {
                toolbarRight = (
                    <div style={{ display: "flex", justifyContent: 'space-around' }}>
                        {toolbarItem && <div style={{ marginRight: (foldHandle || hiddenHandle) ? 8 : 0 }}>{toolbarItem}</div>}
                        {foldHandle && <div style={{ marginRight: hiddenHandle ? 8 : 0 }}>{foldHandle}</div>}
                        {hiddenHandle && <div>{hiddenHandle}</div>}
                    </div>
                )
            } else {
                toolbarRight = (
                    <div style={{ display: "flex", justifyContent: 'space-around' }}>
                        {toolbarItem && <div style={{ marginRight: (foldHandle || hiddenHandle) ? 8 : 0 }}>{toolbarItem}</div>}
                    </div>
                )
            }
        } else {
            toolbarLeft = '';
            toolbarRight = (
                <div style={{ display: "flex", justifyContent: 'space-around', height: '38px' }}>
                    <ToolBar {...this.props} handleParamsChanged={this.handleParamsChanged} hasLeftTab={true} widgetConfig={runtimeConfig} />
                    {toolbarItem && <div style={{ marginRight: (foldHandle || hiddenHandle) ? 8 : 0 }}>{toolbarItem}</div>}
                    {foldHandle && <div style={{ marginRight: hiddenHandle ? 8 : 0 }}>{foldHandle}</div>}
                    {hiddenHandle && <div>{hiddenHandle}</div>}
                </div>
            )

        }
        let paddingPix = (pageLayoutType === 'FLUID' && title) ? 8 : 3;
        if (this.wrapperType === Constants.CARD_WRAPPER_TRANSPARENT || this.wrapperType === Constants.CARD_WRAPPER_TITLE_TRANSPARENT) {
            let classNameArr = this.wrapperType === Constants.CARD_WRAPPER_TITLE_TRANSPARENT ? ['abm_frontend_widget_component_wrapper', 'transparent-panel'] : ['abm_frontend_widget_component_wrapper']
            return (
                <div>
                    <section style={{ display: "flex", alignItems: "center", justifyContent: 'space-between', height: 54, verticalAlign: 'middle',background:'transparent',paddingLeft: paddingPix }}>
                        <div style={{ display: "flex", justifyContent: 'flex-start' }}>
                            {
                                title && <h2 style={{ fontSize: 16, marginRight: '12px' }}><JSXRender jsx={title} /></h2>
                            }
                            {toolbarLeft}
                        </div>
                        {toolbarRight}
                    </section>
                    <div>
                        {
                            this.wrapperType === Constants.CARD_WRAPPER_TITLE_TRANSPARENT ?
                                <Card size="small"
                                    bodyStyle={{
                                        overflow: widgetModel.config.gridPos ? "auto" : "none",
                                        height: cardHeight === 'auto' ? 'auto' : (calcHeight - (toolbarItem ? 46 : 42)),
                                        padding: 8,
                                        backgroundColor: backgroundColor ? backgroundColor : undefined,
                                        display: fold ? 'none' : undefined
                                    }}
                                    className="abm_frontend_widget_component_wrapper"
                                    style={widgetModel.config.style || {}}>
                                    {cardContent}
                                </Card> :
                                <div
                                    style={Object.assign({
                                        overflow: widgetModel.config.gridPos ? "auto" : "none",
                                        display: fold ? 'none' : undefined,
                                        height: cardHeight === 'auto' ? 'auto' : (calcHeight - 54), padding: 0
                                    }, widgetModel.config.style)}
                                    className={classNameArr.join(" ")}
                                >
                                    {cardContent}
                                </div>
                        }
                    </div>
                </div>
            )
        }
        if (widgetModel.hasToolbar() || foldEnable || hiddenEnable || title) {
            headerExist = true;
        }
        return (
            <Card size="small" style={widgetModel.config.style || {}}
                bordered={cardBorder}
                className="abm_frontend_widget_component_wrapper"
                bodyStyle={{
                    overflow: widgetModel.config.gridPos ? "auto" : "none",
                    height: cardHeight === 'auto' ? 'auto' : (calcHeight - (toolbarItem ? 46 : 42)),
                    padding:8,
                    backgroundColor: backgroundColor ? backgroundColor : undefined,
                    display: fold ? 'none' : undefined
                }}
                headStyle={{
                    backgroundColor: headerColor ? (headerColor==='theme'? 'var(--PrimaryColor)' : headerColor): undefined,
                }}
                title={
                    headerExist && <div className="card-title-wrapper">
                        {!headerColor && (this.wrapperType !== Constants.CARD_WRAPPER_ADVANCED) && title && <div className="card-wrapper-title-prefix" />}
                        <div style={{ display: 'flex' }}>
                            {
                                title && <h2 style={{ margin: "auto", paddingLeft: '10px', fontSize: 14, marginRight: title ? '12px' : '0px' }}><JSXRender jsx={title || ''} /></h2>
                            }
                            {toolbarLeft}
                        </div>
                    </div>
                }
                extra={headerExist ? toolbarRight : null}
            >
                {cardContent}
            </Card>
        )
    }
}
