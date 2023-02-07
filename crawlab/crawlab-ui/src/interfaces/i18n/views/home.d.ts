interface LViewsHome {
  metrics: {
    nodes: string;
    projects: string;
    spiders: string;
    schedules: string;
    tasks: string;
    error_tasks: string;
    results: string;
    users: string;
  };
  dailyConfig: {
    title: string;
  };
  tasksByStatusConfig: {
    title: string;
  };
  tasksByNodeConfig: {
    title: string;
  };
  tasksBySpiderConfig: {
    title: string;
  };
}
