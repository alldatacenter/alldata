import React, { useRef, useState } from 'react';
import { Form, Button } from 'antd';
import { useIntl } from 'react-intl';
import querystring from 'querystring';
import { CreateSLAsComponent } from '../SLAsNoticeList/hooks/CreateSLAs';
import { useMount } from '@/common';
import { getSLAById } from '@/action/sla';

export default () => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const [qs] = useState(querystring.parse(window.location.href.split('?')[1] || ''));
    const innerRef = useRef<any>();
    const [detail, setDetail] = useState({});
    useMount(async () => {
        const res = await getSLAById(qs.slaId);
        setDetail(res);
        form.setFieldsValue({
            name: res.name,
            description: res.description,
        });
    });
    const update = () => {
        innerRef.current.saveUpdate();
    };
    return (
        <div style={{ width: 500 }}>
            <CreateSLAsComponent form={form} detail={detail} innerRef={innerRef} />

            <div style={{ textAlign: 'center', paddingTop: 30 }}>
                <Button type="primary" onClick={update}>{intl.formatMessage({ id: 'common_update' })}</Button>
            </div>
        </div>
    );
};
