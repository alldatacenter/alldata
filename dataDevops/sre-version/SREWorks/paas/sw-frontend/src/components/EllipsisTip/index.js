import React from 'react';
import { Tooltip } from 'antd';

function EllipsisTip(props) {
    const style = {
        // display: 'inline-block', 
        maxWidth: props.max || '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    };
    let title = props.title;
    if (!title && typeof props.children === 'string') {
        title = props.children;
    }

    return (
        <Tooltip title={title}>
            <div style={{ ...props.style, ...style }}>
                {props.children}
            </div>
        </Tooltip>
    );
}

export default EllipsisTip;