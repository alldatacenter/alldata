import React, { useState } from 'react';
import { Tabs } from 'antd';
import { useIntl } from 'react-intl';
import querystring from 'querystring';
import { GoBack } from '@/component';
import Notification from './Notification';

const { TabPane } = Tabs;

export default () => {
    const [activeKey, setActiveKey] = useState('notification');
    const [qs] = useState(querystring.parse(window.location.href.split('?')[1] || ''));
    const intl = useIntl();
    return (
        <div>
            <div className="dv-flex-between" style={{ marginBottom: 10, paddingTop: 10 }}>
                <span>
                    <GoBack />
                    <span style={{ fontWeight: 500, marginLeft: 20, fontSize: 16 }}>
                        {qs.slaName}
                        {' '}
                        {intl.formatMessage({ id: 'common_settings' })}
                    </span>
                </span>
                <span />
            </div>
            <div style={{ padding: '0 20px' }}>
                <Tabs activeKey={activeKey} type="card" onChange={(key) => (setActiveKey(key))}>
                    <TabPane tab={intl.formatMessage({ id: 'common_notice' })} key="notification">
                        <Notification />
                    </TabPane>
                </Tabs>
            </div>

        </div>
    );
};
