import React, { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { DvEditor } from '@Editor/index';
import { useIntl } from 'react-intl';
import { useSelector } from '@/store';
import { usePersistFn, useWatch } from '@/common';
import { useAddEditJobsModal } from '../Jobs/useAddEditJobsModal';

const EditorData = () => {
    const intl = useIntl();
    const [visible, setVisible] = useState(true);
    let action:any = null;
    const afterClose = (cb?: () => void) => {
        action = cb;
    };
    const { Render: RenderJobsModal, show: showJobsModal } = useAddEditJobsModal({
        title: intl.formatMessage({ id: 'jobs_tabs_title' }),
        afterClose: () => {
            // eslint-disable-next-line no-unused-expressions
            action && action();
        },
    });

    const params = useParams<{ id: string}>();
    const { loginInfo } = useSelector((r) => r.userReducer);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const { locale } = useSelector((r) => r.commonReducer);
    const editorParams = useMemo(() => ({
        workspaceId,
        monacoConfig: { paths: { vs: '/monaco-editor/min/vs' } },
        baseURL: '/api/v1',
        headers: {
            Authorization: `Bearer ${loginInfo.token}`,
        },
    }), [workspaceId]);
    const onShowModal = usePersistFn((data: any) => {
        showJobsModal({
            id: params.id,
            record: data,
        });
    });
    useWatch(params.id, () => {
        setVisible(false);
        setTimeout(() => {
            setVisible(true);
        }, 100);
    });
    if (!visible) {
        return <div>loading...</div>;
    }
    return (
        <div style={{ height: 'calc(100vh - 74px)', background: '#fff' }}>
            <DvEditor {...editorParams} onShowModal={onShowModal} afterClose={afterClose} locale={locale} id={params.id} />
            <RenderJobsModal />
        </div>
    );
};

export default EditorData;
