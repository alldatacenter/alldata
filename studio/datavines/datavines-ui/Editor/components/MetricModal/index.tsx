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
import MetricTabs from './MetricTabs';

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
    const [form] = Form.useForm();
    const metricRef = useRef<any>();
    const id = props.id || detail?.dataSourceId;
    const { datasourceReducer } = store.getState() as RootReducer;
    useImperativeHandle(innerRef, () => ({
        form,
        getValues() {
            // eslint-disable-next-line no-async-promise-executor
            return new Promise((resolve, reject) => {
                innerRef?.current.form.validateFields().then(async (values) => {
                    let parameterArray: any = null;
                    try {
                        parameterArray = await metricRef.current.getValues();
                    } catch (error) {
                        console.log(error);
                    }
                    if (!parameterArray || parameterArray?.length < 1) {
                        reject();
                        return;
                    }
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
                    params.parameter = JSON.stringify(parameterArray);
                    resolve(params);
                }).catch((error) => {
                    console.log(error);
                    reject(error);
                });
            });
        },
    }));
    return (
        <>
            <MetricTabs detail={detail} id={id} metricRef={metricRef} />
            <Form form={form}>
                <ActuatorConfigure detail={detail} form={form} />
                <RunEvnironment id={id} form={form} detail={detail} />
                <OtherConfig detail={detail} form={form} />
                <ErrorDataStoreConfig detail={detail} form={form} />
            </Form>
        </>
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
            const res = await $http.post('/job', { ...params, runningNow });
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
