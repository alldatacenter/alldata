/**
 * Created by caoshuaibiao on 2019/1/3.
 * 分页table 分页异步加载table组件,后台分页非前端分页
 */

import React, { Component } from 'react';
import { FilterOutlined, SearchOutlined } from '@ant-design/icons';
import { Table, Input, Button, Tooltip, Tag } from 'antd';
import localeHelper from '../../utils/localeHelper';
import * as util from '../../utils/utils';
import httpClient from '../../utils/httpClient';
import Highlighter from 'react-highlight-words';
import JSXRender from "../../components/JSXRender";
import _ from 'lodash';
import debounce from 'lodash.debounce';
import uuidv4 from 'uuid/v4';

import './index.less'

class PagingTable extends Component {

    constructor(props) {
        super(props);
        this.lastFetchId = 0;
        this.state = {
            params: Object.assign({}, this.props.params || {}),
            data: this.props.data || [],
            pagination: Object.assign({
                size: 'small',
                current: 1,
                showSizeChanger: true,
                showQuickJumper: true,
                defaultCurrent: 1,
                hideOnSinglePage: false,
                total: 0,
                pageSizeOptions: ['10', '25', '50', '100'],
                showTotal: total => {
                    let extFooter = (
                        <div>
                            <div style={{ position: 'absolute', left: 0 }}>
                                {
                                    props.extFooter ? props.extFooter : null
                                }
                            </div>
                            <div>
                                {localeHelper.get("manage.trip", '共 {count} 条', { count: total })}
                            </div>
                        </div>
                    );
                    return extFooter;
                }
            }, this.props.customPagination || {}),
            loading: false,
        };
        this.loadData = debounce(this.loadData, 100);
    }

    componentWillMount() {
        let { params = {} } = this.props, { pagination } = this.state;
        this.loadData(Object.assign({}, params, { page: params.page ? params.page : 1, pageSize: params.pageSize ? params.pageSize : pagination.pageSize || 10 }));
    }

    componentDidUpdate(prevProps) {
        let { params = {}, data, dataUrl } = this.props, { pagination } = this.state;
        if (!_.isEqual(prevProps.params || {}, params) || prevProps.dataUrl !== dataUrl || !_.isEqual(prevProps.data, data)) {
            this.loadData(Object.assign({}, params, { page: params.page ? params.page : 1, pageSize: params.pageSize ? params.pageSize : pagination.pageSize || 10 }));
        }
    }

    genRowKey = (data = []) => {
        data.length && data.forEach(d => {
            if (!d.__row_uuid) {
                d.__row_uuid = uuidv4();
            }
        })
    };

    loadData = (params, tablePagination) => {
        let { dataUrl, data, dataMethod, dataIndex, onDataLoaded } = this.props, { pagination } = this.state;
        if (data) {
            this.genRowKey(data);
            pagination.current = params.page ? params.page : 1;
            this.setState({ pagination: tablePagination ? tablePagination : { ...pagination }, data: data });
            return;
        }
        this.setState({ loading: true });
        let dataPromise = null;
        if (dataMethod === 'POST') {
            dataPromise = httpClient.post(dataUrl, params);
        } else {
            dataPromise = httpClient.get(dataUrl, { params: params });
        }
        if (dataPromise) {
            dataPromise.then(results => {
                pagination.total = results.total;
                pagination.current = params.page ? params.page : 1;
                let tdata = results.items || results;
                if (dataIndex) tdata = results[dataIndex] || [];
                this.genRowKey(tdata);
                this.setState({ data: tdata, loading: false, pagination: tablePagination ? Object.assign({}, tablePagination, { total: results.total }) : { ...pagination } });
                onDataLoaded && onDataLoaded(results);
            });
        }
    };

    getData = () => {
        return this.state.data;
    };

