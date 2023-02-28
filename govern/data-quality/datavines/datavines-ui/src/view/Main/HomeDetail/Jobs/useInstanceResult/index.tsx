/* eslint-disable react/no-danger */
import React, { useRef, useState } from 'react';
import { ModalProps, Row, Col } from 'antd';
import {
    useModal, useImmutable, useMount,
} from 'src/common';
import { useIntl } from 'react-intl';
import { $http } from '@/http';
import { useSelector } from '@/store';

type ResultProps = {
    checkResult?: string,
    checkSubject?: string,
    expectedType?: string,
    metricName?: string,
    resultFormulaFormat?: string,
    metricParameter?: Record<string, any>
}
const Inner = (props: any) => {
    const { locale } = useSelector((r) => r.commonReducer);
    const [result, setResult] = useState<ResultProps>({});
    const intl = useIntl();
    const getIntl = (id: any) => intl.formatMessage({ id });
    const getData = async () => {
        try {
            const res = (await $http.get<ResultProps>(`job/execution/result/${props.record.id}`)) || {};
            setResult(res);
        } catch (error) {
            console.log(error);
        } finally {
        }
    };

    const getItem = (key: string, value: any) => (
        <Row style={{ marginBottom: key === 'jobs_task_check_formula' ? 30 : 10 }}>
            <Col span={locale === 'zh_CN' ? 4 : 7} style={{ textAlign: 'right' }}>
                {getIntl(key)}
                ：
            </Col>
            <Col span={locale === 'zh_CN' ? 20 : 17}>{value}</Col>
        </Row>
    );
    const getParams = () => {
        const metricParameter = result.metricParameter || {};
        if (Object.keys(metricParameter).length <= 0) {
            return null;
        }
        return (
            <>
                <Row style={{
                    marginBottom: 10,
                }}
                >
                    <Col span={locale === 'zh_CN' ? 4 : 7} style={{ textAlign: 'right' }}>
                        {getIntl('jobs_task_check_params')}
                        ：
                    </Col>
                    <Col
                        span={locale === 'zh_CN' ? 20 : 17}
                    >
                        <div style={{ height: 22 }}>{' '}</div>
                        {
                            Object.keys(metricParameter).map((item) => (
                                <div>
                                    <span style={{ marginRight: 5 }}>-</span>
                                    {item}
                                    <span style={{ marginRight: 2 }}>：</span>
                                    {metricParameter[item]}
                                </div>
                            ))
                        }
                    </Col>
                </Row>
            </>
        );
    };
    useMount(() => {
        getData();
    });
    return (
        <div style={{
            fontSize: 14,
            minHeight: 260,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
        }}
        >
            {getItem('jobs_task_check_subject', result.checkSubject)}
            {getItem('jobs_task_check_rule', result.metricName)}
            {getParams()}
            {getItem('jobs_task_check_result', result.checkResult)}
            {getItem('jobs_task_check_expectVal_type', result.expectedType)}
            {getItem('jobs_task_check_formula', result.resultFormulaFormat)}
            {/* <Row style={{ marginBottom: 10, fontWeight: 500 }}>
                <Col span={locale === 'zh_CN' ? 4 : 7} style={{ textAlign: 'right' }}>
                    {getIntl('jobs_task_check_explain')}
                    ：
                </Col>
                <Col span={locale === 'zh_CN' ? 20 : 17}>{getIntl('jobs_task_check_explain_text')}</Col>
            </Row> */}
        </div>
    );
};

export const useInstanceResult = (options: ModalProps) => {
    const intl = useIntl();
    const recordRef = useRef<any>();
    const {
        Render, show, ...rest
    } = useModal<any>({
        title: `${intl.formatMessage({ id: 'jobs_task_check_result' })}`,
        footer: null,
        width: '600px',
        ...(options || {}),
        afterClose() {
            recordRef.current = null;
        },
    });
    return {
        Render: useImmutable(() => (<Render><Inner record={recordRef.current} /></Render>)),
        show(record: any) {
            recordRef.current = record;
            show(record);
        },
        ...rest,
    };
};
