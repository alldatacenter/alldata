/*
 * @Author: william.xw
 * @Date: 2019-09-29 10:25:30
 * @LastEditors: william.xw
 * @LastEditTime: 2019-10-15 10:39:14
 * @Description: add B 
 */
import * as React from 'react';
import { Tooltip } from 'antd';

export default class NumberTooltip extends React.Component {
    constructor(props) {
        super(props);
        this.data = [];
        this.state = {
        }
    }

    render() {
        return (
            <Tooltip title={this.props.value}>
                {this.props.type === 'cap' && <span>{printCapSize(this.props.value)}</span>}
                {this.props.type === 'split' && <span>{printSplitSize(this.props.value)}</span>}
                {this.props.type === 'percentage' && <span style={{ color: this.props.notNeedColor ? '' : this.color(this.props.value, this.props.size) }}>{printPercentSize(this.props.value, this.props.digit)}</span>}
            </Tooltip>
        );
    }

    color(value, size) {
        if (!size) { size = 0.8 }
        if (value > size) {
            return 'red'
        } else {
            return
        }
    }
}
function printCapSize(value, decimal_point_precision) {//value转化为容量单位大小，decimal_point_precision为精确到几位小数
    var _1P = 1024 * 1024 * 1024 * 1024 * 1024;
    var _1T = 1024 * 1024 * 1024 * 1024;
    var _1G = 1024 * 1024 * 1024;
    var _1M = 1024 * 1024;
    var _1K = 1024;
    var type = 0;
    if (value < 0) {
        type = -1;
        value = value * (-1);
    }
    if (null == decimal_point_precision || decimal_point_precision.constructor != Number) {
        decimal_point_precision = 2;
    }
    var point = 1;
    for (var i = 0; i < decimal_point_precision; i++) {
        point = point * 10;
    }
    var str = '';
    if (value >= _1P) {
        str = parseInt(value / _1P * point) / point + ' P';
    }
    else if (value >= _1T) {
        str = parseInt(value / _1T * point) / point + ' T';
    }
    else if (value >= _1G) {
        str = parseInt(value / _1G * point) / point + ' G';
    }
    else if (value >= _1M) {
        str = parseInt(value / _1M * point) / point + ' M';
    }
    else if (value >= _1K) {
        str = parseInt(value / _1K * point) / point + ' K';
    }
    else {
        str = parseInt(value * point) / point + ' B';
    }
    if (type < 0) {
        str = '- ' + str;
    }
    return str;
}
function printSplitSize(value) {//value转化为三位分隔字符串
    if (!value && value !== 0) return
    return value.toLocaleString()
}
function printPercentSize(value, digit) {//value百分比转化
    return (value * 100).toFixed(digit ? digit : 2) + '%'
}
