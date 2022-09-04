/**
 * Created by caoshuaibiao on 2019/6/14.
 * OamWidget表单挂件,主要用于显示
 */

import React from 'react';
import OamWidget from '../../../core/framework/OamWidget';

export default class OamWidgetItem extends React.Component {

    constructor(props) {
        super(props);
        let { model, onChange } = this.props;
        this.config = model.defModel;
        this.data = model.initValue || [];
        //显示时把数据派发出去
        onChange && onChange(model.initValue);
    }

    render() {
        let { model } = this.props;
        if (this.config) {
            let extProps = model.extensionProps || {}, { formInitParams } = model.extensionProps;
            //去掉nodeParams中的内置
            return (
                <div>
                    <OamWidget {...extProps} {...formInitParams} widget={this.config} />
                </div>
            )
        }
        return (
            <div key="_undefined_formItem">{localeHelper.get('common.undefindFromEle', '未定义表单元素')}</div>
        )
    }
}