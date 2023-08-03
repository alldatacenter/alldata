import React from 'react';
import { useIntl } from 'react-intl';
import { Popconfirm } from 'antd';

export default ({ onClick, style }: {onClick?: () => void, style?: React.CSSProperties}) => {
    const intl = useIntl();

    return (
        <Popconfirm
            title={intl.formatMessage({ id: 'common_delete_tip' })}
            onConfirm={() => { onClick?.(); }}
            okText={intl.formatMessage({ id: 'common_Ok' })}
            cancelText={intl.formatMessage({ id: 'common_Cancel' })}
        >
            {/* color: '#f81d22', */}
            <a style={{ marginLeft: 10, ...(style || {}) }}>{intl.formatMessage({ id: 'common_delete' })}</a>
        </Popconfirm>
    );
};
