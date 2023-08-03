/* eslint-disable camelcase */
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import {
    Row, Col, Form, Input, FormInstance,
} from 'antd';
import { CustomSelect, useMount } from '../../../common';
import Title from '../Title';
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
    const [operatorList, setOperatorList] = useState([]);
    const [resultFormula, setResultFormula] = useState([]);
    const requiredRule = useRequiredRule();
    useMount(async () => {
        try {
            const $resultFormula = await $http.get('metric/resultFormula/list');
            const $operatorList = await $http.get('metric/operator/list');
            setOperatorList($operatorList || []);
            setResultFormula($resultFormula || []);
            if (detail && detail.id) {
                const {
                    resultFormula, operator, threshold,
                } = detail?.parameterItem || {} as TParameterItem;
                const options: Record<string, any> = {
                    resultFormula,
                    operator,
                    threshold,
                };
                form.setFieldsValue(options);
            }
        } catch (error) {
        }
    });
    const layoutItem = {
        style: {
            marginBottom: 12,
        },
        labelCol: {
            span: 8,
        },
        wrapperCol: {
            span: 16,
        },
    };
    return (
        <Title title={intl.formatMessage({ id: 'dv_metric_title_verify_configure' })}>
            <Row gutter={30}>
                {/* <Col span={6} /> */}
                <Col span={6} offset={2}>
                    <Form.Item
                        {...layoutItem}
                        label={intl.formatMessage({ id: 'dv_metric_verify_formula' })}
                        name="resultFormula"
                        rules={[...requiredRule]}
                    >
                        <CustomSelect
                            allowClear
                            source={resultFormula}
                            sourceValueMap="key"
                        />
                    </Form.Item>
                </Col>
                <Col span={6}>
                    <Form.Item
                        {...layoutItem}
                        label={intl.formatMessage({ id: 'dv_metric_verify_compare' })}
                        name="operator"
                        rules={[...requiredRule]}
                    >
                        <CustomSelect
                            allowClear
                            source={operatorList}
                            sourceValueMap="key"
                        />
                    </Form.Item>
                </Col>
                <Col span={6}>
                    <Form.Item
                        {...layoutItem}
                        rules={[...requiredRule]}
                        label={intl.formatMessage({ id: 'dv_metric_verify_threshold' })}
                        name="threshold"
                    >
                        <Input autoComplete="off" />
                    </Form.Item>
                </Col>
            </Row>
        </Title>
    );
};

export default Index;
