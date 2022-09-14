/**
 * Created by caoshuaibiao on 2020/5/19.
 * 可编辑表格
 */
import React from 'react';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Table, Input } from 'antd';
import uuidv4 from 'uuid/v4';
import localeHelper from '../../utils/localeHelper';
import FormElementFactory from '../FormBuilder/FormElementFactory'
import './index.less';
import FormElementType from "../FormBuilder/FormElementType";
import _ from 'lodash';
import { tempdir } from 'shelljs';

const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 6 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 16 },
    },
};
class EditableTable extends React.Component {

    constructor(props) {
        super(props);
        let { model = {} } = props;
        let { defModel } = model;
        let { columns } = defModel
        let { dataSource } = model.initValue
        this.columns = columns.map(column => {
            column = JSON.parse(JSON.stringify(column));
            //根据定义的可编辑属性生成表单构建器属性
            let { editProps, dataIndex } = column;
            if (editProps) {
                editProps.name = editProps.name || dataIndex;
                editProps.label = ''
            }
            return column;
        });
        //对每条数据生成一个唯一的编辑key
        let rowData = [];
        if (dataSource) {
            rowData = dataSource && dataSource.map(row => {
                return {
                    __edit_key__: uuidv4(),
                    ...row
                }
            });
        }
        this.state = {
            editingRow: false,
            editedData: {},
            rowData: rowData,
            editStatus: false
        };
    }

    handleEdit = (record) => {
        let { editStatusChanged } = this.props;
        this.setState({
            editingRow: record,
            editStatus: true,
        });
        //  editStatusChanged&&editStatusChanged(false);
        //  this.handleChanged(this.state.rowData);
    };

    handleRemove = (record) => {
        this.setState({ editStatus: true })
        let { onChange } = this.props;
        let newData = this.state.rowData.filter(r => r.__edit_key__ !== record.__edit_key__);
        this.setState({
            rowData: newData,
        });
        onChange && onChange(newData)
        this.setState({ editStatus: false })
    };

    handleCancel = () => {
        let { rowData } = this.state;
        this.setState({
            editingRow: false,
            editStatus: false,
            rowData: rowData,
            editedData: {}
        });
    };

    handleSave = () => {
        this.setState({ editingRow: false, })
        let { onChange } = this.props;
        let { rowData, editedData } = this.state;
        rowData.forEach(item => {
            if (editedData.__edit_key__ === item.__edit_key__) {
                item = Object.assign(item, editedData)
            }
        })
        this.setState({
            rowData: rowData,
            editedData: {}
        })
        this.setState({ editStatus: false })
        onChange && onChange(rowData);
    };
    handleAdd = () => {
        let newRow = { __edit_key__: uuidv4() }, { onChange, editStatusChanged } = this.props;
        //增加新增行存在初始值的显示
        this.columns.forEach(column => {
            if (column.editProps && column.editProps.hasOwnProperty("initValue")) {
                newRow[column.editProps.name] = column.editProps.initValue;
            }
        });
        this.setState({
            rowData: [...this.state.rowData, newRow],
            editingRow: newRow
        });
        editStatusChanged && editStatusChanged(false);
        this.handleChanged([...this.state.rowData, newRow]);
    };

    handleChanged = (rowData) => {
        let { onChange } = this.props;
        let newData = rowData.map(r => {
            let { __edit_key__, ...otherData } = r;
            return otherData;
        });
        onChange && onChange(newData);
    };
    submitInputValue = (e, editKey) => {
        let { rowData } = this.state;
        let obj = {};
        rowData.forEach(item => {
            if (item.__edit_key__ === editKey) {
                item[e.target.name] = e.target.value
                obj = { ...item }
            }
        })
        this.setState({ editedData: obj })
    }
    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(nextProps.value, this.state.rowData && !this.state.editStatus)) {
            let arr = nextProps.value.forEach(item => {
                item.__edit_key__ = uuidv4()
            })
            this.setState({
                rowData: nextProps.value,
                editingRow: false,
            })
        }
    }
    render() {
        let { form, enableAdd, enableScroll, isCanRemove, ...tableProps } = this.props, { editingRow, rowData } = this.state;
        let { defModel } = this.props;
        let { enableRemove } = defModel;
        let tableColumns = this.columns.map(column => {
            if (column.editProps) {
                return {
                    ...column,
                    render: (text, record, index) => {
                        // 兼容布尔类型
                        let values;
                        let name = '';
                        for (var key in record) {
                            if (record[key] === text) {
                                name = key
                            }
                        }
                        if (typeof (text) === 'boolean') {
                            values = text
                        } else {
                            values = text || column.editProps.initValue;
                        }
                        if (editingRow && record.__edit_key__ === editingRow.__edit_key__) {
                            return <Input defaultValue={values} name={name} placeholder="请输入编辑值" onChange={(e) => this.submitInputValue(e, record.__edit_key__)} />
                        }
                        return text;
                    }
                }
            }
            return column;
        });
        //动态添加操作列
        tableColumns.push({
            title: localeHelper.get('operation', '操作'),
            key: '_oper',
            width: 80,
            fixed: enableScroll ? "right" : null,
            render: (text, record) => {
                if (editingRow && editingRow.__edit_key__ === record.__edit_key__) {
                    return (
                        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                            <a onClick={this.handleSave} style={{ marginRight: 8 }}>{localeHelper.get('manage.taskplatform.common.save', "保存")}</a>
                            <a onClick={() => this.handleCancel(record)}>{localeHelper.get('ButtonCancel', "取消")}</a>
                        </div>
                    )
                }
                return (
                    <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                        <a disabled={editingRow} onClick={() => this.handleEdit(record)} style={{ marginRight: 8 }}><EditOutlined /></a>
                        {enableRemove && <a onClick={() => this.handleRemove(record)} style={{ color: 'red' }}><DeleteOutlined /></a>}
                    </div>
                );
            }
        });
        let { title } = tableProps;
        return (
            <div className="editable_table">
                <Table
                    scroll={enableScroll ? { x: 1300 } : null}
                    {...tableProps}
                    rowKey="__edit_key__"
                    columns={tableColumns}
                    size="small"
                    dataSource={rowData}
                    title={!enableAdd && !title ? null : () => {
                        return (
                            <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                                <span>{title}</span>
                                <span><a onClick={() => this.handleAdd()}><PlusOutlined style={{ marginRight: 2 }} />{localeHelper.get('manage.taskplatform.common.new', "添加")}</a></span>
                            </div>
                        );
                    }}
                />
            </div>
        );
    }
}


export default EditableTable;