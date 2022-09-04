
import React from 'react';
import { Button } from 'antd';
import _ from 'lodash';

function ActionWrapper(props) {
    const callback = props.callback || (() => { });
    const action = props.action || '';
    const data = props.data || {};
    const extData = props.extData || {}
    const hiddenExp = props.hiddenExp || '';
    //隐藏函数
    if (hiddenExp) {
        let row = data;
        if (eval(hiddenExp)) {
            return null;
        }
    }
    return (
        <span onClick={() => callback(action, _.assign({}, data, extData))}>{props.children}</span>
    );
}

export default ActionWrapper;