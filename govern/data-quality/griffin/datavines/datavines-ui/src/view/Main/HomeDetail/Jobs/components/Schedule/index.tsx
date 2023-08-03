/* eslint-disable jsx-a11y/label-has-associated-control */
/* eslint-disable react/no-unstable-nested-components */
import React, {
    useRef, useImperativeHandle, useState, useEffect,
} from 'react';
import { useIntl } from 'react-intl';
import {
    Input, InputNumber, Form, Radio, DatePicker, Col, Row, Button, FormInstance, message, Spin,
} from 'antd';
import useRequiredRule from '@Editor/hooks/useRequiredRule';
import dayjs from 'dayjs';
import { CustomSelect, useMount, useLoading } from '@/common';
import PageContainer from '../../useAddEditJobsModal/PageContainer';
import { pickProps } from '@/utils';
import { $http } from '@/http';

const get100Years = () => dayjs().add(100, 'year');
type TParam = {
    cycle: string,
    crontab: null | string,
    parameter?: {
        minute: number,
        nminute: number,
        hour: number,
        nhour: number,
        wday: number,
        day: number,
    }
}
type TDetail = {
    id: number;
    jobId: number;
    type: string;
    param?: TParam;
    cronExpression: string;
    status: boolean;
    startTime: string;
    endTime: string;
    createBy: number;
    createTime: Date;
    updateBy: number;
    updateTime: Date;
}
type ScheduleProps = {
    formRef: { current: FormInstance},
    detail: null | TDetail,
}
const commonStyle = {
    display: 'inline-block',
    lineHeight: '32px',
};
const Schedule: React.FC<ScheduleProps> = ({ formRef, detail }) => {
    const intl = useIntl();
    const form = Form.useForm()[0];
    const requiredRule = useRequiredRule();
    const getIntl = (id: any) => intl.formatMessage({ id });
    useImperativeHandle(formRef, () => form);
    const cycleSource = [
        { label: getIntl('jobs_schedule_cycle_month'), value: 'month' },
        { label: getIntl('jobs_schedule_cycle_week'), value: 'week' },
        { label: getIntl('jobs_schedule_cycle_day'), value: 'day' },
        { label: getIntl('jobs_schedule_cycle_hour'), value: 'hour' },
        { label: getIntl('jobs_schedule_cycle_nhour'), value: 'nhour' },
        { label: getIntl('jobs_schedule_cycle_nminute'), value: 'nminute' },
    ];
    const WeekSrouce = [
        { label: getIntl('jobs_monday'), value: 2, key: 'week_1' },
        { label: getIntl('jobs_tuesday'), value: 3, key: 'week_2' },
        { label: getIntl('jobs_wednesday'), value: 4, key: 'week_3' },
        { label: getIntl('jobs_thursday'), value: 5, key: 'week_4' },
        { label: getIntl('jobs_friday'), value: 6, key: 'week_5' },
        { label: getIntl('jobs_saturday'), value: 7, key: 'week_6' },
        { label: getIntl('jobs_sunday'), value: 1, key: 'week_0' },
    ];
    // @ts-ignore
    const Date = <DatePicker showTime />;
    useMount(() => {});
    const timeMap = useRef({
        minute: () => (
            <Form.Item
                label=""
                name="minute"
                rules={requiredRule}
                initialValue={detail?.param?.parameter?.minute}
                style={{ width: 60, display: 'inline-block' }}
            >
                <InputNumber
                    autoComplete="off"
                    min={0}
                    max={59}
                    style={{ width: 60 }}
                />
            </Form.Item>
        ),
        nminute: () => (
            <Form.Item
                label=""
                name="nminute"
                rules={requiredRule}
                initialValue={detail?.param?.parameter?.nminute}
                style={{ width: 60, display: 'inline-block' }}
            >
                <InputNumber
                    autoComplete="off"
                    min={0}
                    max={59}
                    style={{ width: 60 }}
                />
            </Form.Item>
        ),
        hour: () => (
            <Form.Item
                label=""
                name="hour"
                rules={requiredRule}
                initialValue={detail?.param?.parameter?.hour}
                style={{ width: 60, display: 'inline-block' }}
            >
                <InputNumber
                    autoComplete="off"
                    min={0}
                    max={23}
                    style={{ width: 60 }}
                />
            </Form.Item>
        ),
        nhour: () => (
            <Form.Item
                label=""
                name="nhour"
                rules={requiredRule}
                initialValue={detail?.param?.parameter?.nhour}
                style={{ width: 60, display: 'inline-block' }}
            >
                <InputNumber
                    autoComplete="off"
                    min={0}
                    max={23}
                    style={{ width: 60 }}
                />
            </Form.Item>
        ),
        wday: () => (
            <Form.Item
                label=""
                name="wday"
                rules={requiredRule}
                initialValue={detail?.param?.parameter?.wday ? +detail.param.parameter.wday : ''}
                style={{ width: 120, display: 'inline-block' }}
            >
                <CustomSelect source={WeekSrouce} style={{ width: 120 }} />
            </Form.Item>
        ),
        day: () => (
            <Form.Item
                label=""
                name="day"
                rules={requiredRule}
                initialValue={detail?.param?.parameter?.day}
                style={{ width: 160, display: 'inline-block' }}
            >
                <InputNumber
                    autoComplete="off"
                    min={1}
                    max={31}
                    style={{ width: 160 }}
                    formatter={(value: any) => (value ? `${getIntl('jobs_every_month')}${value}${getIntl('jobs_every_month_day')}` : '')}
                    parser={(value: any) => (value || '').replace(getIntl('jobs_every_month'), '').replace(getIntl('jobs_every_month_day'), '')}
                />
            </Form.Item>
        ),
    }).current;
    const renderScheduleTime = (cycleValue: string) => {
        switch (cycleValue) {
            case 'month':
                return (
                    <>
                        {timeMap.day()}
                        <span style={{ ...commonStyle, margin: '0 5px' }} />
                        {timeMap.hour()}
                        <span style={{ ...commonStyle, margin: '0 5px' }}>:</span>
                        {timeMap.minute()}
                        <span style={{ ...commonStyle, marginLeft: 5 }}>{getIntl('jobs_week_after')}</span>
                    </>
                );
            case 'week':
                return (
                    <>
                        <span style={{ ...commonStyle, marginRight: 5 }}>{getIntl('jobs_week_before')}</span>
                        {timeMap.wday()}
                        <span style={{ ...commonStyle, margin: '0 5px' }} />
                        {timeMap.hour()}
                        <span style={{ ...commonStyle, margin: '0 5px' }}>:</span>
                        {timeMap.minute()}
                        <span style={{ ...commonStyle, marginLeft: 5 }}>{getIntl('jobs_week_after')}</span>
                    </>
                );
            case 'day':
                return (
                    <>
                        <span style={{ ...commonStyle, marginRight: 5 }}>{getIntl('jobs_day_before')}</span>
                        {timeMap.hour()}
                        <span style={{ ...commonStyle, margin: '0 5px' }}>:</span>
                        {timeMap.minute()}
                        <span style={{ ...commonStyle, marginLeft: 5 }}>{getIntl('jobs_day_after')}</span>
                    </>
                );
            case 'nhour':
                return (
                    <>
                        <span style={{ ...commonStyle, marginRight: 5 }}>{getIntl('jobs_nhour_before')}</span>
                        {timeMap.nhour()}
                        <span style={{ ...commonStyle, margin: '0 5px' }}>:</span>
                        {timeMap.hour()}
                        <span style={{ ...commonStyle, marginRight: 5 }}>{getIntl('jobs_nhour_middle')}</span>
                        {timeMap.minute()}
                        <span style={{ ...commonStyle, marginLeft: 5 }}>{getIntl('jobs_nhour_after')}</span>
                    </>
                );
            case 'hour':
                return (
                    <>
                        <span style={{ ...commonStyle, marginRight: 5 }}>{getIntl('jobs_hour_before')}</span>
                        {timeMap.minute()}
                        <span style={{ ...commonStyle, marginLeft: 5 }}>{getIntl('jobs_hour_after')}</span>
                    </>
                );
            case 'nminute':
                return (
                    <>
                        <span style={{ ...commonStyle, marginRight: 5 }}>{getIntl('jobs_nminute_before')}</span>
                        {timeMap.nminute()}
                        <span style={{ ...commonStyle, margin: '0 5px' }}>{getIntl('jobs_nminute_middle')}</span>
                        {timeMap.minute()}
                        <span style={{ ...commonStyle, marginLeft: 5 }}>{getIntl('jobs_nminute_after')}</span>
                    </>
                );
            default:
                return <div style={{ marginBottom: 24, height: 32 }}> </div>;
        }
    };
    return (
        <Form form={form}>
            <Form.Item
                label={intl.formatMessage({ id: 'jobs_schedule_type' })}
                name="type"
                rules={requiredRule}
                initialValue={detail?.type || 'cycle'}
            >
                <Radio.Group>
                    <Radio value="cycle">{intl.formatMessage({ id: 'jobs_schedule_custom' })}</Radio>
                    <Radio value="cron">{intl.formatMessage({ id: 'jobs_schedule_crontab' })}</Radio>
                    <Radio value="offline">{intl.formatMessage({ id: 'jobs_schedule_offline' })}</Radio>
                </Radio.Group>
            </Form.Item>
            <Form.Item noStyle dependencies={['type']}>
                {() => {
                    const value = form.getFieldValue('type');
                    if (value !== 'cycle') {
                        return null;
                    }
                    return (
                        <>
                            <Form.Item
                                label={intl.formatMessage({ id: 'jobs_schedule_cycle' })}
                                name="cycle"
                                rules={requiredRule}
                                initialValue={detail?.param?.cycle}
                            >
                                <CustomSelect source={cycleSource} style={{ width: 240 }} />
                            </Form.Item>
                            <Form.Item noStyle dependencies={['cycle']}>
                                {() => {
                                    const cycleValue = form.getFieldValue('cycle');
                                    return (
                                        <Row>
                                            <div
                                                className="ant-col ant-form-item-label"
                                                style={{
                                                    marginLeft: '11px',
                                                    height: '32px',
                                                    marginBottom: '20px',
                                                    lineHeight: '32px',
                                                }}
                                            >
                                                <label className="ant-form-item-required">
                                                    {intl.formatMessage({ id: 'jobs_schedule_time' })}
                                                    <span style={{
                                                        marginBlock: '0',
                                                        marginInlineStart: '2px',
                                                        marginInlineEnd: '8px',
                                                    }}
                                                    >
                                                        :

                                                    </span>
                                                </label>
                                            </div>
                                            {renderScheduleTime(cycleValue)}
                                        </Row>
                                    );
                                }}
                            </Form.Item>

                            <Form.Item
                                label={<span style={{ marginLeft: 11 }}>{intl.formatMessage({ id: 'jobs_schedule_express' })}</span>}
                                name=" "
                                initialValue={undefined}
                                style={{ height: 32 }}
                            >
                                <div style={{ color: '#ff4d4f' }}>{detail?.cronExpression}</div>
                            </Form.Item>
                        </>
                    );
                }}
            </Form.Item>
            <Form.Item noStyle dependencies={['type']}>
                {() => {
                    const value = form.getFieldValue('type');
                    if (value === 'cron') {
                        return (
                            <Form.Item
                                label={intl.formatMessage({ id: 'jobs_schedule_express' })}
                                name="crontab"
                                rules={requiredRule}
                                initialValue={detail?.param?.crontab}
                            >
                                <Input autoComplete="off" style={{ width: 240 }} />
                            </Form.Item>
                        );
                    }
                    return null;
                }}
            </Form.Item>
            <Form.Item noStyle dependencies={['type']}>
                {() => {
                    const value = form.getFieldValue('type');
                    if (value === 'offline' || !value) {
                        return null;
                    }
                    return (
                        <Row>
                            <Col style={{ display: 'inline-block' }}>
                                <Form.Item
                                    label={intl.formatMessage({ id: 'jobs_schedule_obtain_time' })}
                                    name="startTime"
                                    initialValue={detail?.startTime ? dayjs(detail?.startTime) : dayjs()}
                                    rules={requiredRule}
                                >
                                    {Date}
                                </Form.Item>

                            </Col>
                            <span style={{ margin: '5px 10px 0px' }}>
                                {getIntl('jobs_schedule_time_to')}
                            </span>
                            <Form.Item
                                label=""
                                name="endTime"
                                initialValue={detail?.endTime ? dayjs(detail?.endTime) : get100Years()}
                                rules={requiredRule}
                                style={{ display: 'inline-block' }}
                            >
                                {Date}
                            </Form.Item>
                        </Row>
                    );
                }}
            </Form.Item>

        </Form>
    );
};
const ScheduleContainer = ({
    jobId, api = 'job', width, onSavaEnd, style = {}, isShowPush = false,
}: {jobId: string | number, api?:string;width?:string;onSavaEnd?:()=>void;style?:any;isShowPush?:boolean}) => {
    const intl = useIntl();
    const globalSetLoading = useLoading();
    const [loading, setLoading] = useState(true);
    const [detail, setDetail] = useState<TDetail | null>(null);
    const formRef = React.useRef() as { current: FormInstance};
    const getValues = (callback: (...args: any[]) => any) => {
        formRef.current.validateFields().then(async (values) => {
            const params: any = {
                type: values.type,
            };
            if (values.type !== 'offline') {
                params.startTime = dayjs(values.startTime).format('YYYY-MM-DD HH:mm:ss');
                params.endTime = dayjs(values.endTime).format('YYYY-MM-DD HH:mm:ss');
                params.param = {};
            }
            if (values.type === 'cron') {
                params.param.crontab = values.crontab;
            }
            if (values.type === 'cycle') {
                params.param = {
                    cycle: values.cycle,
                    parameter: pickProps(values, ['minute', 'nminute', 'hour', 'nhour', 'wday', 'day']),
                };
            }
            if (callback) {
                callback(params);
            }
        }).catch((err) => {
            console.log(err);
        });
    };
    const getData = async () => {
        try {
            const res = await $http.get(`/${api}/schedule/${jobId}`);
            if (res) {
                if (res.param) {
                    res.param = JSON.parse(res.param);
                }
                formRef?.current?.setFieldsValue({
                    type: res?.type,
                    nminute: res?.param?.parameter?.nminute,
                    minute: res?.param?.parameter?.minute,
                    nhour: res?.param?.parameter?.nhour,
                    day: res?.param?.parameter?.day,
                    wday: res?.param?.parameter?.wday ? +res.param.parameter.wday : '',
                    hour: res?.param?.parameter?.hour,
                    endTime: res?.endTime ? dayjs(res?.endTime) : get100Years(),
                    startTime: res?.startTime ? dayjs(res?.startTime) : dayjs(),
                    crontab: res?.param?.crontab,
                    cycle: res?.param?.cycle,
                });
                setDetail(res);
            } else {
                formRef.current.setFieldsValue({
                    type: 'cycle',
                    nminute: '',
                    minute: '',
                    nhour: '',
                    day: '',
                    hour: '',
                    crontab: '',
                    cycle: '',
                    wday: '',

                });
                setDetail(null);
            }
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        getData();
    }, [jobId]);
    const onSave = async (type:string = 'add') => {
        if (type === 'push') {
            await $http.post('/catalog/refresh', {
                datasourceId: jobId,
            });
            // eslint-disable-next-line no-unused-expressions
            onSavaEnd && onSavaEnd();
            message.success(intl.formatMessage({ id: 'common_success' }));
            return;
        }

        getValues(async (params: any) => {
            globalSetLoading(true);
            try {
                const $params = {
                    jobId,
                    entityUUID: jobId,
                    ...params,
                };
                if (api === 'catalog/metadata') {
                    $params.dataSourceId = jobId;
                }
                if (detail?.id) {
                    $params.id = detail?.id;
                }
                await $http.post(`/${api}/schedule/createOrUpdate`, { ...$params, ...params.param });
                getData();
                // eslint-disable-next-line no-unused-expressions
                onSavaEnd && onSavaEnd();
                message.success(intl.formatMessage({ id: 'common_success' }));
            } catch (error) {
            } finally {
                globalSetLoading(false);
            }
        });
    };

    if (loading) {
        return <Spin spinning={loading} />;
    }
    return (
        <PageContainer
            style={style}
            footer={[
                isShowPush ? <Button style={{ marginRight: '10px' }} type="primary" onClick={() => onSave('push')}>{intl.formatMessage({ id: 'home_metadata_fetch' })}</Button> : undefined,
                <Button type="primary" onClick={() => onSave('add')}>{intl.formatMessage({ id: 'common_save' })}</Button>,
            ]}
        >
            <div style={{ width: width || 'calc(100vw - 80px)' }}>
                <Schedule formRef={formRef} detail={detail} />
            </div>
        </PageContainer>
    );
};

export default ScheduleContainer;
