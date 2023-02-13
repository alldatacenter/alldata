import React, { Component } from 'react';
import { Space, Form, Input, Button } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { debounce } from 'lodash'

class index extends Component {
    constructor(props) {
        super(props)
        let { getFieldValue } = props.form;
        let targetName = this.props.model.name;
        this.state = {
            targetFields: getFieldValue(targetName) || [
                {
                    first: '',
                    last: ''
                }
            ]
        }
    }
    add = () => {
        let { targetFields } = this.state;
        targetFields.push({
            first: '',
            last: ''
        })
        this.setState({ targetFields }, () => {
            this.submit()
        })
    }
    submit = () => {
        const { setFieldsValue } = this.props.form;
        let { targetFields } = this.state;
        let targetName = this.props.model.name;
        setFieldsValue && setFieldsValue({ [targetName]: targetFields })
    }
    remove = (str) => {
        let { targetFields } = this.state, arr = [];
        targetFields.forEach((item, i) => {
            if (`${i}-${item['first']}-${item['last']}` !== str) {
                arr.push(item)
            }
        })
        this.setState({ targetFields: arr }, () => {
            // this.submit()
        })
    }
    debounceChange = (index, name, event) => {
        debounce(this.onChange(index, name, event), 600)
    }
    onChange = (index, name, event) => {
        let { targetFields } = this.state;
        targetFields[index][name] = event.target.value;
        this.setState({ targetFields }, () => {
            this.submit()
        })
    }
    render() {
        let { targetFields } = this.state;
        return (
            <div>
                {targetFields.map((item, index) => (
                    <Space key={item} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                        <Input value={item['first']} onChange={(event) => this.debounceChange(index, 'first', event)} placeholder="First value" />
                        <Input value={item['last']} onChange={(event) => this.debounceChange(index, 'last', event)} placeholder="Last value" />
                        <MinusCircleOutlined onClick={() => this.remove(`${index}-${item['first']}-${item['last']}`)} />
                    </Space>
                ))}
                <Button type="dashed" onClick={() => this.add()} icon={<PlusOutlined />}>add</Button>
            </div>
        );
    }
}

export default index;