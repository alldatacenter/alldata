const plugins: LViewsPlugins = {
  table: {
    columns: {
      name: 'Name',
      status: 'Status',
      processId: 'Process ID',
      description: 'Description',
    },
  },
  navActions: {
    new: {
      label: 'New Plugin',
      tooltip: 'Create a new plugin',
    },
    filter: {
      search: {
        placeholder: 'Search plugins',
      }
    },
    install: {
      label: 'Install Plugin',
      tooltip: 'Install a new plugin',
    },
    settings: {
      label: 'Settings',
      tooltip: 'View or update global plugin settings',
    }
  }
};

export default plugins;
