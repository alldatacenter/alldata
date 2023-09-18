import React, {
    useRef, useState, useMemo, useEffect,
} from 'react';
import { Divider } from 'antd';
import { useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import Tree from './Tree';
import Monaco from './Monaco';
import { IDvEditorProps, IDvSqlTable } from '../type';
import useRequest from '../hooks/useRequest';
import {
    usePersistFn, useMount, IF,
} from '../common';
import { useEditorActions, setEditorFn, useEditorContextState } from '../store/editor';
import SqlTable from './SqlTable';
import DataBase from './Database';
import './index.less';

const Index = (props: IDvEditorProps) => {
    const [{ id, selectDatabases }] = useEditorContextState();
    const { editType } = useSelector((r:any) => r.datasourceReducer);
    const [tableData, setTableData] = useState<IDvSqlTable>({ resultList: [], columns: [] });
    const { $http } = useRequest();
    const monacoRef = useRef<any>();
    const containerRef = useRef<any>();
    const intl = useIntl();
    const fns = useEditorActions({ setEditorFn });
    const [height, setHeight] = useState(null);
    const currentId = useRef('');
    const monacoStyle = useMemo(() => {
        if (!height) {
            return {};
        }
        return {
            height: 260,
        };
    }, [height]);
    const sqlStyle = useMemo(() => {
        if (!height) {
            return {};
        }
        return {
        };
    }, [height]);
    const getDatabases = usePersistFn(async (data:string) => {
        try {
            if (!data) {
                const res = await $http.get(`/catalog/list/database/${currentId.current}`);
                fns.setEditorFn({ databases: res || [] });
                if (res && res.length > 0)localStorage.setItem(currentId.current, JSON.stringify(res));
                return;
            }
            if (!localStorage.getItem(data)) {
                const res = await $http.get(`/catalog/list/database/${data}`);
                fns.setEditorFn({ databases: res || [] });
                if (res && res.length > 0)localStorage.setItem(data, JSON.stringify(res));
            } else {
                fns.setEditorFn({ databases: JSON.parse(localStorage.getItem(data) as string) });
            }
        } catch (error) {
        }
    });
    useMount(() => {
        setHeight(containerRef.current.clientHeight);
    });

    const onRun = async () => {
        const val = monacoRef.current.getValue();
        if (!val || val === '\n') {
            return;
        }
        try {
            const params = {
                datasourceId: id,
                script: val,
                variables: '',
            };
            const res = await $http.post('/datasource/execute', params);
            setTableData(res || {});
        } catch (error) {
            setTableData({} as any);
        }
    };
    useEffect(() => {
        if (selectDatabases.length > 0 && currentId.current !== selectDatabases[0].uuid) {
            try {
                currentId.current = selectDatabases[0].uuid;
                getDatabases(selectDatabases[0].uuid);
            } catch (error) {
                console.log('error', error);
            }
        }
    }, [selectDatabases]);

    return (
        <div className="dv-editor" ref={containerRef}>
            <div className="dv-content">
                <div className="dv-editor-left">
                    <Tree getDatabases={getDatabases} onShowModal={props.onShowModal} />
                </div>
                <div className="dv-editor-right">
                    <IF visible={editType}>
                        <div className="dv-editor__header">
                            <a onClick={onRun}>{intl.formatMessage({ id: 'dv_metric_run' })}</a>
                        </div>
                        <Monaco monacoRef={monacoRef} style={monacoStyle} />
                        <Divider />
                        <SqlTable style={sqlStyle} tableData={tableData} />
                    </IF>
                    <IF visible={!editType}>
                        <DataBase onShowModal={props.onShowModal} afterClose={props.afterClose} />
                    </IF>
                </div>
            </div>

        </div>
    );
};

export default Index;
