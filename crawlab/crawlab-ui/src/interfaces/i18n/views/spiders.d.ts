interface LViewsSpiders {
  table: {
    columns: {
      name: string;
      project: string;
      lastStatus: string;
      lastRunAt: string;
      stats: string;
      createTs: string;
      updateTs: string;
      description: string;
    };
  };
  navActions: LNavActions;
  navActionsExtra: {
    filter: {
      select: {
        project: {
          label: string;
        };
      };
    };
  };
}
