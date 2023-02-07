const spiders: LViewsSpiders = {
  table: {
    columns: {
      name: '名称',
      project: '项目',
      lastStatus: '最近状态',
      lastRunAt: '最近运行',
      stats: '统计数据',
      createTs: '创建时间',
      updateTs: '更新时间',
      description: '描述',
    }
  },
  navActions: {
    new: {
      label: '新建爬虫',
      tooltip: '添加一个新爬虫'
    },
    filter: {
      search: {
        placeholder: '搜索爬虫'
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        project: {
          label: '项目',
        }
      }
    }
  }
};

export default spiders;
