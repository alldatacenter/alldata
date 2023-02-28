/* eslint-disable react/no-danger */
import React, { useRef, useState, useImperativeHandle } from 'react';
import { ModalProps, Spin } from 'antd';
import { FullscreenExitOutlined, DownloadOutlined, SyncOutlined } from '@ant-design/icons';
import {
    useModal, useContextModal, useImmutable, usePersistFn, useMount, IF,
} from 'src/common';
import { useIntl } from 'react-intl';
import { $http } from '@/http';
import { download } from '@/utils';

type InnerProps = {
    innerRef: any
}

const dealMsg = (msg: string) => {
    if (msg) {
        return msg.replace(/\r\n/g, '<br>');
    }
    return '';
};
const Inner = ({ innerRef }: InnerProps) => {
    const [loading, setLoading] = useState(false);
    const { data } = useContextModal();
    const [wholeLog, setWholeLog] = useState<{offsetLine: number, msg: string}[]>([]);
    const getData = async (offsetLine: number) => {
        try {
            setLoading(true);
            const res = (await $http.get('task/log/queryLogWithOffsetLine', {
                taskId: data.id,
                offsetLine,
            })) || [];
            res.msg = dealMsg(res.msg);
            if (offsetLine === 0) {
                setWholeLog([res]);
            } else {
                setWholeLog([...wholeLog, res]);
            }
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    useMount(async () => {
        getData(0);
    });
    useImperativeHandle(innerRef, () => ({
        onRefresh() {
            getData(wholeLog[wholeLog.length - 1]?.offsetLine || 0);
        },
    }));
    return (
        <Spin spinning={loading}>
            <div style={{ minHeight: 300 }}>
                {
                    wholeLog.map((item) => (
                        <div dangerouslySetInnerHTML={{ __html: item.msg }} />
                    ))
                }
                <div />
            </div>
        </Spin>
    );
};

export const useLogger = (options: ModalProps) => {
    const intl = useIntl();
    // const [className, setClassName] = useState('dv-modal-fullscreen');
    // const classNameRef = useRef(className);
    // classNameRef.current = className;
    const innerRef = useRef<any>();
    const recordRef = useRef<any>();
    const onOk = usePersistFn(() => {
        hide();
    });
    const onDownload = usePersistFn(async () => {
        try {
            const blob = await $http.get('task/log/download', { taskId: recordRef.current?.id }, {
                responseType: 'blob',
            });
            download(blob);
        } catch (error) {
        }
    });
    const {
        Render, hide, show, ...rest
    } = useModal<any>({
        title: (
            <div className="dv-flex-between">
                <span>{intl.formatMessage({ id: 'job_log_view_log' })}</span>
                <div style={{ marginRight: 30 }}>
                    <a
                        style={{ marginRight: 10 }}
                        onClick={() => {
                            innerRef.current.onRefresh();
                        }}
                    >
                        <SyncOutlined style={{ marginRight: 5 }} />
                        {intl.formatMessage({ id: 'job_log_refresh' })}
                    </a>
                    <a style={{ marginRight: 10 }} onClick={onDownload}>
                        <DownloadOutlined style={{ marginRight: 5 }} />
                        {intl.formatMessage({ id: 'job_log_download' })}
                    </a>
                    {/* <IF visible={!classNameRef.current}>
                        <a onClick={() => {
                            setClassName('dv-modal-fullscreen');
                        }}
                        >
                            <FullscreenExitOutlined style={{ marginRight: 5 }} />
                            {intl.formatMessage({ id: 'job_log_fullScreen' })}
                        </a>
                    </IF> */}

                </div>
            </div>
        ),
        className: 'dv-modal-fullscreen',
        footer: null,
        width: '90%',
        ...(options || {}),
        afterClose() {
            // setClassName('');
            recordRef.current = null;
        },
        onOk,
    });
    return {
        Render: useImmutable(() => (<Render><Inner innerRef={innerRef} /></Render>)),
        show(record: any) {
            recordRef.current = record;
            show(record);
        },
        ...rest,
    };
};
