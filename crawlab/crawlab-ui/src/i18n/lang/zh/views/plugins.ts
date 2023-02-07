const plugins: LViewsPlugins = {
  table: {
    columns: {
      name: '名称',
      status: '状态',
      processId: '进程 ID',
      description: '描述',
    },
  },
  navActions: {
    new: {
      label: '新建插件',
      tooltip: '添加一个新插件',
    },
    filter: {
      search: {
        placeholder: '搜索插件',
      }
    },
    install: {
      label: '安装插件',
      tooltip: '安装一个新插件',
    },
    settings: {
      label: '设置',
      tooltip: '查看或更新全局插件设置',
    }
  }
};

export default plugins;
