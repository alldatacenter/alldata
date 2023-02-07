const tasks: LViewsTasks = {
  table: {
    columns: {
      node: '节点',
      spider: '爬虫',
      schedule: '定时任务',
      priority: '优先级',
      status: '状态',
      cmd: '执行命令',
      stat: {
        create_ts: '创建时间',
        start_ts: '开始时间',
        end_ts: '结束时间',
        wait_duration: '等待时间',
        runtime_duration: '运行时间',
        total_duration: '总时间',
        results: '结果数',
      }
    },
  },
  navActions: {
    new: {
      label: '新建任务',
      tooltip: '创建一个新任务',
    },
    filter: {
      search: {
        placeholder: '搜索任务',
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        node: {
          label: '节点',
        },
        spider: {
          label: '爬虫',
        },
        schedule: {
          label: '定时任务',
        },
        priority: {
          label: '优先级',
        },
        status: {
          label: '状态',
        }
      },
      search: {
        cmd: {
          placeholder: '搜索执行命令',
        }
      }
    },
  }
};

export default tasks;
