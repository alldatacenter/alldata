export type TWarnSLATableItem = {
    id?: any,
    name?: string,
    workspaceId?: any;
    updateTime?: string,
    updateBy?: string,
    description?: string,
}

export type TWarnTableItem = {
    id: string | number,
    name: string,
}

export type TWarnTableData = {
    list: TWarnTableItem[],
    total: number
};

export type TWarnMetricTableItem = {
    id: string | number,
    name: string,
}

export type TWarnMetricTableData = {
    list: TWarnMetricTableItem[],
    total: number
};

export type TNoticeTableItem = {
    id: string | number,
    name: string,
}

export type TNoticeTableData = {
    list: TNoticeTableItem[],
    total: number
};
