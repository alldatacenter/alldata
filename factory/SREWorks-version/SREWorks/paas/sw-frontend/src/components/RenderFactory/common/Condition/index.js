/**
 * @author caoshuaibiao
 * @date 2021/6/4 14:58
 * @Description:条件显示包装器Render,用于控制其中的子元素是否显示,expr返回true是显示,false不显示
 */

import React from 'react';
function Condition(props) {
    const expr = props.expr || '';
    if (expr && eval(expr)) {
        return <span>{props.children}</span>;
    }
    return null;
}
export default Condition;