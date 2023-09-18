import React, { useState } from 'react';
import { Tabs } from 'antd';
import { useIntl } from 'react-intl';
import SLAsList from './SLAsList';
import NoticeList from './NoticeList';
import { Title } from '@/component';

const { TabPane } = Tabs;

export default () => {
    const [activeKey, setActiveKey] = useState('SLAS');
    const intl = useIntl();
    return (
        <div style={{ paddingTop: 20 }}>
            <Title>
                {intl.formatMessage({ id: 'sla_title' })}
            </Title>
            <Tabs activeKey={activeKey} onChange={(key) => (setActiveKey(key))}>
                <TabPane tab="SLA" key="SLAS">
                    <SLAsList />
                </TabPane>
                <TabPane tab={intl.formatMessage({ id: 'common_notice' })} key="Notice">
                    <NoticeList />
                </TabPane>
            </Tabs>
        </div>
    );
};
