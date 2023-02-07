const tasks: LViewsTasks = {
  table: {
    columns: {
      node: 'Node',
      spider: 'Spider',
      schedule: 'Schedule',
      priority: 'Priority',
      status: 'Status',
      cmd: 'Execute Command',
      stat: {
        create_ts: 'Created At',
        start_ts: 'Started At',
        end_ts: 'Finished At',
        wait_duration: 'Wait Duration',
        runtime_duration: 'Runtime Duration',
        total_duration: 'Total Duration',
        results: 'Results',
      }
    }
  },
  navActions: {
    new: {
      label: 'New Task',
      tooltip: 'Create a new task',
    },
    filter: {
      search: {
        placeholder: 'Search tasks',
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        node: {
          label: 'Node',
        },
        spider: {
          label: 'Spider',
        },
        schedule: {
          label: 'Schedule',
        },
        priority: {
          label: 'Priority',
        },
        status: {
          label: 'Status',
        }
      },
      search: {
        cmd: {
          placeholder: 'Search Execute Command',
        }
      }
    },
  }
};

export default tasks;
