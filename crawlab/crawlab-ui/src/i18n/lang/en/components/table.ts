const table: LComponentsTable = {
  columns: {
    actions: 'Actions',
  },
  actions: {
    editSelected: 'Edit Selected',
    deleteSelected: 'Delete Selected',
    export: 'Export',
    customizeColumns: 'Customize Columns',
  },
  columnsTransfer: {
    title: 'Table Columns Customization',
    titles: {
      left: 'Available',
      right: 'Selected'
    }
  },
  header: {
    sort: {
      tooltip: {
        sort: 'Sort',
        sortAscending: 'Sort Ascending',
        sortDescending: 'Sort Descending',
      },
    },
    filter: {
      tooltip: {
        filter: 'Filter',
        search: 'Search',
        include: 'Include',
      },
    },
    dialog: {
      sort: {
        title: 'Sort',
        clearSort: 'Clear sort',
        ascending: 'Ascending',
        descending: 'Descending',
      },
      filter: {
        title: 'Filter',
        search: 'Search',
      },
    },
  },
};

export default table;
