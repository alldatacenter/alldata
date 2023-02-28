import React, { useImperativeHandle, useRef, useState } from 'react';
import {
    Button, Form, FormInstance, message,
} from 'antd';
import { useIntl } from 'react-intl';
import {
    useModal, useImmutable, usePersistFn, useLoading, IF,
} from '@/common';
import useRequest from '../../hooks/useRequest';
import RuleSelect from './RuleSelect';
import MetricSelect from './MetricSelect';
import ExpectedValue from './ExpectedValue';
import VerifyConfigure from './VerifyConfigure';
import ActuatorConfigure from './ActuatorConfigure';
import RunEvnironment from './RunEvnironment';
import OtherConfig from './OtherConfig';
import { pickProps } from './helper';
import ErrorDataStoreConfig from './ErrorDataStoreConfig';
import { TDetail } from './type';
import store, { RootReducer } from '@/store';

type InnerProps = {
    innerRef?: {
        current: {
            form: FormInstance,
            getValues: (...args: any[]) => any
        }
    },
    id: string,
    detail: TDetail,
}
const keys = [
    'engineType',
    'retryTimes',
    'retryInterval',
    'timeout',
    'timeoutStrategy',
    'tenantCode',
    'env',
    'errorDataStorageId',
];
export const MetricConfig = (props: InnerProps) => {
    const { innerRef, detail } = props;
    // console.log('detail', detail, props);
    const [form] = Form.useForm();
    const metricSelectRef = useRef<any>();
    const id = props.id || detail?.dataSourceId;
    const [metricType, setMetricTypeParent] = useState('');
    const { datasourceReducer } = store.getState() as RootReducer;
    useImperativeHandle(innerRef, () => ({
        form,
        getValues() {
            return new Promise((resolve, reject) => {
                innerRef?.current.form.validateFields().then((values) => {
                    console.log('values', values);
                    const params: any = {
                        type: datasourceReducer.modeType === 'comparison' ? 'DATA_RECONCILIATION' : 'DATA_QUALITY',
                        dataSourceId: id,
                        ...(pickProps(values, [...keys])),
                    };
                    if (values.engineType === 'spark') {
                        params.engineParameter = JSON.stringify({
                            programType: 'JAVA',
                            ...pickProps(values, ['deployMode', 'driverCores', 'driverMemory', 'numExecutors', 'executorMemory', 'executorCores', 'others']),
                        });
                    }
                    const parameter: any = {
                        ...(pickProps(values, ['metricType', 'expectedType', 'resultFormula', 'operator', 'threshold'])),
                        metricParameter: {
                            ...(pickProps(values, ['database', 'table', 'column', 'filter'])),
                        },
                    };
                    if (values.expectedType === 'fix_value') {
                        parameter.expectedParameter = {
                            expected_value: values.expected_value,
                        };
                    }
                    if (datasourceReducer.modeType === 'comparison') {
                        params.dataSourceId2 = values.dataSourceId2;
                        Object.assign(parameter, pickProps(values, ['metricParameter', 'metricParameter2']));
                        if (values.metricType === 'multi_table_accuracy') {
                            values.mappingColumns.forEach((item: any) => {
                                item.operator = '=';
                            });
                            parameter.mappingColumns = values.mappingColumns;
                        }
                    } else if (datasourceReducer.modeType === 'quality') {
                        parameter.metricParameter = {
                            ...(pickProps(values, ['database', 'table', 'column', 'filter'])),
                            ...metricSelectRef.current.getDynamicValues(),
                        };
                    }
                    params.parameter = JSON.stringify([parameter]);
                    console.log('params', params);
                    resolve(params);
                }).catch((error) => {
                    console.log('error', error);
                    reject(error);
                });
            });
        },
    }));
    return (
        <Form form={form}>
            <IF visible={datasourceReducer.modeType === 'comparison'}>
                <RuleSelect detail={detail} id={id} form={form} setMetricTypeParent={setMetricTypeParent} />
            </IF>
            <IF visible={datasourceReducer.modeType === 'quality'}>
                <MetricSelect detail={detail} id={id} form={form} metricSelectRef={metricSelectRef} />
            </IF>
            <IF visible={datasourceReducer.modeType === 'quality' || metricType === 'multi_table_accuracy'}>
                <ExpectedValue detail={detail} form={form} />
            </IF>
            <VerifyConfigure detail={detail} form={form} />
            <ActuatorConfigure detail={detail} form={form} />
            <RunEvnironment id={id} form={form} />
            <OtherConfig detail={detail} form={form} />
            <ErrorDataStoreConfig detail={detail} form={form} />
        </Form>
    );
};

export const useMetricModal = () => {
    const innerRef: InnerProps['innerRef'] = useRef<any>();
    const intl = useIntl();
    const { $http } = useRequest();
    const [id, setId] = useState('');
    const idRef = useRef(id);
    idRef.current = id;
    const [detail, setDetail] = useState<TDetail>(null);
    const detailRef = useRef(detail);
    detailRef.current = detail;
    const setLoading = useLoading();
    const onJob = usePersistFn(async (runningNow = 0) => {
        try {
            setLoading(true);
            const params = await innerRef.current.getValues();
            // console.log('params', params);
            const res = await $http.post('/job', { ...params, runningNow });
            console.log('res', res);
            message.success('Success!');
        } catch (error) {
            console.log('error', error);
        } finally {
            setLoading(false);
        }
    });
    const onSave = usePersistFn(async () => {
        onJob();
    });
    const onSaveRun = usePersistFn(async () => {
        onJob(1);
    });
    const { Render, show, ...rest } = useModal<any>({
        title: (
            <div className="dv-editor-flex-between" style={{ height: '100%' }}>
                <span>
                    {'Metric '}
                    {
                        intl.formatMessage({ id: 'dv_config_text' })
                    }
                </span>
                <span style={{ marginRight: 20, marginTop: -8 }}>
                    <Button type="primary" onClick={onSave}>{intl.formatMessage({ id: 'dv_metric_save' })}</Button>
                    <Button type="primary" onClick={onSaveRun} style={{ marginLeft: 12 }}>{intl.formatMessage({ id: 'dv_metric_save_run' })}</Button>
                </span>
            </div>
        ),
        className: 'dv-modal-fullscreen',
        width: 900,
        afterClose() {
            setId('');
            setDetail(null);
        },
        maskClosable: false,
        footer: null,
    });
    return {
        Render: useImmutable(() => (<Render><MetricConfig id={idRef.current} detail={detailRef.current} innerRef={innerRef} /></Render>)),
        show($id: string, $detail: TDetail) {
            setId($id);
            setDetail($detail);
            show({});
        },
        ...rest,
    };
};
