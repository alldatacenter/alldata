interface LComponentsSchedule {
  form: {
    name: string;
    spider: string;
    cron: string;
    cronInfo: string;
    command: string;
    param: string;
    defaultMode: string;
    enabled: string;
    selectedTags: string;
    selectedNodes: string;
    description: string;
  };
  rules: {
    message: {
      invalidCronExpression: string;
    };
  };
  message: {
    success: {
      enable: string;
      disable: string;
    };
  };
  cron: {
    title: {
      cronDescription: string;
      nextRun: string;
      cron: string;
      description: string;
      next: string;
    };
  };
}
