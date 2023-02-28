import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import {
    Row, Col, Form, Input, Radio, FormInstance,
} from 'antd';
import Title from '../Title';
import { CustomSelect, useMount } from '../../../common';
import {
    layoutItem, layoutActuatorItem, layoutActuatorLineItem,
} from '../helper';
import useRequest from '../../../hooks/useRequest';
import useRequiredRule from '../../../hooks/useRequiredRule';
import { TDetail, TEngineParameter } from '../type';
import './index.less';
import store, { RootReducer } from '@/store';

type InnerProps = {
    form: FormInstance,
    detail: TDetail
}

const Index = ({ form, detail }: InnerProps) => {
    const { $http } = useRequest();
    const intl = useIntl();
    const requiredRule = useRequiredRule();
    const [engineList, setEngineList] = useState([]);
    const { datasourceReducer } = store.getState() as RootReducer;
    // console.log('123123', detail);
    useMount(async () => {
        try {
            const $engineList = await $http.get('metric/engine/list');
            setEngineList($engineList || []);
            const paramter = detail?.engineParameter || {} as TEngineParameter;
            form.setFieldsValue({
                deployMode: paramter.deployMode ?? 'cluster',
                driverCores: paramter.driverCores ?? 1,
                driverMemory: paramter.driverMemory ?? '512M',
                numExecutors: paramter.numExecutors ?? 2,
                executorMemory: paramter.executorMemory ?? '2G',
                executorCores: paramter.executorCores ?? 2,
                others: paramter.others ?? '--conf spark.yarn.maxAppAttempts=1',
                tenantCode: detail?.tenantCode,
                env: detail?.env,
                engineType: detail?.engineType || (datasourceReducer.modeType === 'comparison' ? 'spark' : 'local'),
            });
        } catch (error) {
            console.log('error', error);
        }
    });
    const renderSpark = () => (
        <>
            <Form.Item
                dependencies={['actuatorType']}
                {...layoutActuatorLineItem}
                label={intl.formatMessage({ id: 'dv_metric_actuator_deploy_mode' })}
                name="deployMode"
                rules={[...requiredRule]}
                initialValue="cluster"
            >
                <Radio.Group>
                    <Radio value="cluster">cluster</Radio>
                    <Radio value="client">client</Radio>
                    <Radio value="local">local</Radio>
                </Radio.Group>
            </Form.Item>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutActuatorItem}
                        label={intl.formatMessage({ id: 'dv_metric_actuator_driver_cores' })}
                        name="driverCores"
                        rules={[...requiredRule]}
                    >
                        <Input allowClear />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutActuatorItem}
                        label={intl.formatMessage({ id: 'dv_metric_actuator_driver_memory' })}
                        name="driverMemory"
                        rules={[...requiredRule]}
                    >
                        <Input allowClear />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutActuatorItem}
                        label={intl.formatMessage({ id: 'dv_metric_actuator_executor_numbers' })}
                        name="numExecutors"
                        rules={[...requiredRule]}
                    >
                        <Input allowClear />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutActuatorItem}
                        label={intl.formatMessage({ id: 'dv_metric_actuator_executor_memory' })}
                        name="executorMemory"
                        rules={[...requiredRule]}
                    >
                        <Input allowClear />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutActuatorItem}
                        label={intl.formatMessage({ id: 'dv_metric_actuator_executor_cores' })}
                        name="executorCores"
                        rules={[...requiredRule]}
                    >
                        <Input allowClear />
                    </Form.Item>
                </Col>
            </Row>
            <Form.Item
                {...layoutActuatorLineItem}
                label={intl.formatMessage({ id: 'dv_metric_actuator_executor_options' })}
                name="others"
                rules={[...requiredRule]}
            >
                <Input.TextArea rows={3} />
            </Form.Item>
        </>
    );
    return (
        <Title title={intl.formatMessage({ id: 'dv_metric_title_actuator_engine_config' })}>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={<span>{intl.formatMessage({ id: 'dv_metric_title_actuator_engine' })}</span>}
                        name="engineType"
                        rules={[...requiredRule]}
                    >
                        <CustomSelect
                            source={engineList}
                            sourceValueMap="key"
                            style={{ width: 200 }}
                            disabled={datasourceReducer.modeType === 'comparison'}
                        />
                    </Form.Item>

                </Col>
            </Row>
            <Form.Item noStyle dependencies={['engineType']}>
                {() => {
                    const value = form.getFieldValue('engineType');
                    if (value !== 'spark') {
                        return null;
                    }
                    return renderSpark();
                }}
            </Form.Item>

        </Title>
    );
};

export default Index;
