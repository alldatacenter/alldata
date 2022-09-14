/**
 * Created by caoshuaibiao on 2019/1/8.
 * 简单表格展示
 */

import React from 'react';
import PagingTable from "../../../../../components/PagingTable";
import SimpleForm from "../../../../../components/FormBuilder/SimpleForm";
import properties from 'appRoot/properties';
import { Spin, Button, Card, Modal, Tooltip, List, Dropdown, Menu, Checkbox } from 'antd';
import _ from 'lodash';
import ActionsRender from '../../../ActionsRender';
import LinksRender from '../../../LinksRender';
import httpClient from "../../../../../utils/httpClient";
import localeHelper from "../../../../../utils/localeHelper";
import * as util from "../../../../../utils/utils";
import './index.less';
import OamWidget from '../../../../../core/framework/OamWidget';
import DefaultItemToolbar from '../../../../../core/framework/core/DefaultItemToolbar';
import PagedTable from './PagedTable';

const rowClassMapping = {
    "red": "bg_red",
    "blue": "bg_blue",
    "green": "bg_green",
    "yellow": "bg_yellow",
    "themeColor": "bg_theme"
};

class SimpleTable extends React.Component {

    constructor(props) {
        super(props);
        let apiConf = this.getApiConf();
        this.state = {
            loading: true,
            columns: null,
            tableConf: null,
            selectedRowKeys: [],
            clickRow: null,
            data: props.widgetData || '',
            filterParams: false,
            dispalyItems: false,
            reloadCount: 0,
            apiConf: apiConf,
            footChecked: false,
            footIndete: false
        };
    }

    getApiConf = () => {
        let apiConf = _.get(this.props.mode, "config.api"), nodeParams = this.getNodeParams();
        if (apiConf) {
            apiConf = JSON.parse(JSON.stringify(apiConf), function (key, value) {
                if (typeof value === 'string' && value.includes("$")) {
                    let paramKey = value.replace("$(", "").replace(")", "");
                    if (nodeParams[paramKey]) {
                        return nodeParams[paramKey];
                    }
                    return util.renderTemplateString(value, nodeParams)
                }
                return value;
            });
        }
        return apiConf;
    };

    getNodeParams = () => {
        let { parameters, nodeParams, rowData = {}, widgetDefaultParams = {}, formInitParams = {} } = this.props;
        return Object.assign({}, formInitParams, widgetDefaultParams, parameters, nodeParams, rowData);
    };

