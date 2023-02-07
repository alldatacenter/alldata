const spiders: LViewsSpiders = {
  table: {
    columns: {
      name: 'Name',
      project: 'Project',
      lastStatus: 'Last Status',
      lastRunAt: 'Last Run At',
      stats: 'Stats',
      createTs: 'Created At',
      updateTs: 'Updated At',
      description: 'Description',
    }
  },
  navActions: {
    new: {
      label: 'New Spider',
      tooltip: 'Create a new spider'
    },
    filter: {
      search: {
        placeholder: 'Search spiders'
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        project: {
          label: 'Project',
        }
      }
    }
  }
};

export default spiders;
