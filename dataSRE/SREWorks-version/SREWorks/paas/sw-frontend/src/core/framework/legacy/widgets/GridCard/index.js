/**
 * gridCard
 */

import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { DownOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { Spin, Card, Avatar, Popover, List, Dropdown, Menu, Pagination } from 'antd';
import _ from 'lodash';
import './index.less';
import * as util from '../../../../../utils/utils';
import httpClient from '../../../../../utils/httpClient';
import safeEval from '../../../../../utils/SafeEval';

const { Meta } = Card;
const colors = ['#90ee90', '#2191ee', '#9a69ee', '#41ee1a', '#484aee', '#6B8E23', '#48D1CC', '#3CB371', '#388E8E', '#1874CD'];

class GridCard extends React.Component {

    constructor(props) {
        super(props);
        let apiConf = _.get(props.mode, "config.api");
        this.dataSourcePaging = (this.props.widgetConfig && this.props.widgetConfig.paging) || false
        if (apiConf) {
            //apiConf=JSON.parse(util.renderTemplateString(JSON.stringify(apiConf),this.getNodeParams()));
            apiConf = this.getApiConf();
        }
        if (this.props.widgetConfig && this.props.widgetConfig.dataSourceMeta && this.props.widgetConfig.dataSourceMeta.api) {
            this.props.widgetConfig.dataSourceMeta['url'] = this.props.widgetConfig.dataSourceMeta.api
            let obj = {
                ...this.props.widgetConfig.dataSourceMeta,
                url: this.props.widgetConfig.dataSourceMeta.api
            }
            let nodeParams = this.getNodeParams();
            apiConf = JSON.parse(JSON.stringify(obj), function (key, value) {
                if (typeof value === 'string' && value.includes("$")) {
                    let paramKey = value.replace("$(", "").replace(")", "");
                    if (nodeParams[paramKey]) {
                        return nodeParams[paramKey];
                    }
                    return util.renderTemplateString(value, nodeParams)
                }
                return value;
            });
        } else {
            apiConf = this.getApiConf();
        }
        this.state = {
            loading: true,
            columns: null,
            data: this.props.widgetConfig.widgetData? this.props.widgetConfig.widgetData : [],
            reloadCount: 0,
            apiConf: apiConf,
            footChecked: false,
            footIndete: false,
            rowActions: [],
            pageData: [],
            page: 1,
            total: 0,
            pageSize: 20
        }

    }

    getApiConf = () => {
        let apiConf = _.get(this.props.mode, "config.api"), nodeParams = this.getNodeParams();
        if (apiConf) {
            apiConf['params'] = (apiConf && apiConf.params) || {};
        }
        // apiConf.params = apiConf.params || {};
        return util.renderTemplateJsonObject(apiConf, nodeParams);
    };

    getNodeParams = () => {
        let { parameters, nodeParams, rowData = {}, widgetDefaultParams = {}, formInitParams = {} } = this.props;
        return Object.assign({}, formInitParams, widgetDefaultParams, parameters, nodeParams, rowData);
    };
    componentWillMount() {
        const { mode } = this.props;
        let { widgetConfig={} } = this.props;
        let { rowActions, api, paging = false, filters, dynamicColumnsUrl } = mode.config, columns = [], dispalyItems = false,
            hasOper = false;
        columns = (mode.config.columns || []).map(mc => {
            mc.title = mc.label || mc.title;
            mc.key = mc.dataIndex;
            return mc;
        });
        //存在操作的时候动态增加一个操作列
        if (rowActions) {
            hasOper = true;
            this.setState({
                rowActions
            })
        }
        let noPaging = (paging === "false" || paging === false) || (api && (api.paging === "false" || api.paging === false)),
            apiConf;
        // if (api) {
        //     //let nowRenderString=util.renderTemplateString(JSON.stringify(api),this.getNodeParams());
        //     //apiConf=JSON.parse(nowRenderString);
        //     apiConf = this.getApiConf();
        // }
        this.setState({ columns: columns });
        this.setState({
            tableConf: mode,
            dispalyItems: dispalyItems,
            loading: false,
            data: [],
            // apiConf: apiConf
        }, () => {
            let api = _.get(mode, "config.api");
            //let nowRenderString=util.renderTemplateString(JSON.stringify(api),this.getNodeParams());
            if (api && api.paging && (api.paging === false || api.paging === "false")) {
                //不分页的一次加载完
                this.formatData()
            } else {
                this.formatData()
            }
        });

    }

    formatData = (filterParams = {}) => {
        let { mode, apiConf = {}, nodeParams = {}, widgetConfig = {} } = this.props;
        let afterResponseHandler = '';
        if (widgetConfig && widgetConfig.dataSourceMeta) {
            afterResponseHandler = widgetConfig.dataSourceMeta['afterResponseHandler']
        }
        let runtimeUrl = {};
        if (widgetConfig && widgetConfig.dataSourceMeta) {
            runtimeUrl = widgetConfig.dataSourceMeta
        }
        runtimeUrl && runtimeUrl.api && this.loadAllData(runtimeUrl, filterParams).then(respData => {
            if (afterResponseHandler && afterResponseHandler.length > 50) {
                respData = safeEval("(" + afterResponseHandler + ")(respData,nodeParams,httpClient)", { respData: respData, nodeParams: nodeParams, httpClient: httpClient });
                if (respData instanceof Promise) {
                    respData.then(res => {
                        let dataIndex = mode.config.dataIndex, tdata = res && res.items || res || [];
                        if (dataIndex) tdata = res[dataIndex] || [];
                        this.setState({
                            loading: false,
                            data: tdata,
                            pageData: this.dataSourcePaging ? tdata : tdata.slice(0, this.state.pageSize),
                            total: res.total,
                        });
                    })
                    return
                }
            }
            let dataIndex = mode.config.dataIndex, tdata = respData && respData.items || respData || [];
            if (dataIndex) tdata = respData[dataIndex] || [];
            this.setState({
                loading: false,
                data: tdata,
                pageData: this.dataSourcePaging ? tdata : tdata.slice(0, this.state.pageSize),
                total: respData.total,
            });
        })
    }

    loadAllData = (api, filterParams = {}) => {
        if(api && (api.url || api.api)) {
            this.setState({
                loading: true,
            });
        } else {
            return false
        }
        let { page, pageSize, apiConf } = this.state;
        let beforeRequestHandler = '';
        const { parameters, nodeParams, mode, nodeId, widgetConfig = {}, widgetDefaultParams } = this.props;
        if (!api) {
            let params = {
                nodeId: nodeId,
                elementId: mode.elementId,
                parameters: parameters,
            };
            return Promise.resolve([]);
        } else {
            if (widgetConfig.dataSourceMeta) {
                beforeRequestHandler = widgetConfig.dataSourceMeta['beforeRequestHandler']
            }
            let confParams = Object.assign(_.get(api, "params") || {}, util.getUrlParams(), { ___refresh_timestamp: nodeParams.___refresh_timestamp });
            let reqParams = {};
            // 造成get方式参数重复
            Object.keys(apiConf.params || {}).forEach(key => {
                reqParams[key] = confParams[key]
            });
            if (this.dataSourcePaging) {
                reqParams['page'] = page;
                reqParams['pageSize'] = pageSize;
            }
            if (beforeRequestHandler && beforeRequestHandler.length > 50) {
                let funcParams = safeEval("(" + beforeRequestHandler + ")(nodeParams)", { nodeParams: nodeParams });
                reqParams = {...reqParams,...funcParams}
            }
            let finalUrl = api.url || api.api;
            if(!finalUrl) {
                return false
            }
            if(finalUrl.includes('$(')) {
                finalUrl = util.renderTemplateString(finalUrl,nodeParams)
            }
            if (api.method === 'POST') {
                return httpClient.post(finalUrl, reqParams);
            } else {
                return httpClient.get(finalUrl, { params: reqParams })
            }
        }

    };

    componentDidUpdate(prevProps) {
        const { mode, parameters, nodeParams } = this.props, { apiConf } = this.state;
        if (!_.isEqual(prevProps.nodeParams, this.props.nodeParams)) {//前端api方式配置
            let api = _.get(mode, "config.api") || apiConf;
            //let nowRenderString=util.renderTemplateString(JSON.stringify(api),this.getNodeParams());
            if (api.paging === false || api.paging === "false") {
                //不分页的一次加载完
                this.formatData()
            } else {
                //let newApiConf=JSON.parse(nowRenderString);
                this.setState({
                    page: 1,
                    pageSize: 20
                }, () => {
                    this.formatData()
                })
            }
        }

    }

    handleOpenRowAction = (name, record) => {
        const { openAction, mode } = this.props;
        let outputs = mode.config.outputs, params = { ...record };
        if (outputs) {
            outputs.forEach(output => {
                if (output.valueType === 'List') {
                    params[output.name] = [record];
                } else {
                    params[output.name] = record[output.valueIndex];
                }
            })
        }
        openAction(name, params)
    };

    descriptionRender = (item) => {
        let { mode } = this.props;
        let flex = mode.config && (mode.config.flex || 2);
        let style = { width: (100 / flex) + '%' };
        let { columns } = this.state;
        return <div className="description-wrap">
            {columns.map((column, index) => {
                return <div key={index} className="description-item" style={style}>
                    <div className="description-item-title">{column.label}</div>
                    <div className="description-item-value">
                        <Popover content={_.get(item, column.dataIndex) || "-"}>
                            {_.get(item, column.dataIndex) || "-"}
                        </Popover></div>
                </div>
            })}
        </div>
    }
    descriptionRenderBackup = (item) => {
        let { mode } = this.props;
        let flex = mode.config && (mode.config.flex || 2);
        let style = { width: '100%' };
        let { columns } = this.state;
        return <div className="description-wrap-backup">
            <div style={{ width: '50%' }}>
                {columns && columns.slice(1).map((column, index) => {
                    return <div key={index} className="description-item-backup" style={style}>
                        <div className="description-item-title">
                            {column.label}
                            <span> : </span>
                            <span className="description-item-value-in">
                                <Popover content={_.get(item, column.dataIndex) || "-"}>
                                    {_.get(item, column.dataIndex) || "-"}
                                </Popover></span>
                        </div>
                    </div>
                })}
            </div>
            <div style={{ width: '50%' }}>
                {columns && columns[0] && [columns[0]].map((column, index) => {
                    return <div key={index} className="description-item-backup" style={{ justifyContent: 'center', padding: 'auto 0' }}>
                        <div className="description-item-title" style={{ textAlign: 'center' }}>{column.label}</div>
                        <div className="description-item-value-out" style={{ textAlign: 'center', marginTop: 15 }}>
                            <Popover content={_.get(item, column.dataIndex) || "-"}>
                                {_.get(item, column.dataIndex) || "-"}
                            </Popover></div>
                    </div>
                })}
            </div>
        </div>
    }

    actionsRender = (rowActions, record) => {
        let actions = [];
        let moreAction = [];
        (rowActions.actions || []).map((item, index) => {
            if (rowActions.actions.length > 3 && index > 1) {
                moreAction.push(item)
            } else {
                actions.push(<div onClick={() => this.handleOpenRowAction(item.name, record)}>
                    {item.icon && <LegacyIcon type={item.icon} key={item.icon} />}
                    <span style={{ marginLeft: 10 }}>{item.label || '-'}</span>
                </div>)
            }
        })
        if (!!moreAction.length) {
            const menu = <Menu>
                {moreAction.map((item, index) => {
                    return (
                        <Menu.Item key={index}>
                            <div onClick={() => this.handleOpenRowAction(item.name, record)}>
                                {item.icon && <LegacyIcon type={item.icon} key={item.icon} />}
                                <span style={{ marginLeft: 10 }}>{item.label || '-'}</span>
                            </div>
                        </Menu.Item>
                    );

                })}
            </Menu>
            actions.push(<Dropdown overlay={menu}>
                <div>
                    更多 <DownOutlined />
                </div>
            </Dropdown>)
        }
        return actions;
    }
    onChange = (currentPage, size) => {
        // this.setState({page}, () => {
        //     this.formatData({page, pageSize: this.state.pageSize})
        // })
        if (this.dataSourcePaging) {
            this.setState({
                page: currentPage,
                pageSize: size
            }, () => this.formatData())
        } else {
            let { data } = this.state;
            this.setState({
                page: currentPage,
                pageData: data.slice((currentPage - 1) * size, currentPage * size),
                pageSize: size

            })
        }
    }
    render() {
        let { mode, nodeId, parameters, openAction, handleParamsChanged, renderComponents, nodeParams, widgetConfig = {}, ...contentProps } = this.props,
            { columns, loading, rowActions, data, pageData, page, pageSize } = this.state;
        let { layoutSelect = 'advance' } = widgetConfig;
        if (!columns) return <Spin style={{ width: "100%" }} />;
        if (pageData.length === 0 && !(this.state.apiConf && this.state.apiConf.api)) {
            pageData = [
                {
                    age: "111",
                    icon: "https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png",
                    name: "张三",
                    title: "测试测试测试"
                },
                {
                    age: "112",
                    icon: "https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png",
                    name: "李四",
                    title: "测试测试测试"
                },
                {
                    age: "111",
                    icon: "https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png",
                    name: "张三",
                    title: "测试测试测试"
                }
            ]
        }
        return (
            <Spin spinning={loading}>
                <List
                    style={mode.config.cStyle || {}}
                    className="grid-card"
                    grid={mode.config.grid || { gutter: 16, column: 4 }}
                    dataSource={pageData}
                    renderItem={(item, r) => {
                        let title = _.get(item, mode.config.columnTitle || mode.config.title) || item.title;
                        let icon = _.get(item, mode.config.icon) || item.icon;
                        let avatar = icon ? <Avatar shape="square" src={icon} /> : <Avatar style={{
                            backgroundColor: colors[r % 10],
                            verticalAlign: 'middle',
                            fontSize: '18px'
                        }}>
                            {title && (title).substring(0, 1)}
                        </Avatar>;
                        let itemRowAction = item.rowActions && !!item.rowActions.actions.length ? item.rowActions : rowActions
                        return (
                            <List.Item key={r}>
                                <Card
                                    hoverable
                                    actions={this.actionsRender(itemRowAction, item)}>
                                    <Meta
                                        avatar={avatar}
                                        title={title || item.label || item.name}
                                        description={layoutSelect === 'advance' ? this.descriptionRenderBackup(item) : this.descriptionRender(item)}
                                    />
                                    {mode.config.toolTip && _.get(item, mode.config.toolTip) &&
                                        <Popover content={_.get(item, mode.config.toolTip)}>
                                            <QuestionCircleOutlined className="tooltip-icon" /></Popover>}
                                </Card>
                            </List.Item>
                        );
                    }}
                />
                <Pagination style={{ textAlign: 'right' }}
                    showSizeChanger
                    showQuickJumper
                    current={page}
                    showTotal={total => `共 ${total} 条`}
                    defaultPageSize={pageSize}
                    defaultCurrent={page}
                    total={this.state.total}
                    onChange={this.onChange} />
            </Spin>
        );
    }
}

export default GridCard
