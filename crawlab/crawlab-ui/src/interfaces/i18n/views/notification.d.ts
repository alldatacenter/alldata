interface LViewsNotification {
  navActions: LNavActions;
  settings: {
    form: {
      name: string;
      description: string;
      type: string;
      enabled: string;
      title: string;
      template: string;
      templateContent: string;
      mail: {
        smtp: {
          server: string;
          port: string;
          user: string;
          password: string;
          sender: {
            email: string;
            identity: string;
          };
        };
        to: string;
        cc: string;
      };
      mobile: {
        webhook: string;
      };
    };
    type: {
      mail: string;
      mobile: string;
    };
  };
  triggers: {
    models: {
      tags: string;
      nodes: string;
      projects: string;
      spiders: string;
      tasks: string;
      jobs: string;
      schedules: string;
      users: string;
      settings: string;
      tokens: string;
      variables: string;
      task_stats: string;
      plugins: string;
      spider_stats: string;
      data_sources: string;
      data_collections: string;
      passwords: string;
    };
  };
  tabs: {
    overview: string;
    triggers: string;
    template: string;
  };
}
