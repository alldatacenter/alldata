import React, {
    useRef, useImperativeHandle, useState, useMemo,
} from 'react';
import {
    ModalProps, Tabs, Spin, Button, message,
} from 'antd';
import {
    useModal, useImmutable, usePersistFn, useContextModal, useMount, useLoading, IF,
} from 'src/common';
import { useIntl } from 'react-intl';
import { DvEditor } from '@Editor/index';
import { useSelector } from '@/store';
import { $http } from '@/http';
import Schedule from '../components/Schedule';
import PageContainer from './PageContainer';
import { SelectSLAsComponent } from '../useSelectSLAsModal';

const { TabPane } = Tabs;

type InnerProps = {
    innerRef: any,
    hide?: (...args: any[]) => any,
    baseData?:any,
    styleChildren?:any,
    styleTabContent?:any,
    style?:any
}

export const Inner = ({
    innerRef, hide, baseData, styleChildren, styleTabContent, style,
}: InnerProps) => {
    const [activeKey, setActiveKey] = useState('metric');
    const [loading, $setLoading] = useState(true);
    const setLoading = useLoading();
    const intl = useIntl();
    const { data } = baseData || useContextModal();
    // console.log('data', data);
    const [jobId, setJobId] = useState(data.record?.id);
    const [metricDetail, setMetricDetail] = useState(data.record?.parameterItem ? data.record : {});
    const metricConfigRef = useRef<any>();
    const { loginInfo } = useSelector((r) => r.userReducer);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const { locale } = useSelector((r) => r.commonReducer);
    const { entityUuid } = useSelector((r) => r.datasourceReducer);
    const editorParams = useMemo(() => ({
        baseURL: '/api/v1',
        workspaceId,
        headers: {
            Authorization: `Bearer ${loginInfo.token}`,
        },
    }), [workspaceId]);
    useImperativeHandle(innerRef, () => ({
        getData() {
            return {};
        },
    }));
    const getData = async (id: string, showLoading = true) => {
        if (showLoading) {
            setLoading(true);
        }
        const res = await $http.get(`/job/${id}`);
        res.parameterItem = res.parameter ? JSON.parse(res.parameter)[0] : {};
        if (showLoading) {
            setMetricDetail(res);
            setLoading(false);
        } else {
            return res;
        }
    };
    useMount(async () => {
        if (!data.record?.id) {
            $setLoading(false);
            return;
        }
        try {
            const res = await getData(data.record.id, false);
            console.log('data.record?.id)', data.record?.id, '获取详情');
            setMetricDetail(res);
        } catch (error) {
        } finally {
            setTimeout(() => {
                $setLoading(false);
            }, 100);
        }
    });
    const onJob = usePersistFn(async (runningNow = 0) => {
        try {
            setLoading(true);
            const params = await metricConfigRef.current.getValues();
            if (data.record?.id) {
                await $http.put('/job', { ...params, id: data.record?.id, runningNow });
            } else {
                let resData = {};
                let url = '/job';
                if (entityUuid) {
                    resData = {
                        entityUuid,
                        jobCreate: { ...params, runningNow },
                    };
                    url = '/catalog/add-metric';
                } else {
                    resData = { ...params, runningNow };
                }
                const res = await $http.post(url, resData);
                setJobId(res);
            }
            message.success('Success!');
            if (runningNow) {
                // eslint-disable-next-line no-unused-expressions
                hide && hide();
            }
        } catch (error) {
            console.log('error', error);
        } finally {
            setLoading(false);
        }
    });
    if (loading) {
        return <Spin spinning={loading} />;
    }
    const slaId = (data?.record?.slaList || [])[0]?.id;
    return (
        <div style={style}>
            <Tabs
                activeKey={activeKey}
                onChange={(key) => {
                    if (!jobId) {
                        message.info(intl.formatMessage({ id: 'jobs_add_tip' }));
                        return null;
                    }
                    setActiveKey(key);
                }}
            >
                <TabPane tab={intl.formatMessage({ id: 'jobs_tabs_config' })} key="metric">

                    <PageContainer
                        style={styleChildren || {}}
                        footer={(
                            <>
                                <Button type="primary" onClick={() => onJob()}>
                                    {intl.formatMessage({ id: 'common_save' })}

                                </Button>
                                <Button style={{ marginLeft: 10 }} type="primary" onClick={() => onJob(1)}>{intl.formatMessage({ id: 'jobs_save_run' })}</Button>
                            </>
                        )}
                    >
                        <div style={{ width: 'calc(100% - 80px)' }}>
                            <DvEditor {...editorParams} locale={locale} innerRef={metricConfigRef} id={data.id} showMetricConfig detail={metricDetail} />
                        </div>
                    </PageContainer>
                </TabPane>
                <TabPane tab={intl.formatMessage({ id: 'jobs_tabs_schedule' })} key="schedule">
                    <IF visible={jobId}><Schedule jobId={jobId} style={styleTabContent || {}} /></IF>
                </TabPane>
                <TabPane tab={intl.formatMessage({ id: 'jobs_tabs_SLA' })} key="SLA">
                    <IF visible={jobId}><SelectSLAsComponent jobId={jobId} id={slaId} style={styleTabContent || {}} /></IF>
                </TabPane>
            </Tabs>
        </div>
    );
};

export const useAddEditJobsModal = (options: ModalProps) => {
    const innerRef = useRef();
    const onOk = usePersistFn(() => {
    });
    const { Render, hide, ...rest } = useModal<any>({
        title: 'Schedule Manage',
        width: 640,
        ...(options || {}),
        bodyStyle: {
            overflow: 'hidden',
        },
        onOk,
        footer: null,
        className: 'dv-modal-fullscreen',
    });
    return {
        Render: useImmutable(() => (<Render><Inner hide={hide} innerRef={innerRef} /></Render>)),
        hide,
        ...rest,
    };
};
