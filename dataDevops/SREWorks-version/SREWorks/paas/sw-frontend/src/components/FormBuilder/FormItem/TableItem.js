/**
 * Created by caoshuaibiao on 2019/3/28.
 * table形式展示的表单元素
 */

import React from 'react';
import { Spin, Button, Card, Modal, Tooltip, List, Row, Col, Divider, message } from 'antd';
import PagingTable from '../../PagingTable';
import OamWidget from '../../../core/framework/OamWidget';
import EditableTable from '../../EditableTable';
import SelectedTable from '../../SelectedTable';
import EditTableCut from '../../EditTableCut';

export default class TableItem extends React.Component {

    constructor(props) {
        super(props);
        let { model, onChange } = props;
        this.config = model.defModel || {};
        this.data = model.initValue || [];
        //存在外界传入非数组的数据,转换为数组防止数据格式有问题
        if (!Array.isArray(this.data)) {
            this.data = [this.data]
        }
        this.state = {
            data: this.data
        }
        this.handleChange(this.data);
    }

    handleChange = (data) => {
        let postData = [], postColumns = this.config.postColumns, { onChange } = this.props;
        if (postColumns && postColumns.length > 0) {
            data.forEach(item => {
                let pd = {};
                postColumns.forEach(pc => {
                    pd[pc] = item[pc];
                });
                postData.push(pd);
            })
        } else {
            postData = data;
        }
        onChange && onChange(postData);
    };

    statusChanged = (status) => {
        let { model } = this.props;
        model.__saved__ = status;
    };
    handleChangeSelected = (data) => {
        const { onChange } = this.props;
        onChange && onChange(data);
    }
    componentWillReceiveProps(nextProps) {
        this.setState({
            data: nextProps.value
        })
    }
    render() {
        let { model, onChange } = this.props, { enableEdit, enableEditTableCut, enableAdd, enableRemove, enableSelected, columns, multiple, ...otherTableProps } = this.config;
        let { formInitParams = {} } = model.extensionProps || {};
        let { data } = this.state;
        //启用了可编辑表格
        if (enableEdit) {
            return <EditableTable {...otherTableProps} columns={columns} enableAdd={enableAdd}
                enableRemove={enableRemove} onChange={this.handleChange}
                dataSource={data} editStatusChanged={this.statusChanged} />
        }
        if (enableSelected) {
            return <SelectedTable multiple={multiple} columns={columns} dataSource={data} handleChange={this.handleChangeSelected} />
        }
        if (this.config && this.config.tableType === 'AG_TABLE') {
            let { formInitParams = {} } = model.extensionProps || {};
            return (
                <div>
                    <OamWidget {...formInitParams} widget={this.config} data={data} />
                </div>
            )
        }
        return (
            <div>
                <PagingTable columns={this.config.columns} data={data} {...this.props} />
            </div>
        )
    }
}