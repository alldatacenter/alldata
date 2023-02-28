export type TJobsTableItem = {
    id: string | number,
    type?: string
}

export type TJobsTableData = {
    list: TJobsTableItem[],
    total: number
};
