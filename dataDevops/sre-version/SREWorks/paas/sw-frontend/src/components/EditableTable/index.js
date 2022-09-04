/**
 * Created by caoshuaibiao on 2020/5/19.
 * 可编辑表格
 */
import React from 'react';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Table } from 'antd';
import uuidv4 from 'uuid/v4';
import localeHelper from '../../utils/localeHelper';
import FormElementFactory from '../FormBuilder/FormElementFactory'
import './index.less';
import FormElementType from "../FormBuilder/FormElementType";

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
        let { columns = [], dataSource, form } = props;
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
        let rowData = dataSource && dataSource.map(row => {
            return {
                __edit_key__: uuidv4(),
                ...row
            }
        });
        this.state = {
            editingRow: false,
            editedData: [],
            rowData: rowData
        };
    }

    handleEdit = (record) => {
        let { editStatusChanged } = this.props;
        this.setState({
            editingRow: record
        });
        editStatusChanged && editStatusChanged(false);
        this.handleChanged(this.state.rowData);
    };

    handleRemove = (record) => {
        let newData = this.state.rowData.filter(r => r.__edit_key__ !== record.__edit_key__);
        this.setState({
            rowData: newData,
        });
        this.handleChanged(newData);
    };

    handleCancel = () => {
        let { editStatusChanged } = this.props, { rowData } = this.state;
        this.setState({
            editingRow: false
        });
        editStatusChanged && editStatusChanged(true);
        this.handleChanged(rowData);
    };

    handleSave = (record) => {
        let { form, editStatusChanged } = this.props, { rowData } = this.state;
        let editRowValues = form.getFieldsValue();
        rowData.forEach(r => {
            if (r.__edit_key__ === record.__edit_key__) {
                Object.assign(r, editRowValues[r.__edit_key__])
            }
        });
        let newData = [...rowData];
        this.setState({
            editingRow: false,
            rowData: newData
        });
        editStatusChanged && editStatusChanged(true);
        this.handleChanged(newData);
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
    render() {
        let { form, enableRemove, enableAdd, enableScroll, isCanRemove, ...tableProps } = this.props, { editingRow, rowData } = this.state;
        let tableColumns = this.columns.map(column => {
            if (column.editProps) {
                return {
                    ...column,
                    render: (text, record, index) => {
                        // 兼容布尔类型
                        let values;
                        if (typeof (text) === 'boolean') {
                            values = text
                        } else {
                            values = text || column.editProps.initValue;
                        }
                        if (editingRow && record.__edit_key__ === editingRow.__edit_key__) {
                            return FormElementFactory.createFormItem(Object.assign({}, column.editProps,
                                { name: `${record.__edit_key__}.${column.editProps.name}`, initValue: values }),
                                form, formItemLayout)
                        }
                        //增加选择类型下的列值展示
                        let { optionValues } = column.editProps;
                        if (optionValues) {
                            let labels = [];
                            let tmpV = Array.isArray(values) ? values : [values]
                            optionValues.forEach(option => {
                                if (tmpV.includes(option.value)) {
                                    labels.push(option.label)
                                }
                            });
                            return labels.join(",")
                        }
                        //单选模式显示状态下赋值
                        if (column.editProps.type === FormElementType.SELECT) {
                            if (record[column.editProps.name + "Option"]
                                && record[column.editProps.name + "Option"].label) {
                                text = record[column.editProps.name + "Option"].label;
                            } else if (column.editProps.defaultLabelIndex) {
                                text = record[column.editProps.defaultLabelIndex];
                            }
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
                if (editingRow.__edit_key__ === record.__edit_key__) {
                    return (
                        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                            <a onClick={() => this.handleSave(record)} style={{ marginRight: 8 }}>{localeHelper.get('manage.taskplatform.common.save', "保存")}</a>
                            <a onClick={() => this.handleCancel(record)}>{localeHelper.get('ButtonCancel', "取消")}</a>
                        </div>
                    )
                }
                return (
                    <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                        <a disabled={editingRow} onClick={() => this.handleEdit(record)} style={{ marginRight: 8 }}><EditOutlined /></a>
                        {enableRemove && !record[isCanRemove] && <a onClick={() => this.handleRemove(record)} style={{ color: 'red' }}><DeleteOutlined /></a>}
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


export default Form.create()(EditableTable);