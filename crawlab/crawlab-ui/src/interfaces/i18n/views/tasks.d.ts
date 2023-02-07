interface LViewsTasks {
  table: {
    columns: {
      node: string;
      spider: string;
      schedule: string;
      priority: string;
      status: string;
      cmd: string;
      stat: {
        create_ts: string;
        start_ts: string;
        end_ts: string;
        wait_duration: string;
        runtime_duration: string;
        total_duration: string;
        results: string;
      };
    };
  };
  navActions: LNavActions;
  navActionsExtra: {
    filter: {
      select: {
        node: {
          label: string;
        };
        spider: {
          label: string;
        };
        schedule: {
          label: string;
        };
        priority: {
          label: string;
        };
        status: {
          label: string;
        };
      };
      search: {
        cmd: {
          placeholder: string;
        };
      };
    };
  };
}
