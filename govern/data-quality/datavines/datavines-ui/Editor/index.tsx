// Editor component library entry
import React, { useState } from 'react';
import Editor from './components';
import './index.less';
import enUS from './locale/en_US';
import zhCN from './locale/zh_CN';
import { IDvEditorProps } from './type';
import {
    EditorProvider, useEditorActions, setEditorFn,
} from './store/editor';
import { useMount, useWatch } from './common';
import { MetricConfig } from './components/MetricModal';
// eslint-disable-next-line import/order
import querystring from 'querystring';
import { $http } from '@/http';

const App = (props: IDvEditorProps) => {
    const {
        monacoConfig, baseURL, headers, id, workspaceId, locale, ...rest
    } = props;
    const [loading, setLoading] = useState(true);
    const fns = useEditorActions({ setEditorFn });
    const [qs] = useState(querystring.parse(window.location.href.split('?')[1] || ''));

    // console.log("qs", qs)
    // console.log('selectDatabases', JSON.stringify(selectDatabases));
    useMount(async () => {
        let { name } = qs;
        let uuid = '';
        // if (!name && id) {
        const $dataSoucrce = await $http.get('/datasource/page', {
            workSpaceId: workspaceId,
            pageNumber: 1,
            pageSize: 9999,
        });
        $dataSoucrce.records.forEach((element: {
                uuid: string; id: string | number; name: string | string[] | undefined;
}) => {
            // console.log(element.id === id, element.id, id);
            if (`${element.id}` === id) {
                name = element.name;
                uuid = element.uuid;
            }
        });
        // console.log('执行');
        // }
        console.log('uuid');
        fns.setEditorFn({
            workspaceId,
            monacoConfig,
            locale,
            baseURL,
            headers: headers || {},
            id,
            selectDatabases: [{
                name,
                uuid,
            }],
        });
        setLoading(false);
    });

    useWatch(id, async () => {
        let { name } = qs;
        let uuid = '';
        // if (!name && id) {
        const $dataSoucrce = await $http.get('/datasource/page', {
            workSpaceId: workspaceId,
            pageNumber: 1,
            pageSize: 9999,
        });
        $dataSoucrce.records.forEach((element: {
                uuid: string; id: string | number; name: string | string[] | undefined;
}) => {
            if (element.id === id) {
                name = element.name;
                uuid = element.uuid;
            }
        });
        console.log('执行');
        // }
        fns.setEditorFn({
            id,
            selectDatabases: [{
                name,
                uuid,
            }],
        });
    });
    if (loading) {
        return null;
    }

    if (props.showMetricConfig) {
        return <MetricConfig id={id as string} detail={props.detail || null} {...rest} />;
    }
    return <Editor {...props} />;
};

const DvEditor = (props: IDvEditorProps) => (
    <EditorProvider>
        <App {...props} />
    </EditorProvider>
);

export {
    DvEditor,
    enUS,
    zhCN,
};
