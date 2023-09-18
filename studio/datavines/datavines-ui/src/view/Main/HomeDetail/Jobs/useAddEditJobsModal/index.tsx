import React, {
    useRef, useImperativeHandle, useState, useMemo,
} from 'react';
import {
    ModalProps, Tabs, Spin, Button, message, Input,
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
    const [jobId, setJobId] = useState(data.record?.id);
    const [metricDetail, setMetricDetail] = useState(data.record?.parameterItem ? data.record : {});
    const metricConfigRef = useRef<any>();
    const { loginInfo } = useSelector((r) => r.userReducer);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const { locale } = useSelector((r) => r.commonReducer);
    const { entityUuid } = useSelector((r) => r.datasourceReducer);
    const [jsonData, setJsonData] = useState('');
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
        try {
            res.parameter = res.parameter ? JSON.parse(res.parameter) || [] : [];
            res.parameterItem = res.parameter?.[0] || {};
        } catch (error) {
            console.log(error);
        }
        if (showLoading) {
            setMetricDetail(res);
            setLoading(false);
        } else {
            return res;
        }
    };
    const getConfig = async (id: string) => {
        const res = await $http.get(`/job/config/${id}`);
        setJsonData(JSON.stringify(JSON.parse(res), null, 4));
    };
    const downLoadConfig = () => {
        const blob = new Blob([jsonData], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        // eslint-disable-next-line no-nested-ternary
        a.download = data.record?.name ? `${data.record.name}.json`
            : (data.record?.jobName ? `${data.record.jobName}.json` : 'test.json');
        document.documentElement.appendChild(a);
        a.click();
        document.documentElement.removeChild(a);
    };
    useMount(async () => {
        if (!data.record?.id) {
            $setLoading(false);
            return;
        }
        try {
            const res = await getData(data.record.id, false);
            getConfig(data.record.id);
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
            if (jobId) {
                await $http.put('/job', { ...params, id: jobId, runningNow });
                getConfig(jobId);
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
                getConfig(res);
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
    const onCopy = async () => {
        const tempInputDOM = document.createElement('input');
        tempInputDOM.value = jsonData;
        document.body.appendChild(tempInputDOM);
        tempInputDOM.select(); // 选中input里的内容
        document.execCommand('Copy'); // 执行浏览器复制命令
        document.body.removeChild(tempInputDOM);
        message.success(intl.formatMessage({ id: 'common_success' }));
    };
    if (loading) {
        return <Spin spinning={loading} />;
    }
    const slaId = (data?.record?.slaList || [])[0]?.id;
    return (
        <div style={style}>
            <Tabs
                tabPosition="left"
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
                <TabPane tab={intl.formatMessage({ id: 'jobs_tabs_config_file' })} key="config">
                    <IF visible={jobId}>
                        <div style={{ marginBottom: 20, textAlign: 'right' }}>
                            <Button type="primary" onClick={onCopy}>
                                {intl.formatMessage({ id: 'jobs_tabs_config_file_copy' })}
                            </Button>
                            <Button style={{ marginLeft: 10 }} onClick={downLoadConfig}>
                                {intl.formatMessage({ id: 'jobs_tabs_config_file_download' })}
                            </Button>
                        </div>
                        <Input.TextArea key={activeKey} style={{ height: 'calc(100vh - 200px)', marginBottom: 24 }} readOnly value={jsonData} />
                    </IF>
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
