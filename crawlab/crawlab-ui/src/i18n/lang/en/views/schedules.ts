const schedules: LViewsSchedules = {
  table: {
    columns: {
      name: 'Name',
      spider: 'Spider',
      mode: 'Mode',
      cron: 'Cron Expression',
      enabled: 'Enabled',
      entryId: 'Entry ID',
      description: 'Description',
    }
  },
  navActions: {
    new: {
      label: 'New Schedule',
      tooltip: 'Create a new schedule',
    },
    filter: {
      search: {
        placeholder: 'Search schedules',
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        spider: {
          label: 'Spider',
        },
        mode: {
          label: 'Mode',
        },
        enabled: {
          label: 'Enabled',
        }
      },
      search: {
        cron: {
          placeholder: 'Search cron expression',
        }
      }
    }
  }
};

export default schedules;
