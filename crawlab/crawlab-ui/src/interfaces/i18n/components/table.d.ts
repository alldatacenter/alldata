interface LComponentsTable {
  columns: {
    actions: string;
  };
  actions: {
    editSelected: string;
    deleteSelected: string;
    export: string;
    customizeColumns: string;
  };
  columnsTransfer: {
    title: string;
    titles: {
      left: string;
      right: string;
    };
  };
  header: {
    sort: {
      tooltip: {
        sort: string;
        sortAscending: string;
        sortDescending: string;
      };
    };
    filter: {
      tooltip: {
        filter: string;
        search: string;
        include: string;
      };
    };
    dialog: {
      sort: {
        title: string;
        clearSort: string;
        ascending: string;
        descending: string;
      };
      filter: {
        title: string;
        search: string;
      };
    };
  };
}
