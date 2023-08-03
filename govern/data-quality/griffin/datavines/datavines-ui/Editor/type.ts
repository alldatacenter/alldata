import type {
    editor, languages, Uri, CancellationTokenSource, Emitter, KeyCode, KeyMod, MarkerSeverity, MarkerTag, Position, Range, Selection, SelectionDirection,
} from 'monaco-editor';
import { TDetail } from './components/MetricModal/type';

export type TMonaco = {
    editor: typeof editor,
    languages: typeof languages,
    Uri: typeof Uri,
    CancellationTokenSource: typeof CancellationTokenSource,
    Emitter: typeof Emitter,
    KeyCode: typeof KeyCode,
    KeyMod: typeof KeyMod,
    MarkerSeverity: typeof MarkerSeverity,
    MarkerTag: typeof MarkerTag,
    Position: typeof Position,
    Range: typeof Range,
    Selection: typeof Selection,
    SelectionDirection: typeof SelectionDirection,
}

export type TCodeEditor = editor.ICodeEditor;
declare global {
    interface Window {
        monaco: TMonaco
    }
}
export type TSqlType = 'mysql' | 'clickhouse' | 'hive' | 'impala' | 'postgresql';

export type THintsItem = [str:string, arg:string[]]
export type TUseEditor = {
    elRef: any,
    value: string,
    language: TSqlType,
    tableColumnHints: THintsItem[],
    onChange?: (...args: any[]) => any
};

export interface IMonacoConfig{
    paths: {
        vs: string,
        [key: string]: any;
    }
    [key: string]: any;
}

export interface IDvEditorProps {
    monacoConfig?: IMonacoConfig,
    baseURL: string,
    headers?: Record<string, any>,
    id: number | string | null,
    workspaceId?: any,
    showMetricConfig?: boolean,
    detail?: TDetail,
    innerRef?: any,
    locale?: any,
    onShowModal?: (...args: any[]) => any;
    uuid?:number | string | null,
    afterClose?: (...args: any[]) => any;
    jobId?:number | string | null;
}

export interface IDvDataBaseItem{
    comment: string,
    name: string,
    type: string,
    children?: IDvDataBaseItem[],
    uuid:string;
    isLoading?:boolean;
}

export interface IDvSqlTableColumnItem {
    name: string;
    type: string;
    comment: null;
}

export interface IDvSqlTableResultItem {
    id: number;
    name: string;
    type: string;
    param: string;
    workspace_id: number;
    create_by: number;
    create_time: number;
    update_by: number;
    update_time: number;
}
export interface IDvSqlTable {
    resultList: IDvSqlTableResultItem[];
    columns: IDvSqlTableColumnItem[];
}