    getColumnSearch = (dataIndex, columnDef) => ({
        filterDropdown: ({
            setSelectedKeys, selectedKeys, confirm, clearFilters,
        }) => (
            <div className="custom-filter-dropdown">
                <Input
                    ref={node => { this.searchInput = node; }}
                    value={selectedKeys[0]}
                    onChange={e => setSelectedKeys(e.target.value ? [e.target.value] : [])}
                    onPressEnter={() => this.handleSearch(selectedKeys, confirm)}
                    style={{ width: 220, marginBottom: 8, display: 'block' }}
                />
                <Button
                    type="primary"
                    onClick={() => this.handleSearch(selectedKeys, confirm)}
                    icon={<SearchOutlined />}
                    size="small"
                    style={{ width: 90, marginRight: 8 }}
                >
                    {localeHelper.get("manage.ots.search", '搜索')}
                </Button>
                <Button
                    onClick={() => this.handleReset(clearFilters)}
                    size="small"
                    style={{ width: 90 }}
                >
                    {localeHelper.get("manage.taskplatform.common.reset", '重置')}
                </Button>
            </div>
        ),
        filterIcon: filtered => <FilterOutlined style={{ color: filtered ? '#1890ff' : undefined }} />,
        onFilter: (value, record) => {
            let columnData = _.get(record, dataIndex);
            if (columnData) {
                let columnText = columnData.value || columnData;
                if (columnText.toString) {
                    return columnText.toString().toLowerCase().includes(value.toLowerCase());
                }
                return columnText == value;
            }
            return false;
        },
        onFilterDropdownVisibleChange: (visible) => {
            if (visible) {
                setTimeout(() => this.searchInput.select());
            }
        },
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
            //console.log("text---->",text,"path---->",path);
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
            if ((columnDef.maxWidth || columnDef.width) && !columnDef.render) {
                hasTiped = true;
                let sto = {};
                if (columnDef.maxWidth) {
                    sto.maxWidth = columnDef.maxWidth;
                }
                if (columnDef.width) {
                    sto.width = isFinite(columnDef.width) ? parseInt(columnDef.width) : columnDef.width;
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

    handleSearch = (selectedKeys, confirm) => {
        confirm();
        this.setState({ searchText: selectedKeys[0] });
    };

    handleReset = (clearFilters) => {
        clearFilters();
        this.setState({ searchText: '' });
    };

    handleTableChange = (pagination, filters, sorter) => {
        let { current, pageSize } = pagination, filterParams = {}, sortParams = {}, params = Object.assign({}, this.props.params || {});
        let dyanParams = {
            page: current,
            pageSize: pageSize,
        };
        Object.keys(filters).forEach(key => {
            filterParams[key] = Array.isArray(filters[key]) ? filters[key].join(",") : filters[key];
        });
        if (sorter.field) {
            sortParams.orderField = sorter.field;
            sortParams.orderBy = sorter.order.startsWith("asc") ? 'asc' : 'desc';
        }
        dyanParams = Object.assign(dyanParams, params, { filters: filterParams }, sortParams);
        this.loadData(dyanParams, pagination);
        this.props.onChange && this.props.onChange(pagination, filters, sorter);
    };
    componentWillReceiveProps(nextProps) {
    }
    render() {
        const { pagination, data, loading } = this.state, { style, scrollX, scrollY, className, columns, renderComponents, openAction, handleParamsChanged, dataUrl, extParams = {},widgetConfig={}, ...otherTableProps } = this.props;
        let { emptyText='',bordered=false,size } = widgetConfig;
        const tableColumns = columns && columns.map(c => {
            let columnDef = Object.assign({ ...this.getColumnSearch(c.dataIndex, c) }, c);
            if (c.filters) {
                delete columnDef.filterDropdown
            }
            //对于后端分页/列设置不排序,前端不再进行排序计算
            if (dataUrl && columnDef.sorter != false && !this.props.data) {
                columnDef.sorter = true;
            }
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
            if (columnDef.dataIndex && columnDef.dataIndex.includes(".")) {
                columnDef.dataIndex = columnDef.dataIndex.split(".");
            }
            return columnDef;
        });
        return (
            <div style={style || {}} className={className ? className : "globalBackground"}>
                <Table
                    rowKey="__row_uuid"
                    locale={{
                        emptyText:emptyText ? <JSXRender  {...this.props} jsx={emptyText} /> : ''
                      }}
                    columns={tableColumns}
                    size={size||'small'}
                    bordered={bordered}
                    dataSource={Array.isArray(data) ? data : []}
                    pagination={pagination}
                    loading={loading}
                    scroll={{ x: scrollX, y: scrollY }}
                    onChange={this.handleTableChange}
                    {...otherTableProps}
                />
            </div>
        );
    }
}

export default PagingTable;