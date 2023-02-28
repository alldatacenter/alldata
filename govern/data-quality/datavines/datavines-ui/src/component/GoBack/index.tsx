import React, { memo } from 'react';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useHistory } from 'react-router-dom';

export default memo(({ style, children }: { style?: React.CSSProperties, children?: React.ReactNode}) => {
    const history = useHistory();
    const onClick = () => {
        history.goBack();
    };
    return (
        <a onClick={onClick} style={{ ...(style || {}) }}>
            {
                children || <ArrowLeftOutlined style={{ fontSize: 16, cursor: 'pointer', color: '#ffd56a' }} />
            }

        </a>
    );
});
