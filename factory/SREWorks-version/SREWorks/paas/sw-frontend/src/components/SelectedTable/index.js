import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Table, Radio, Checkbox } from 'antd';


class SelectedTable extends Component {
    constructor(props) {
        super(props);
        let multiple = props.multiple || false;
        this.columns = props.columns || [
            {
                title: 'Name',
                dataIndex: 'name',
            },
            {
                title: 'Age',
                dataIndex: 'age'
            }
        ]
        this.state = {
            dataSource: props.dataSource || [
                {
                    name: 'wangw',
                    age: '18',
                    checked: false
                },
                {
                    name: 'songy',
                    age: '19',
                    checked: false
                }
            ]
        }
        this.columns.unshift({
            dataIndex: 'checked',
            render: (flag, record, index) => {
                if (multiple) {
                    return <Checkbox onClick={() => this.changeSelect(index, 'checkbox')} checked={flag}></Checkbox>
                } else {
                    return <Radio onClick={() => this.changeSelect(index, 'radio')} checked={flag}></Radio>
                }
            }
        })
    }
    changeSelect = (index, type = 'radio') => {
        const { handleChange } = this.props;
        let dataSource = JSON.parse(JSON.stringify(this.state.dataSource))
        type === 'radio' && dataSource.forEach((item, i) => {
            if (i === index) {
                item.checked = true
            } else {
                item.checked = false
            }
        })
        type === 'checkbox' && dataSource.forEach((item, i) => {
            if (i === index) {
                item.checked = !item.checked
            }
        })
        handleChange && handleChange(dataSource.filter(item => item.checked))
        this.setState({ dataSource })
    }
    render() {
        const { dataSource } = this.state;
        return (
            <Table pagination={false} columns={this.columns} dataSource={dataSource}></Table>
        );
    }
}

export default Form.create()(SelectedTable);