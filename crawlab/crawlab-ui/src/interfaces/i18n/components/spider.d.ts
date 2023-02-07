interface LComponentsSpider {
  form: {
    name: string;
    project: string;
    command: string;
    param: string;
    defaultMode: string;
    resultsCollection: string;
    selectedTags: string;
    selectedNodes: string;
    description: string;
  };
  actions: {
    files: {
      tooltip: {
        fileEditorActions: string;
        uploadFiles: string;
        fileEditorSettings: string;
      };
    };
    data: {
      tooltip: {
        dataActions: string;
        export: string;
        displayAllFields: string;
        inferDataFieldsTypes: string;
        dedup: {
          enabled: string;
          disabled: string;
          fields: string;
        };
      };
    };
  };
  stat: {
    totalTasks: string;
    totalResults: string;
    averageWaitDuration: string;
    averageRuntimeDuration: string;
    averageTotalDuration: string;
  };
  dialog: {
    run: {
      title: string;
    };
  };
  message: {
    success: {
      scheduleTask: string;
    };
  };
}
