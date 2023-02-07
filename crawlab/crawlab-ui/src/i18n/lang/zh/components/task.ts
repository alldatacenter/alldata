const task: LComponentsTask = {
  form: {
    spider: '爬虫',
    node: '节点',
    status: '状态',
    command: '执行命令',
    param: '执行参数',
    mode: '模式',
    priority: '优先级',
    selectedTags: '指定标签',
    selectedNodes: '指定节点',
    tooltip: {
      goToSpider: '导航至爬虫',
      goToNode: '导航至节点',
      taskErrorMessage: '任务错误信息',
      cancelTask: '取消任务',
      customized: '自定义',
    },
  },
  actions: {
    data: {
      tooltip: {
        dataActions: '数据操作',
        export: '导出',
        displayAllFields: '显示所有字段',
        inferDataFieldsTypes: '推断数据字段类型',
      }
    }
  },
  status: {
    label: {
      pending: '待定',
      running: '运行中',
      finished: '已完成',
      error: '错误',
      cancelled: '已取消',
      abnormal: '异常',
      unknown: '未知',
    },
    tooltip: {
      pending: '任务正在队列中待定',
      running: '任务正在运行',
      finished: '任务已成功完成',
      error: '任务发生错误:',
      cancelled: '任务已被取消',
      abnormal: '任务异常终止',
      unknown: '未知任务状态',
    },
  },
  priority: {
    high: '高',
    higher: '较高',
    medium: '中',
    lower: '较低',
    low: '低',
  },
  mode: {
    label: {
      randomNode: '随机节点',
      allNodes: '所有节点',
      selectedNodes: '指定节点',
      selectedTags: '指定标签',
      unknown: '未知',
    },
    tooltip: {
      randomNode: '在随机一个节点运行',
      allNodes: '在所有节点运行',
      selectedNodes: '在指定节点运行',
      selectedTags: '在指定标签对应节点运行',
      unknown: '未知运行模式',
    },
  },
  results: {
    results: '结果数',
    noResults: '无结果',
  },
  logs: {
    actions: {
      autoUpdateLogs: '自动更新日志',
    },
  },
};

export default task;
