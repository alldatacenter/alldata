import React, { Component } from 'react';
import { Table, Pagination, Tooltip, Tag } from "antd";
import JSXRender from '../../../../../../components/JSXRender';
import * as util from '../../../../../../utils/utils'
import httpClient from '../../../../../../utils/httpClient';
import safeEval from '../../../../../../utils/SafeEval';
import Highlighter from 'react-highlight-words';
import uuidv4 from 'uuid/v4';
import _ from 'lodash';

class PagedTable extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: false,
            data: [],
            cacheData: [],
            page: 1,
            pageSize: 10,
            total: 0,
            nodeParams: props.nodeParams,
            pageSizeOptions: [10, 20, 50, 100],
        }
    }
    pageChange = (page, pageSize) => {
        let { widgetConfig } = this.props;
        this.setState({
            page: page,
            pageSize: pageSize
        }, () => {
            if (widgetConfig && widgetConfig.paging) {
                this.recycleFuc()
            } else {
                this.localPageChange()
            }
        })
    }
    localPageChange = () => {
        let { page, pageSize, cacheData } = this.state;
        this.setState({
            data: cacheData.slice((page - 1) * pageSize, page * pageSize)
        })
    }
    componentDidMount() {
        this.setState({
            loading: false
        }, () => this.recycleFuc())
    }
    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(nextProps.nodeParams, this.state.nodeParams)) {
            this.setState({
                loading: true,
                page: 1,
                nodeParams: nextProps.nodeParams
            }, () => {
                this.recycleFuc()
            })
        }
    }
    recycleFuc = () => {
        let { dataSourceMeta = {} } = this.props;
        let { nodeParams } = this.state;
        let { afterResponseHandler } = dataSourceMeta;
        this.loadData().then(respData => {
            if (afterResponseHandler && afterResponseHandler.length > 50) {
                respData = safeEval("(" + afterResponseHandler + ")(respData,nodeParams,httpClient)", { respData: respData, nodeParams: nodeParams, httpClient: httpClient });
                if (respData instanceof Promise) {
                    respData.then(res => {
                        let tdata = res.items || res || [];
                        tdata.forEach(item => {
                            if (!item.__row_uuid) {
                                item.__row_uuid = uuidv4()
                            }
                        })
                        let total = res.total || res.length || tdata.length || 0
                        this.setState({
                            loading: false,
                            data: tdata,
                            cacheData: tdata,
                            total: total
                        });
                    })
                    return
                }
            }
            let tdata = respData.items || respData || [];
            tdata.forEach(item => {
                if (!item.__row_uuid) {
                    item.__row_uuid = uuidv4()
                }
            })
            let total = respData.total || respData.length || tdata.length || 0
            this.setState({
                loading: false,
                data: tdata,
                cacheData: tdata,
                total: total
            });
        }).catch(() => {
            this.setState({
                loading: false,
            });
        })
    }
    loadData = () => {
        let { widgetData, dataSourceMeta, apiConf = {}, widgetConfig } = this.props;
        let { page, pageSize, nodeParams } = this.state;
        let { beforeRequestHandler } = dataSourceMeta;
        if (widgetData && !(widgetConfig && widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.api)) {
            return Promise.resolve(widgetData)
        }
        this.setState({
            loading: true,
        });
        let cloneApiConf = _.cloneDeep(apiConf);
        let CloneDataSourceMeta = _.cloneDeep(dataSourceMeta)
        if (widgetConfig && widgetConfig.dataSourceMeta && widgetConfig.dataSourceMeta.api) {
            let obj = {
                ...this.props.widgetConfig.dataSourceMeta,
                url: this.props.widgetConfig.dataSourceMeta.api
            }
            cloneApiConf = CloneDataSourceMeta
            cloneApiConf = JSON.parse(JSON.stringify(obj), function (key, value) {
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
        if (cloneApiConf) {
            let reqParams = Object.assign({}, { ...cloneApiConf.params });
            if (widgetConfig && widgetConfig.paging) {
                reqParams = { ...reqParams, ...{ page, pageSize } }
            }
            if (beforeRequestHandler && beforeRequestHandler.length > 50) {
                let funcParams = safeEval("(" + beforeRequestHandler + ")(nodeParams)", { nodeParams: nodeParams });
                reqParams = { ...reqParams, ...funcParams }
            }
            if (cloneApiConf.method === 'POST') {
                return httpClient.post(cloneApiConf.url || cloneApiConf.api, reqParams);
            } else {
                return httpClient.get(cloneApiConf.url || cloneApiConf.api, { params: reqParams })
            }
        }

    };
    handleColumnHref = (url, rowData) => {
        let rowParams = {};
        Object.keys(rowData).forEach(rk => {
            rowParams[rk] = (rowData[rk].value ? rowData[rk].value : rowData[rk])
        });
        if (url.startsWith("http")) {
            window.open(url);
        } else {
            let curHref = window.location.href;
            let link = curHref.substring(0, curHref.indexOf("/#/")) + "/#/", path = url.startsWith("/") ? url.substring(1) : url, start = url.indexOf("?"), initParam = "";
            if (start > 0) {
                path = path.substring(0, start - 1);
                initParam = "&" + url.substring(start + 1);
            }
            window.open(link + path + "?" + util.objectToUrlParams(rowParams) + initParam);
        }

    };
    getColumnSearch = (dataIndex, columnDef) => ({
        sorter: (a, b) => {
            let aValue = _.get(a, dataIndex, 0), bValue = _.get(b, dataIndex, 0);
            if (isFinite(aValue) && isFinite(bValue)) {
                return parseFloat(aValue) - parseFloat(bValue);
            }
            if (aValue && bValue.localeCompare) {
                return aValue.localeCompare(bValue)
            }
            return aValue - bValue;
        },
        render: (columnData, rowData) => {
            if (!columnData && columnData !== 0 && columnData !== false) return "";
            let text = (columnData.value || columnData.value === 0 ? columnData.value : columnData), path = columnData.path, color = columnData.color;
            if (text === 0 || text === false) { text = text + "" }
            if (path) {
                return <a onClick={() => this.handleColumnHref(path, rowData)}>{text}</a>;
            }
            if (color) {
                return <span style={{ color: color }}>{text}</span>;
            }
            let tipText = "", hasTiped = false;
            if (columnDef.tooltip) {
                tipText = <JSXRender jsx={util.renderTemplateString(columnDef.tooltip, { row: rowData })} />
            }
            if (columnDef.dict) {
                let dictValue = _.get(columnDef.dict, text + "") || text || "-";
                let { color, label } = dictValue, displayCell = null;
                if (color && label) {
                    displayCell = <Tag color={color}>{label}</Tag>;
                } else {
                    displayCell = <span>{dictValue}</span>;
                }
                if (tipText) {
                    return (
                        <Tooltip title={tipText}>
                            <span>{displayCell}</span>
                        </Tooltip>
                    )
                }
                return displayCell;
            }
            if (columnDef.maxWidth || columnDef.width) {
                hasTiped = true;
                let sto = {};
                if (columnDef.maxWidth) {
                    sto.maxWidth = Number(columnDef.maxWidth);
                }
                if (columnDef.width) {
                    sto.width = isFinite(columnDef.width) ? Number(columnDef.width) : columnDef.width;
                }
                return (
                    <Tooltip title={tipText || text}>
                        <span className='text-overflow' style={sto}>{text}</span>
                    </Tooltip>
                )
            }
            if (tipText && !hasTiped) {
                return (
                    <Tooltip title={tipText}>
                        <span>{text}</span>
                    </Tooltip>
                )
            }
            return (
                <Highlighter
                    highlightStyle={{ backgroundColor: '#ffc069', padding: 0 }}
                    searchWords={[this.state.searchText]}
                    autoEscape
                    textToHighlight={text.toString && text.toString()}
                />
            )
        },
    });
    render() {
        const { style, scrollX, scrollY, className, columns, renderComponents, openAction, handleParamsChanged, dataUrl, extParams = {},widgetConfig, ...otherTableProps } = this.props;
        const { page, pageSize, pageSizeOptions, data, loading, total } = this.state;
        let {emptyText='',bordered,size="small"} = widgetConfig;
        let tableColumns = columns && columns.map(c => {
            let columnDef = Object.assign({ ...this.getColumnSearch(c.dataIndex, c) }, c);
            // if(c.filters){
            //     delete columnDef.filterDropdown
            // }
            if (c.render && typeof c.render === 'string') {
                return {
                    ...columnDef,
                    render: (value, row, index, params) => {
                        if (value === 0 || value === false) { value = value + "" }
                        let renderCell = (<JSXRender
                            {...{ ...extParams, value, row, index, params, openAction: openAction, handleParamsChanged: handleParamsChanged }}
                            jsx={util.renderTemplateString(c.render, { ...extParams, value, row, index, params })}
                        />);
                        if (c.tooltip) {
                            return (
                                <Tooltip title={<JSXRender jsx={util.renderTemplateString(c.tooltip, { row: row })} />}>
                                    <span>{renderCell}</span>
                                </Tooltip>
                            )
                        }
                        return renderCell;
                    }
                }
            }
            //antd4 dataIndex由"."变数组
            if (c.dataIndex && c.dataIndex.includes(".")) {
                c.dataIndex = c.dataIndex.split(".");
            }
            return columnDef;
        });
        const paginationProps = {
            showSizeChanger: true,
            showQuickJumper: true,
            pageSize: pageSize,
            pageSizeOptions: pageSizeOptions,
            showTotal: () => `共${total}条`,
            current: page,
            total: total,
            onChange: this.pageChange
        };
        return (
            <div style={style || {}} className={className ? className : "globalBackground"}>
                <Table
                    rowKey="__row_uuid"
                    locale={{
                        emptyText:emptyText ? <JSXRender  {...this.props} jsx={emptyText} /> : null
                      }}
                    bordered={bordered}
                    loading={loading}
                    columns={tableColumns}
                    size={size||"small"}
                    scroll={{ x: scrollX || 'max-content', y: scrollY }}
                    pagination={paginationProps}
                    dataSource={Array.isArray(data) ? data : []}
                    {...otherTableProps}
                />
            </div>
        );
    }
}

export default PagedTable