export declare global {
  interface LComponents {
    // common components
    chart: LComponentsChart;
    table: LComponentsTable;
    dialog: LComponentsDialog;
    transfer: LComponentsTransfer;
    nav: LComponentsNav;
    tab: LComponentsTab;
    input: LComponentsInput;
    git: LComponentsGit;
    empty: LComponentsEmpty;
    form: LComponentsForm;
    file: LComponentsFile;
    date: LComponentsDate;
    metric: LComponentsMetric;
    export: LComponentsExport;
    result: LComponentsResult;

    // model-related components
    node: LComponentsNode;
    project: LComponentsProject;
    spider: LComponentsSpider;
    schedule: LComponentsSchedule;
    task: LComponentsTask;
    user: LComponentsUser;
    tag: LComponentsTag;
    plugin: LComponentsPlugin;
  }
}