    componentWillMount() {
        const { mode, nodeId, openAction, parameters, nodeParams, widgetData, widgetConfig = {} } = this.props;
        let { rowActions, api, paging = false, filters, dynamicColumnsUrl } = mode.config, columns = [], dispalyItems = false, hasOper = false, { itemToolbar } = widgetConfig;
        if (this.props.widgetConfig && this.props.widgetConfig.dataSourceMeta) {
            paging = (this.props.widgetConfig && this.props.widgetConfig.paging) || false
        }
        let { apiConf } = this.state;
        columns = mode.config.columns.map(mc => {
            mc.title = mc.label || mc.title;
            mc.key = mc.dataIndex;
            //增加行动态Action支持
            if (mc.type === 'ACTION') {
                mc.fixed = mc.hasOwnProperty("fixed") ? mc.fixed : 'right';
                mc.key = mc.key || '_oper';
                mc.sorter = false;
                mc.filterDropdown = false;
                mc.render = (text, record) => record.rowActions ? <ActionsRender {...record.rowActions} nodeParams={this.getNodeParams()} record={record} openAction={this.handleOpenRowAction} /> : <span />;
            }
            return mc;
        });
        //存在操作的时候动态增加一个操作列
        if (rowActions) {
            hasOper = true;
            columns.push({
                title: localeHelper.get('operation', '操作'),
                key: '_oper',
                fixed: rowActions.hasOwnProperty("fixed") ? rowActions.fixed : 'right',
                sorter: false,
                filterDropdown: false,
                render: (text, record) => <ActionsRender {...rowActions} nodeParams={this.getNodeParams()} record={record} openAction={this.handleOpenRowAction} />
            });
        } else if (itemToolbar && itemToolbar.actionList && itemToolbar.actionList.length) {//增加新的可视化row toolbar适配
            hasOper = true;
            columns.push({
                title: localeHelper.get('operation', '操作'),
                key: '_oper',
                fixed: itemToolbar.hasOwnProperty("fixed") ? itemToolbar.fixed : 'right',
                sorter: false,
                width: itemToolbar.hasOwnProperty("width") ? itemToolbar.width : '',
                filterDropdown: false,
                render: (text, record) => <DefaultItemToolbar {...this.props} itemToolbar={itemToolbar} itemData={record} actionParams={Object.assign({}, this.props.actionParams, record)} widgetConfig={widgetConfig} />
            });
        }
        if (filters) {
            dispalyItems = [];
            columns.forEach(c => {
                if (filters.includes(c.dataIndex)) {
                    dispalyItems.push({ type: 1, name: c.dataIndex, initValue: '', required: false, label: c.title, inputTip: localeHelper.get('myProject.searchHint', '输入{title}后按 Enter 键进行查询', { title: c.title }) });
                }
            })
        }
        let noPaging = (paging === "false" || paging === false) || (api && (api.paging === "false" || api.paging === false));
        if (dynamicColumnsUrl) {
            httpClient.get(util.renderTemplateString(dynamicColumnsUrl, this.getNodeParams())).then(dcs => {
                let oc = null;
                if (hasOper) {
                    oc = columns.pop();
                }
                columns = columns.concat(dcs.map(d => { d.title = d.title || d.label; return d }));
                if (oc) columns.push(oc);
                this.setState({ columns });
            })
        } else {
            this.setState({ columns: columns });
        }
        this.setState({
            tableConf: mode,
            dispalyItems: dispalyItems,
            loading: false,
            data: this.state.data || (noPaging ? [] : false),
        });
        if (noPaging && ((widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.api) || (apiConf && apiConf.url))) {
            this.loadAllData(api).then(data => {
                let dataIndex = mode.config.dataIndex, tdata = data && data.items || data || [];
                if (dataIndex) tdata = data[dataIndex] || [];
                this.setState({
                    loading: false,
                    data: tdata
                });
            })
        }

    }

    loadAllData = (api, filterParams = {}) => {
        let { apiConf } = this.state;
        let { widgetData } = this.props;
        if (widgetData && !api) {
            return Promise.resolve(widgetData)
        }
        this.setState({
            loading: true,
        });
        const { parameters, nodeParams, mode, nodeId, widgetDefaultParams } = this.props;
        if (!api) {
            let params = {
                nodeId: nodeId,
                elementId: mode.elementId,
                parameters: parameters,
            };
            return Promise.resolve([]);
        } else {
            let apiString = JSON.stringify(api);
            if (apiString.indexOf("$") >= 0) {
                //let nowRenderString=util.renderTemplateString(apiString,this.getNodeParams());
                //api=JSON.parse(nowRenderString);
                api = this.getApiConf();
            }
            let reqParams = Object.assign({}, api.params, filterParams);
            if (api.method === 'POST') {
                return httpClient.post(api.url, reqParams);
            } else {
                return httpClient.get(api.url, { params: reqParams })
            }
        }

    };

    onSelectChange = (selectedRowKeys, selectedRows) => {
        let sAll = false, checkAll = false;
        if (this.paging_table) {
            let data = this.paging_table.getData();
            sAll = selectedRowKeys.length > 0 && selectedRowKeys.length < data.length;
            checkAll = data.length === selectedRowKeys.length;
        }
        this.setState({ selectedRowKeys, footIndete: sAll, footChecked: checkAll });
        let { handleParamsChanged, mode } = this.props;
        //table默认关闭节点刷新页面
        this.handleDefaultOutputs(mode.config.outputs);
        handleParamsChanged && handleParamsChanged(selectedRows, mode.config.outputs);
    };

