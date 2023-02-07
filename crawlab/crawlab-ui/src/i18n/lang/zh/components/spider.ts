const spider: LComponentsSpider = {
  form: {
    name: '名称',
    project: '项目',
    command: '执行命令',
    param: '参数',
    defaultMode: '默认模式',
    resultsCollection: '结果集',
    selectedTags: '指定标签',
    selectedNodes: '指定节点',
    description: '描述',
  },
  actions: {
    files: {
      tooltip: {
        fileEditorActions: '文件编辑器操作',
        uploadFiles: '上传文件',
        fileEditorSettings: '文件编辑器设置',
      },
    },
    data: {
      tooltip: {
        dataActions: '数据操作',
        export: '导出',
        displayAllFields: '显示所有字段',
        inferDataFieldsTypes: '推断数据字段类型',
        dedup: {
          enabled: '已启用去重',
          disabled: '已禁用去重',
          fields: '设置去重字段',
        },
      },
    }
  },
  stat: {
    totalTasks: '总任务数',
    totalResults: '总结果数',
    averageWaitDuration: '平均等待时间',
    averageRuntimeDuration: '平均运行时间',
    averageTotalDuration: '平均总时间',
  },
  dialog: {
    run: {
      title: '运行爬虫',
    }
  },
  message: {
    success: {
      scheduleTask: '派发任务成功',
    }
  }
};

export default spider;
