/* eslint-disable camelcase */
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import {
    Row, Col, Form, FormInstance,
} from 'antd';
import { CustomSelect, useMount } from '../../../common';
import Title from '../Title';
import { layoutItem } from '../helper';
import useRequest from '../../../hooks/useRequest';
import { TDetail } from '../type';
import { useEditorContextState } from '../../../store/editor';

type InnerProps = {
    form: FormInstance,
    detail: TDetail,
}

const Index = ({ form, detail }: InnerProps) => {
    const intl = useIntl();
    const { $http } = useRequest();
    const [context] = useEditorContextState();
    const [errorList, setErrorList] = useState([]);
    useMount(async () => {
        try {
            const res = await $http.get(`/errorDataStorage/list/${context.workspaceId}`);
            setErrorList(res || []);
            if (detail && detail.id) {
                form.setFieldsValue({
                    errorDataStorageId: detail?.errorDataStorageId || undefined,
                });
            }
        } catch (error) {
        }
    });

    return (
        <Title title={intl.formatMessage({ id: 'dv_metric_error_store_config' })}>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={intl.formatMessage({ id: 'dv_metric_error_store_engine' })}
                        name="errorDataStorageId"
                    >
                        <CustomSelect allowClear source={errorList} sourceValueMap="id" sourceLabelMap="name" />
                    </Form.Item>
                </Col>
            </Row>
        </Title>
    );
};

export default Index;
