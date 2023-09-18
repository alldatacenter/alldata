export type TJobsInstanceTableItem = {
    id: string | number,
    jobType: string,
    name: string,
    status: 'submitted' | 'running' | 'failure' | 'success' | 'kill',
    updateTime: string,
}

export type TJobsInstanceTableData = {
    list: TJobsInstanceTableItem[],
    total: number
};
