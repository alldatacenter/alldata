import React, { Component } from 'react';
import { Input } from 'antd';
import '@ant-design/compatible/assets/index.css';
import localeHelper from '../../../utils/localeHelper';
import * as util from "../../../utils/utils";

class EnhancedInput extends Component {
    constructor(props) {
        super(props)
        this.state = {
            value: this.props.value
        }
    }
    handleChange = (value) => {
        onChange && onChange(value);
    }
    render() {
        const { defModel, item } = this.props;
        let transDefmodel = JSON.parse(JSON.stringify(defModel));
        const { value } = this.state;
        if ((defModel.addonBefore && defModel.addonBefore.indexOf('$(') !== -1) || (defModel.addonAfter && defModel.addonAfter.indexOf('$(') !== -1)) {
            transDefmodel = util.renderTemplateJsonObject(defModel, this.props.nodeParams);
        }
        return (
            <Input
                addonBefore={transDefmodel.addonBefore || "http:"}
                onChange={this.handleChange}
                value={value}
                addonAfter={transDefmodel.addonAfter || ".com"}
                placeholder={(transDefmodel.inputTip || transDefmodel.tooltip || transDefmodel.inputTip) ? (transDefmodel.inputTip || item.tooltip || item.inputTip) : (localeHelper.get('common.placeInput', "请输入") + item.label)} autoComplete="off" />
        )
    }
}

export default EnhancedInput;