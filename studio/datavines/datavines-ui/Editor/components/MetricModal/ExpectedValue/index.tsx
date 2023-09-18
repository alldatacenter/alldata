/* eslint-disable camelcase */
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import {
    Row, Col, Form, Input, FormInstance,
} from 'antd';
import { CustomSelect, useMount } from '../../../common';
import Title from '../Title';
import { layoutItem } from '../helper';
import useRequest from '../../../hooks/useRequest';
import useRequiredRule from '../../../hooks/useRequiredRule';
import { TDetail, TParameterItem } from '../type';

type InnerProps = {
    form: FormInstance,
    detail: TDetail
}

const Index = ({ form, detail }: InnerProps) => {
    const intl = useIntl();
    const { $http } = useRequest();
    const [expectedTypeList, setExpectedTypeList] = useState([]);
    const requiredRules = useRequiredRule();
    useMount(async () => {
        try {
            const res = await $http.get('metric/expectedValue/list');
            setExpectedTypeList(res || []);
            if (detail && detail.id) {
                const {
                    expectedType, expectedParameter,
                } = detail?.parameterItem || {} as TParameterItem;
                const options: Record<string, any> = {
                    expectedType,
                    expected_value: expectedParameter?.expected_value,
                };
                form.setFieldsValue(options);
            }
        } catch (error) {
        }
    });

    return (
        <Title title={intl.formatMessage({ id: 'dv_metric_title_expected_value' })}>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        rules={requiredRules}
                        label={intl.formatMessage({ id: 'dv_metric_expected_value_type' })}
                        name="expectedType"
                    >
                        <CustomSelect allowClear source={expectedTypeList} sourceValueMap="key" />
                    </Form.Item>
                </Col>

                <Col span={12}>
                    <Form.Item noStyle dependencies={['expectedType']}>
                        {() => {
                            const value = form.getFieldValue('expectedType');
                            if (value !== 'fix_value') {
                                return null;
                            }
                            return (
                                <Form.Item
                                    {...layoutItem}
                                    rules={requiredRules}
                                    label={intl.formatMessage({ id: 'dv_metric_expected_value' })}
                                    name="expected_value"
                                >
                                    <Input autoComplete="off" allowClear />
                                </Form.Item>
                            );
                        }}
                    </Form.Item>

                </Col>
            </Row>
        </Title>
    );
};

export default Index;
