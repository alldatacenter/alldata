interface LViewsSchedules {
  table: {
    columns: {
      name: string;
      spider: string;
      mode: string;
      cron: string;
      enabled: string;
      entryId: string;
      description: string;
    };
  };
  navActions: LNavActions;
  navActionsExtra: {
    filter: {
      select: {
        spider: {
          label: string;
        };
        mode: {
          label: string;
        };
        enabled: {
          label: string;
        };
      };
      search: {
        cron: {
          placeholder: string;
        };
      };
    };
  };
}
