export interface ICreateDataSourceProps {
    type: string;
    placeholder: string;
    rows: number;
    disabled: boolean;
    size: string;
}

export interface ICreateDataSourceValidate {
    required: boolean;
    message: string;
    type: string;
    trigger: string;
}

export interface ICreateDataSourceItem {
    field: string;
    props: ICreateDataSourceProps;
    type: string;
    title: string;
    value: string;
    validate: ICreateDataSourceValidate[];
}

export type TableType = 'CARD' | 'TABLE';

export interface IDataSourceListItem {
    id: number,
    name: string,
    param: string,
    type: string,
    updater: string,
    updateTime: string,
    [key: string]: any
}

export interface IDataSourceList {
    total: number,
    list: IDataSourceListItem[]
}
