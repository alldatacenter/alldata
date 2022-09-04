/**
 * @author caoshuaibiao
 * @date 2021/7/7 17:05
 * @Description:滑条输入
 */
import React from 'react';
import { Slider } from 'antd';
import { ClearOutlined, PlusOutlined, DeleteOutlined } from "@ant-design/icons";

class SliderItem extends React.Component {

    constructor(props) {
        super(props);
        let { model } = props;
        let { defModel = {} } = model;
        this.defModel = defModel
        this.state = {
            value: props.value || defModel.defaultValue
        }
        this.valueMapping = model.valueMapping || defModel.valueMapping
        this.enableAdd = model.enableAdd || defModel.enableAdd;

    }

    componentDidMount() {
        if (this.enableAdd) {
            this.handleAfterChange(this.state.value);
        }
    }

    handleChange = (value) => {
        let { onChange } = this.props;
        this.setState({
            value: value
        });
        if (!this.enableAdd) {
            onChange && onChange(value)
        }
    }

    handleDelete = (item) => {
        let { value } = this.state;
        let newValues = value.filter(r => r != item);
        this.handleAfterChange(newValues);
    }

    handleAfterChange = (value) => {
        let rang = [], { onChange } = this.props;
        if (Array.isArray(value) && this.enableAdd) {
            if (!value.includes(0)) {
                value.unshift(0)
            }
            if (!value.includes(24)) {
                value.push(24)
            }
        }
        //去除重复点
        value.forEach(r => {
            if (!rang.includes(r)) {
                rang.push(r);
            }
        })
        //除了首尾点,其他点增加删除操作
        let marks = {};
        if (rang.length > 2) {
            for (let i = 1; i < rang.length - 1; i++) {
                marks[rang[i]] = {
                    label: <a style={{ color: '#f50' }} onClick={() => this.handleDelete(rang[i])}><DeleteOutlined /></a>,
                }
            }
        }
        this.setState({
            value: rang,
            marks: marks
        });
        onChange && onChange(rang);
    };


    render() {
        let { model } = this.props, { value, marks } = this.state;
        let { defModel = {} } = model;
        return (
            <div style={{ display: "flex", alignItems: "center" }}>
                <div style={{ flex: 1, marginTop: 5 }}>
                    <Slider marks={marks} {...defModel} {...model} value={value} onChange={this.handleChange} onAfterChange={this.enableAdd ? this.handleAfterChange : undefined} />
                </div>
            </div>
        )
    }
}

export default SliderItem;