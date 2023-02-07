const schedule: LComponentsSchedule = {
  form: {
    name: '名称',
    spider: '爬虫',
    cron: 'Cron 表达式',
    cronInfo: 'Cron 信息',
    command: '命令',
    param: '参数',
    defaultMode: '默认模式',
    enabled: '是否启用',
    selectedTags: '指定标签',
    selectedNodes: '指定节点',
    description: '描述',
  },
  rules: {
    message: {
      invalidCronExpression: '无效 Cron 表达式. [分] [时] [日] [月] [星期几]',
    }
  },
  message: {
    success: {
      enable: '启用成功',
      disable: '禁用成功',
    }
  },
  cron: {
    title: {
      cronDescription: 'Cron 描述',
      nextRun: '下次执行',
      cron: 'Cron 表达式',
      description: '描述',
      next: '下一时刻',
    },
  }
};

export default schedule;
