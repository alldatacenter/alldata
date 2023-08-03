import React from 'react';
import useRequest from '../../../hooks/useRequest';
import { useEditorActions, setEditorFn, useEditorContextState } from '../../../store/editor';
import { usePersistFn } from '../../../common';
import { IDvDataBaseItem } from '../../../type';

type TuseTableCloumn = {
    $setExpandedKeys: (key: React.Key, isCancel?: boolean) => void;
}

const useTableCloumn = ({ $setExpandedKeys }: TuseTableCloumn) => {
    const [{ databases, selectDatabases }] = useEditorContextState();
    const fns = useEditorActions({ setEditorFn });
    const { $http } = useRequest();
    const onRequestTable = usePersistFn(async (databaseName, key: string, allSelectDatabases, cb?:()=>void) => {
        try {
            const fileDatabaseName = databases.find((item) => ((item.name === databaseName) && (item.children || []).length > 0));
            if (fileDatabaseName) {
                fns.setEditorFn({ selectDatabases: [...allSelectDatabases] });
                // eslint-disable-next-line no-unused-expressions
                cb && cb();
                return;
            }

            $http.get(`catalog/list/table/${key}`).then((res) => {
                const data = databases.reduce<IDvDataBaseItem[]>((prev, cur) => {
                    if (cur.name === databaseName) {
                        cur.children = res;
                    }
                    prev.push({ ...cur });
                    return prev;
                }, []);
                fns.setEditorFn({ databases: data, selectDatabases: [...allSelectDatabases] });
                allSelectDatabases.shift();
                allSelectDatabases.map((item:any) => `${item.uuid}@@${item.name}`).join('##');
                // eslint-disable-next-line no-unused-expressions
                cb && cb();
            });
        } catch (error) {
        }
    });
    const onRequestCloumn = usePersistFn(async (databaseName, tableName, key: string, allSelectDatabases, cb?:()=>void) => {
        try {
            const fileDatabase = databases.find((item) => (item.name === databaseName));
            if (fileDatabase) {
                const findTable = (fileDatabase.children || []).find((item) => (item.name === tableName) && (item.children || []).length > 0);
                if (findTable) {
                    fns.setEditorFn({ selectDatabases: [...allSelectDatabases] });
                    // eslint-disable-next-line no-unused-expressions
                    cb && cb();
                    return;
                }
            }
            const res = await $http.get(`catalog/list/column/${key}`);
            const data = databases.reduce<IDvDataBaseItem[]>((prev, cur) => {
                if (cur.name === databaseName) {
                    const children = (cur.children || []).map((item) => {
                        if (item.name === tableName) {
                            return {
                                ...item,
                                children: res || [],
                            };
                        }
                        return { ...item };
                    });
                    cur.children = children;
                }
                prev.push({ ...cur });
                return prev;
            }, []);
            fns.setEditorFn({ databases: data, selectDatabases: [...allSelectDatabases] });
            allSelectDatabases.shift();
            allSelectDatabases.map((item:any) => `${item.uuid}@@${item.name}`).join('##');
            // eslint-disable-next-line no-unused-expressions
            cb && cb();
        } catch (error) {
        }
    });
    const onSeletCol = (name:string, key: string, allSelectDatabases: { uuid: string; name: string; }[]) => {
        fns.setEditorFn({ selectDatabases: [...allSelectDatabases] });
    };
    return {
        onRequestTable,
        onRequestCloumn,
        onSeletCol,
    };
};

export default useTableCloumn;
