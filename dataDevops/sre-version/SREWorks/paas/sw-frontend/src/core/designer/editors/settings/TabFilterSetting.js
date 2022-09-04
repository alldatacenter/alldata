import React, { Component } from 'react';
import EditableTable from '../../../../components/EditableTable';

class TabFilterSetting extends Component {
    constructor(props) {
        super(props);
        const { dataSource, parameters } = props;
        this.state = {
            columns: [{
                title: 'tab标签',
                dataIndex: 'label',
                key: 'label',
                editProps: {
                    inputTip: "操作的名称",
                    label: "",
                    name: "label",
                    required: false,
                    type: 1
                }
            }, {
                title: 'tab标识',
                dataIndex: 'name',
                key: 'name',
                editProps: {
                    inputTip: "tab标识",
                    label: "",
                    name: "name",
                    required: false,
                    type: 1
                }
            }],
            dataSource: parameters || []
        }
    }
    changeSetting = (newData) => {
        const { onChange } = this.props;
        onChange && onChange(newData);
        this.setState({ dataSource: newData })
    }
    editStatusChanged = (flag) => {
        this.setState({})
    }
    render() {
        const { columns, dataSource = [] } = this.state;
        return (
            <EditableTable pagination={false} style={{ width: '50%' }} enableRemove={true} enableAdd={true} columns={columns} dataSource={dataSource} editStatusChanged={this.editStatusChanged} onChange={this.changeSetting} />
        );
    }
}

export default TabFilterSetting;