    handleDefaultOutputs = (outputs) => {
        if (outputs) {
            outputs.forEach(op => {
                if (op.reload !== true) {
                    op.reload = false
                }
            })
        }
    };
    componentDidUpdate(prevProps) {
        let { mode, parameters, nodeParams } = this.props, { apiConf } = this.state;
        if (apiConf && apiConf.url && !_.isEqual(prevProps.nodeParams, nodeParams)) {//前端api方式配置
            let api = apiConf;
            //let nowRenderString=util.renderTemplateString(JSON.stringify(api),this.getNodeParams());
            if (api.paging === false || api.paging === "false") {
                //不分页的一次加载完
                this.loadAllData(api).then(data => {
                    let dataIndex = mode.config.dataIndex, tdata = data && data.items || data || [];
                    if (dataIndex) tdata = data[dataIndex] || [];
                    this.setState({
                        loading: false,
                        reloadCount: this.state.reloadCount + 1,
                        data: tdata,
                    });
                });
            }
        }
        else if (!_.isEqual(prevProps.nodeParams, nodeParams)) {//datasource形式
            let paging = (apiConf && apiConf.paging) || false;
            if (paging === "false" || paging === false) {
                this.loadAllData().then(data => {
                    let dataIndex = mode.config.dataIndex, tdata = data && data.items || data || [];
                    if (dataIndex) tdata = data[dataIndex] || [];
                    this.setState({
                        loading: false,
                        reloadCount: this.state.reloadCount + 1,
                        data: tdata,
                    });
                });
            } else {
                this.setState({
                    reloadCount: this.state.reloadCount + 1,
                });
            }
        }

    }

    onClickRow = (clickRow) => {
        let { handleParamsChanged, mode } = this.props;
        this.setState({
            clickRow: clickRow
        });
        this.handleDefaultOutputs(mode.config.outputs);
        handleParamsChanged && handleParamsChanged(clickRow, mode.config.outputs);
    };

    handleFilterChanged = (values) => {
        const { mode } = this.props;
        let { api } = mode.config;
        if (api && (api.paging === "false" || api.paging === false)) {
            this.loadAllData(api, values).then(data => {
                let dataIndex = mode.config.dataIndex, tdata = data && data.items || data;
                if (dataIndex) tdata = data[dataIndex] || [];
                this.setState({
                    loading: false,
                    filterParams: values,
                    data: tdata
                });
            });
        } else {
            this.setState({
                filterParams: values
            });
        }
    };

    handleOpenRowAction = (name, record, callback) => {
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
        openAction(name, params, callback)
    };

    handleRefresh = () => {
        const { handleParamsChanged } = this.props;
        handleParamsChanged && handleParamsChanged({ ___refresh_timestamp: (new Date()).getTime() });
    };

    handleFooterChecked = (e) => {
        let data = this.paging_table.getData();
        if (e.target.checked) {
            this.onSelectChange(data.map(d => d.__row_uuid), data);
        } else {
            this.onSelectChange([], [])
        }
    };

    getExportData = () => {
        //获取导出数据,对于不分页的直接返回data,分页的每次最多导出一万条，进行查询后导出
        let { data } = this.state, { mode } = this.props;
        if (data) {
            return Promise.resolve(data)
        } else {
            let { api, dataIndex } = mode.config;
            return this.loadAllData(api, { page: 1, pageSize: 10000 }).then(data => {
                this.setState({
                    loading: false
                });
                let tdata = data && data.items || data;
                if (dataIndex) tdata = data[dataIndex] || [];
                return tdata;
            })
        }

    };

    handleExport = () => {
        let { mode } = this.props;
        this.getExportData().then(oriData => {
            //获取导出数据后,按照定义的列生成导出数据,进行导出
            let { columns, title = 'export' } = mode.config;
            util.exportCsv(columns, oriData, title + ".csv");
        })

    };

