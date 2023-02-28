import { IMonacoConfig, IDvDataBaseItem } from '../type';
import createStore from './createStore';

type TEditor = {
    workspaceId: any,
    monacoConfig: IMonacoConfig,
    id: string | number | null,
    headers: Record<string, any>,
    databases: IDvDataBaseItem[],
    baseURL: string,
    locale: any,
    selectDatabases:{name:string, uuid:string}[],
}

const initState: TEditor = {
    monacoConfig: { paths: { vs: '' } },
    workspaceId: null,
    headers: {},
    databases: [],
    locale: '',
    baseURL: '',
    id: null,
    selectDatabases: [],

};

const reducer = (state: TEditor, action: any) => ({
    ...state,
    ...action.payload,
});

// @ts-ignore
const setEditorFn = ({ dispatch, getState }) => (value) => {
    console.log('editor state', { ...getState(), ...value });
    dispatch({ payload: { ...value } });
};

// @ts-ignore
const clearEditorDataFn = ({ dispatch }) => () => {
    dispatch({ payload: { databases: [] } });
};

const {
    useContextState: useEditorContextState, useActions: useEditorActions, Provider: EditorProvider,
} = createStore<TEditor>(reducer, initState);

export {
    useEditorContextState, useEditorActions, EditorProvider, setEditorFn, clearEditorDataFn,
};