    handleOpenAction = (actionName, actionParams, callBack) => {
        let { mode, nodeId, parameters, openAction, handleParamsChanged, renderComponents, nodeParams } = this.props;
        //table内置的导出处理
        if (actionName === '__export__') {
            this.handleExport();
        } else {
            openAction(actionName, actionParams, callBack);
        }
    };
    render() {
        let { mode, nodeId, parameters, openAction, handleParamsChanged, renderComponents, nodeParams, widgetConfig = {}, dispatch, ...contentProps } = this.props, { columns, loading, selectedRowKeys, clickRow, data, filterParams, dispalyItems, reloadCount, apiConf, footChecked, footIndete } = this.state;
        if (!columns) return <Spin style={{ width: "100%" }} />;
        let confParams = Object.assign(_.get(apiConf, "params") || {}, util.getUrlParams(), { ___refresh_timestamp: nodeParams.___refresh_timestamp }), allParams = this.getNodeParams();
        let tableParams = {
            nodeId: nodeId,
            elementId: mode.elementId,
            parameters: parameters,
            ...confParams
        };
        if (filterParams) {
            tableParams.filters = filterParams
        }
        let dynamiConf = {}, { outputs, selectable = {}, checkbox, border, headerActions, headerLinks, title, api, footerActions, customPagination = {}, expandedRow, size = "small", bordered = false, rowColorMapping } = mode.config;
        if (outputs && (checkbox !== false && checkbox !== 'false') && outputs.length) {
            let rowSelection = {};
            //rowSelection.type=selectable.type||'checkbox';
            rowSelection.selectedRowKeys = selectedRowKeys;
            rowSelection.onChange = this.onSelectChange;
            dynamiConf.rowSelection = rowSelection;
        }
        //行根据值显示不同的颜色
        if (rowColorMapping && Object.keys(rowColorMapping).length > 0 && rowColorMapping.dataIndex) {
            let colorMappingKey = rowColorMapping.dataIndex;
            dynamiConf.rowClassName = (record, index) => {
                //数字/boolean类型无法做key因此转为字符串
                try {
                    let keyValue = _.get(record, colorMappingKey) + "";
                    return rowClassMapping[rowColorMapping.mapping[keyValue]]
                } catch (err) {
                    return ""
                }
            }
        }
        if (selectable.type === 'click') {
            dynamiConf.rowClassName = (record, index) => {
                if (clickRow == record) {
                    return "ant-table-row-hover"
                }
                return "";
            };
            dynamiConf.onRow = (record) => {
                return {
                    onClick: (event) => {
                        this.onClickRow(record);
                    }
                };
            }
        }
        let titleItem = null, headerActionItem = null, headerLinkItem = null, footerActionItem = null;
        if (title || headerActions || headerLinks) {
            if (title) {
                titleItem = (
                    <div key="__title">
                        <h4 style={{ fontSize: 13 }} key="__title">{title}</h4>
                    </div>
                )
            }
            if (headerActions) {
                headerActionItem = (
                    <div key="__actions" style={{ fontSize: 12 }}>
                        <ActionsRender  {...headerActions} nodeParams={allParams} openAction={this.handleOpenAction} handleParamsChanged={handleParamsChanged} handleRefresh={this.handleRefresh} />
                    </div>
                )
            }
            if (headerLinks) {
                headerLinkItem = (
                    <div key="__link">
                        <LinksRender  {...headerLinks} />
                    </div>
                )
            }
            dynamiConf.title = () => {
                return (
                    <div key="__dtitle" style={{ display: "flex", justifyContent: "space-between" }}>
                        <div>
                            {title ? titleItem : headerActionItem}
                        </div>
                        <div style={{ display: "flex" }}>
                            {title ? headerActionItem : null}
                            {headerLinkItem}
                        </div>
                    </div>
                )
            }
        }
        if (footerActions) {
            let slectAllCheckbox = null;
            if (dynamiConf.rowSelection) {
                slectAllCheckbox = <Checkbox style={{ marginRight: 12, marginLeft: 24 }} indeterminate={footIndete} checked={footChecked} onChange={this.handleFooterChecked} />;
            }
            footerActionItem = (
                <div key="__footerActions" style={{ fontSize: 12, display: 'flex', marginTop: -42 }}>
                    {slectAllCheckbox}
                    <ActionsRender  {...footerActions} nodeParams={allParams} openAction={openAction} handleParamsChanged={handleParamsChanged} handleRefresh={this.handleRefresh} />
                </div>
            )
        }
        let filterForm = null;
        if (dispalyItems) {
            const formItemLayout = {
                labelCol: {
                    xs: { span: 24 },
                    sm: { span: 6 },
                    md: { span: 4 },
                },
                wrapperCol: {
                    xs: { span: 24 },
                    sm: { span: 18 },
                    md: { span: 16 },
                },
            };
            filterForm = (
                <div className="widget_table_filter_form">
                    <SimpleForm
                        items={dispalyItems}
                        colCount={dispalyItems.length > 1 ? dispalyItems.length : 2}
                        enterSubmit={true}
                        formItemLayout={formItemLayout}
                        onSubmit={this.handleFilterChanged}
                    />
                </div>
            );
        }
        if (api && api.method === 'POST') {
            dynamiConf.dataMethod = 'POST';
        }
        if (expandedRow && Object.keys(expandedRow).length) {
            dynamiConf.expandedRowRender = record => <OamWidget openAction={openAction} widget={mode.__template.config.expandedRow} parameters={parameters} nodeParams={nodeParams} nodeId={nodeId} rowData={record} widgetParams={record} />
        }
        let tableKey = "table_reload_" + reloadCount + data && data.length, { wrapper } = widgetConfig, dyClass = "inner-table globalBackground";
        if (size !== "small") {
            dyClass = dyClass + " ant-table-small";
        }
        if (wrapper === "none" || wrapper === 'transparent') {
            dyClass = dyClass + " no_wrapper_table_pagination";
        }
        return (
            <div className={dyClass + ' widget_table_row'}>
                {filterForm}
                <Spin spinning={loading} key={tableKey}>
                    {
                        widgetConfig && widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.api ?
                            <PagedTable
                                key={tableKey}
                                ref={r => this.paging_table = r}
                                params={tableParams}
                                columns={columns}
                                apiConf={apiConf}
                                dataSourceMeta={widgetConfig.dataSourceMeta}
                                openAction={openAction}
                                renderComponents={renderComponents}
                                scrollX={mode.config.scrollX}
                                scrollY={mode.config.scrollY}
                                handleParamsChanged={handleParamsChanged}
                                dataIndex={mode.config.dataIndex}
                                extParams={allParams}
                                size={size}
                                nodeParams={nodeParams}
                                widgetConfig={widgetConfig}
                                {...dynamiConf}
                            />
                            : <PagingTable key={tableKey}
                                ref={r => this.paging_table = r}
                                dataUrl={apiConf && apiConf.url ? apiConf.url : ""}
                                params={tableParams}
                                columns={columns}
                                data={data}
                                dispatch={dispatch}
                                scrollX={mode.config.scrollX}
                                scrollY={mode.config.scrollY}
                                renderComponents={renderComponents}
                                openAction={openAction}
                                handleParamsChanged={handleParamsChanged}
                                customPagination={customPagination}
                                dataIndex={mode.config.dataIndex}
                                extParams={allParams}
                                size={size}
                                nodeParams={nodeParams}
                                widgetConfig={widgetConfig}
                                {...dynamiConf}
                            />

                    }
                    {footerActionItem}
                </Spin>
            </div>
        )
    }
}

export default SimpleTable